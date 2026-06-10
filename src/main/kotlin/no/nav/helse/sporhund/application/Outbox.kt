package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.Dialogtype
import no.nav.helse.sporhund.domain.Fagområde
import no.nav.helse.sporhund.domain.Identitetsnummer
import no.nav.helse.sporhund.domain.Navn
import no.nav.helse.sporhund.domain.NyDialogmeldingFraNavEvent
import no.nav.helse.sporhund.domain.Saksbehandler
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

interface Outbox {
    fun nyMelding(melding: OutboxMelding)

    fun <T : OutboxMelding> meldinger(type: KClass<T>): List<T>

    fun meldingSendt(id: OutboxMeldingId)
}

inline fun <reified T : OutboxMelding> Outbox.meldinger(): List<T> = meldinger(T::class)

@JvmInline
value class OutboxMeldingId(
    val value: UUID,
)

sealed interface OutboxMelding {
    val id: OutboxMeldingId

    companion object {
        fun nyDialogmeldingFraNav(nyDialogmeldingFraNavEvent: NyDialogmeldingFraNavEvent) =
            NyDialogmeldingFraNav(
                id = OutboxMeldingId(UUID.randomUUID()),
                nyDialogmeldingFraNavEvent = nyDialogmeldingFraNavEvent,
            )

        fun opprettUtgåendeJournalpost(
            melding: Dialogmelding.FraNav,
            dialog: Dialog,
            avsender: Saksbehandler,
        ) = OpprettUtgåendeJournalpost(
            id = OutboxMeldingId(UUID.randomUUID()),
            conversationRef = dialog.conversationRef,
            meldingId = melding.id,
            tekst = melding.melding,
            avsender = avsender,
            mottaker = melding.behandler,
            gjelder = dialog.identitetsnummer,
            søkernavn = dialog.søkernavn,
            tidspunkt = melding.tidspunkt,
            fagområde = dialog.fagområde,
            dialogtype = dialog.dialogtype,
        )

        fun knyttInnkommendeJournalpost(
            journalpostId: String,
            dialog: Dialog,
        ) = KnyttInnkommendeJournalpost(
            id = OutboxMeldingId(UUID.randomUUID()),
            journalpostId = journalpostId,
            conversationRef = dialog.conversationRef,
            identitetsnummer = dialog.identitetsnummer,
        )
    }
}

data class NyDialogmeldingFraNav(
    override val id: OutboxMeldingId,
    val nyDialogmeldingFraNavEvent: NyDialogmeldingFraNavEvent,
) : OutboxMelding

data class OpprettUtgåendeJournalpost(
    override val id: OutboxMeldingId,
    val conversationRef: ConversationRef,
    val meldingId: DialogmeldingId<UUID>,
    val tekst: String,
    val avsender: Saksbehandler,
    val mottaker: Behandler,
    val gjelder: Identitetsnummer,
    val søkernavn: Navn,
    val tidspunkt: Instant,
    val fagområde: Fagområde,
    val dialogtype: Dialogtype,
) : OutboxMelding

data class KnyttInnkommendeJournalpost(
    override val id: OutboxMeldingId,
    val journalpostId: String,
    val conversationRef: ConversationRef,
    val identitetsnummer: Identitetsnummer,
) : OutboxMelding
