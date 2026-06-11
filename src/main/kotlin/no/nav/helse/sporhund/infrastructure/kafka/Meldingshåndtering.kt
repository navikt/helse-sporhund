package no.nav.helse.sporhund.infrastructure.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.domain.*
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.ZoneId
import java.util.*

fun KafkaConsumer.håndterSvarFraBehandler(
    transactionProvider: TransactionProvider,
    record: ConsumerRecord<String, String>,
) {
    val kafkamelding = objectMapper.readValue<DialogmeldingFraBehandlerKafkaDto>(record.value())
    if (!kafkamelding.erRelevant()) {
        loggInfo("Meldingen er ikke relevant. Ignorerer meldingen.", "melding" to objectMapper.writeValueAsString(kafkamelding))
        return
    }

    if (kafkamelding.conversationRef != null) {
        loggInfo("conversationRef er uuid, forsøker å knytte meldingen til dialog", "melding" to objectMapper.writeValueAsString(kafkamelding))
        val svarFraBehandler = kafkamelding.svarFraBehandlerMedConversationRef()
        svarFraBehandler.håndterSvarMedConversationRef(transactionProvider)
    } else {
        loggInfo("conversationRef er null, ignorerer meldingen.", "melding" to objectMapper.writeValueAsString(kafkamelding))
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
        dialog.nyMelding(fraBehandler)
        dialogRepository.lagre(dialog)
        outbox.nyMelding(OutboxMelding.knyttInnkommendeJournalpost(fraBehandler.id.value, dialog))
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
                this.legekontorOrgName,
                Organisasjonsnummer(checkNotNull(this.legekontorOrgNr)),
                adresse = null,
            ),
        telefonnummer = null,
    )
