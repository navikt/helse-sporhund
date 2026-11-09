package no.nav.helse.sporhund.application.kommando

import no.nav.helse.sporhund.application.DialogmeldingTilBehandlerSender
import no.nav.helse.sporhund.application.MeldingTilBehandlerPdfInput
import no.nav.helse.sporhund.application.PdfProvider
import no.nav.helse.sporhund.application.PurringTilBehandlerPdfInput
import no.nav.helse.sporhund.application.SessionContext
import no.nav.helse.sporhund.application.UtgåendeDialogmeldingJournalfører
import no.nav.helse.sporhund.application.UtgåendeDialogmeldingJournalpost
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.NyDialogmeldingFraNavEvent
import java.util.UUID

class DialogmeldingKommandoHåndterer(
    private val pdfProvider: PdfProvider,
    private val sender: DialogmeldingTilBehandlerSender,
    private val journalfører: UtgåendeDialogmeldingJournalfører,
) : KommandoHåndterer {
    override fun håndter(
        kommando: Kommando,
        session: SessionContext,
    ): List<Kommando> =
        when (kommando) {
            is LagDialogmeldingPdf -> lagPdf(kommando, session)
            is SendDialogmeldingPåKafka -> sendPåKafka(kommando, session)
            is JournalførDialogmelding -> journalfør(kommando, session)
        }

    private fun lagPdf(
        kommando: LagDialogmeldingPdf,
        session: SessionContext,
    ): List<Kommando> {
        val (dialog, melding) = session.finnDialogOgMelding(kommando.conversationRef, kommando.meldingId)
        val pdf =
            when (melding) {
                is Dialogmelding.FraNav ->
                    pdfProvider.genererPdf(
                        MeldingTilBehandlerPdfInput.fra(
                            conversationRef = dialog.conversationRef,
                            tekst = melding.melding,
                            avsenderIdent = melding.saksbehandler,
                            avsenderNavn = checkNotNull(kommando.avsenderNavn) { "Mangler navn på avsender av dialogmelding fra Nav" },
                            mottaker = melding.behandler,
                            gjelder = dialog.identitetsnummer,
                            søker = dialog.søker,
                            tidspunkt = melding.tidspunkt,
                            fagområde = dialog.fagområde,
                        ),
                    )
                is Dialogmelding.FraSystem ->
                    pdfProvider.genererPurringPdf(
                        PurringTilBehandlerPdfInput.fra(
                            conversationRef = dialog.conversationRef,
                            tekst = melding.melding,
                            mottaker = melding.behandler,
                            gjelder = dialog.identitetsnummer,
                            søker = dialog.søker,
                            tidspunkt = melding.tidspunkt,
                            fagområde = dialog.fagområde,
                        ),
                    )
            }
        session.dialogmeldingPdfRepository.lagre(melding.id, pdf)
        return listOf(
            SendDialogmeldingPåKafka(KommandoId(UUID.randomUUID()), dialog.conversationRef, melding.id),
            JournalførDialogmelding(KommandoId(UUID.randomUUID()), dialog.conversationRef, melding.id),
        )
    }

    private fun sendPåKafka(
        kommando: SendDialogmeldingPåKafka,
        session: SessionContext,
    ): List<Kommando> {
        val (dialog, melding) = session.finnDialogOgMelding(kommando.conversationRef, kommando.meldingId)
        sender.send(
            NyDialogmeldingFraNavEvent(
                conversationRef = dialog.conversationRef,
                behandlerRef = melding.behandlerRef,
                identitetsnummer = dialog.identitetsnummer,
                meldingId = melding.id,
                tekst = melding.melding,
                erPurring = melding.erPurring,
            ),
            session.finnPdf(melding.id),
        )
        return emptyList()
    }

    private fun journalfør(
        kommando: JournalførDialogmelding,
        session: SessionContext,
    ): List<Kommando> {
        val (dialog, melding) = session.finnDialogOgMelding(kommando.conversationRef, kommando.meldingId)
        journalfører.journalfør(
            UtgåendeDialogmeldingJournalpost(
                conversationRef = dialog.conversationRef,
                meldingId = melding.id,
                mottaker = melding.behandler,
                gjelder = dialog.identitetsnummer,
                fagområde = dialog.fagområde,
                erPurring = melding.erPurring,
            ),
            session.finnPdf(melding.id),
        )
        return emptyList()
    }

    private fun SessionContext.finnDialogOgMelding(
        conversationRef: ConversationRef,
        meldingId: DialogmeldingId<UUID>,
    ): Pair<Dialog, Dialogmelding.Utgående> {
        val dialog = dialogRepository.finnDialog(conversationRef) ?: error("Fant ikke dialog for kommando")
        val melding =
            dialog.meldinger.filterIsInstance<Dialogmelding.Utgående>().singleOrNull { it.id == meldingId }
                ?: error("Fant ikke utgående dialogmelding ${meldingId.value} i dialogen")
        return dialog to melding
    }

    private fun SessionContext.finnPdf(meldingId: DialogmeldingId<UUID>): ByteArray =
        dialogmeldingPdfRepository.finn(meldingId)
            ?: error("Fant ikke PDF for dialogmelding ${meldingId.value}")
}
