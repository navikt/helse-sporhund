package no.nav.helse.sporhund.infrastructure.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.loggDebug
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.HprNummer
import no.nav.helse.sporhund.domain.Identitetsnummer
import no.nav.helse.sporhund.domain.Kontor
import no.nav.helse.sporhund.domain.Navn
import no.nav.helse.sporhund.domain.Organisasjonsnummer
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.ZoneId
import java.util.UUID

fun KafkaConsumerJobb.håndterSvarFraBehandler(
    transactionProvider: TransactionProvider,
    record: ConsumerRecord<String, String>,
) {
    val kafkamelding = objectMapper.readValue<DialogmeldingFraBehandlerKafkaDto>(record.value())
    if (!kafkamelding.erRelevant()) {
        loggDebug(
            "Meldingen er ikke relevant. Ignorerer meldingen.",
            "melding" to objectMapper.writeValueAsString(kafkamelding.medMaskertForesporselSvar()),
        )
        return
    }

    if (kafkamelding.conversationRef != null) {
        loggInfo(
            "conversationRef er uuid, forsøker å knytte meldingen til dialog",
            "melding" to objectMapper.writeValueAsString(kafkamelding.medMaskertForesporselSvar()),
        )
        val svarFraBehandler = kafkamelding.svarFraBehandlerMedConversationRef()
        svarFraBehandler.håndterSvarMedConversationRef(transactionProvider)
    } else {
        loggDebug(
            "conversationRef er null, ignorerer meldingen.",
            "melding" to objectMapper.writeValueAsString(kafkamelding.medMaskertForesporselSvar()),
        )
    }
}

fun KafkaConsumerJobb.håndterStatusOppdatering(
    transactionProvider: TransactionProvider,
    record: ConsumerRecord<String, String>,
) {
    val kafkamelding = objectMapper.readValue<DialogmeldingStatusKafkaDto>(record.value())
    if (kafkamelding.status != "OK" && kafkamelding.status != "AVVIST") {
        loggDebug("Ignorerer statusmelding med status=${kafkamelding.status}")
        return
    }
    val meldingId = UUID.fromString(kafkamelding.bestillingUuid)
    transactionProvider.transaction {
        val dialog = dialogRepository.finnDialogVedMeldingId(meldingId)
        if (dialog == null) {
            loggDebug("Fant ingen dialog for bestillingUuid=${kafkamelding.bestillingUuid}, ignorerer statusmelding")
            return@transaction
        }
        dialog.mottaKvittering(
            meldingId = DialogmeldingId(meldingId),
            avvist = kafkamelding.status == "AVVIST",
        )
        dialogRepository.lagre(dialog)
        loggInfo(
            "Mottok kvittering for melding",
            "bestillingUuid" to kafkamelding.bestillingUuid,
            "status" to kafkamelding.status,
        )
    }
}

private fun SvarFraBehandler.MedConversationRef.håndterSvarMedConversationRef(
    transactionProvider: TransactionProvider,
) {
    transactionProvider.transaction {
        val dialog = dialogRepository.finnDialog(conversationRef) ?: return@transaction
        val fraBehandler =
            Dialogmelding.FraBehandler.ny(
                meldingId = meldingId,
                tidspunkt = tidspunktMottattNav,
                melding = tekst,
                behandler = behandler,
                antallVedlegg = antallVedlegg,
            )
        if (dialog.meldingFinnes(fraBehandler.id)) return@transaction loggInfo("Melding med id=$meldingId finnes allerede i dialogen, forsøker ikke å legge den til på nytt")
        dialog.nyMelding(fraBehandler)
        dialogRepository.lagre(dialog)
        outbox.nyMelding(OutboxMelding.knyttInnkommendeJournalpost(journalpostId, dialog))
        loggInfo("Knytter meldingen til dialog")
    }
}

private fun DialogmeldingFraBehandlerKafkaDto.svarFraBehandlerMedConversationRef(): SvarFraBehandler.MedConversationRef {
    val forespørselssvar = checkNotNull(this.dialogmelding.foresporselFraSaksbehandlerForesporselSvar)
    return SvarFraBehandler.MedConversationRef(
        conversationRef = ConversationRef(UUID.fromString(checkNotNull(this.conversationRef))),
        hprNummer = HprNummer(checkNotNull(this.legehpr).toInt()),
        identitetsnummerSykmeldt = Identitetsnummer.fraString(this.personIdentPasient),
        behandler = this.tilBehandler(),
        tekst = forespørselssvar.tekstNotatInnhold,
        antallVedlegg = this.antallVedlegg,
        tidspunktMottattNav = this.mottattTidspunkt.atZone(ZoneId.of("Europe/Oslo")).toInstant(),
        meldingId = this.msgId,
        journalpostId = this.journalpostId,
    )
}

private fun String.tilNavn(): Navn {
    val deler = this.trim().split(" ").filter { it.isNotEmpty() }
    return Navn(
        fornavn = deler.first(),
        mellomnavn = if (deler.size > 2) deler.drop(1).dropLast(1).joinToString(" ") else null,
        etternavn = deler.last(),
    )
}

private fun DialogmeldingFraBehandlerKafkaDto.tilBehandler(): Behandler =
    Behandler(
        hprNummer = HprNummer(checkNotNull(this.legehpr).toInt()),
        navn = this.dialogmelding.navnHelsepersonell.tilNavn(),
        kontor =
            Kontor(
                navn = this.legekontorOrgName,
                organisasjonsnummer = this.legekontorOrgNr?.let { Organisasjonsnummer(it) },
                adresse = null,
            ),
        telefonnummer = null,
    )
