package no.nav.helse.sporhund.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.db.objectMapper
import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.HprNummer
import no.nav.helse.sporhund.domain.Identitetsnummer
import no.nav.helse.sporhund.domain.Organisasjonsnummer
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.ZoneId
import java.util.UUID

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
        val svarFraBehandler = kafkamelding.svarFraBehandlerMedConversationRef()
        svarFraBehandler.håndterSvarMedConversationRef(transactionProvider)
        loggInfo("conversationRef er uuid, forsøker å knytte meldingen til dialog", "melding" to objectMapper.writeValueAsString(kafkamelding))
    } else {
        loggInfo("conversationRef er null, ignorerer meldingen.", "melding" to objectMapper.writeValueAsString(kafkamelding))
    }
}

private fun SvarFraBehandler.MedConversationRef.håndterSvarMedConversationRef(
    transactionProvider: TransactionProvider,
) {
    transactionProvider.transaction {
        val dialog = dialogRepository.finnDialog(conversationRef) ?: return@transaction
        dialog.nyMelding(
            Dialogmelding.FraBehandler(
                id = DialogmeldingId(UUID.randomUUID()),
                tidspunkt = tidspunktMottattNav,
                melding = tekst,
                behandler = behandler,
                antallVedlegg = antallVedlegg,
            ),
        )
        dialogRepository.lagre(dialog)
        loggInfo("Knytter meldingen til dialog")
    }
}

private fun DialogmeldingFraBehandlerKafkaDto.svarFraBehandlerMedConversationRef(): SvarFraBehandler.MedConversationRef {
    val forespørselssvar = checkNotNull(this.dialogmelding.foresporselFraSaksbehandlerForesporselSvar)
    return SvarFraBehandler.MedConversationRef(
        conversationRef = ConversationRef(UUID.fromString(checkNotNull(this.conversationRef))),
        hprNummer = HprNummer(checkNotNull(this.legehpr)),
        identitetsnummerSykmeldt = Identitetsnummer.fraString(this.personIdentPasient),
        behandler = this.tilBehandler(),
        tekst = forespørselssvar.tekstNotatInnhold,
        antallVedlegg = this.antallVedlegg,
        tidspunktMottattNav = this.mottattTidspunkt.atZone(ZoneId.of("Europe/Oslo")).toInstant(),
    )
}

private fun DialogmeldingFraBehandlerKafkaDto.tilBehandler(): Behandler =
    Behandler(
        hprNummer = HprNummer(checkNotNull(this.legehpr)),
        navn = this.dialogmelding.navnHelsepersonell,
        kontor = this.legekontorOrgName,
        kontorOrganisasjonsnummer = Organisasjonsnummer(checkNotNull(this.legekontorOrgNr)),
    )
