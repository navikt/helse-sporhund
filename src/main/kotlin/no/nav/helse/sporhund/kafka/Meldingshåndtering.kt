package no.nav.helse.sporhund.kafka

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.db.objectMapper
import no.nav.helse.sporhund.domain.BehandlerRef
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Identitetsnummer
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.util.UUID

fun KafkaConsumer.håndterSvarFraBehandler(
    transactionProvider: TransactionProvider,
    record: ConsumerRecord<String, String>,
) {
    val json = objectMapper.readTree(record.value())
    if (json["conversationRef"] != null && !json["conversationRef"].erUuid()) {
        loggInfo("conversationRef er ikke UUID: ${json["conversationRef"].asText()}. Ignorerer meldingen.", "melding" to json.toPrettyString())
    }
    when (val dialogmeldingFraBehandler = json.parseDialogmeldingFraBehandler()) {
        is SvarFraBehandler.MedConversationRef -> {
            håndterSvarMedConversationRef(transactionProvider, dialogmeldingFraBehandler.json)
        }

        is SvarFraBehandler.UtenConversationRef -> {
            håndterSvarUtenConversationRef(transactionProvider, dialogmeldingFraBehandler.json)
        }
    }
}

private fun JsonNode.parseDialogmeldingFraBehandler(): SvarFraBehandler {
    val behandlerRef = BehandlerRef(this["personIdentBehandler"].asText())
    val identitetsnummer = Identitetsnummer.fraString(this["personIdentPasient"].asText())

    return if (this["conversationRef"] != null) {
        SvarFraBehandler.MedConversationRef(
            conversationRef = ConversationRef(UUID.fromString(this["conversationRef"].asText())),
            behandlerRef = behandlerRef,
            identitetsnummer = identitetsnummer,
            json = this,
        )
    } else {
        SvarFraBehandler.UtenConversationRef(
            behandlerRef = behandlerRef,
            identitetsnummer = identitetsnummer,
            json = this,
        )
    }
}

private fun JsonNode.erUuid(): Boolean =
    runCatching {
        UUID.fromString(this.asText())
    }.isSuccess

private fun håndterSvarUtenConversationRef(
    transactionProvider: TransactionProvider,
    json: JsonNode,
) {
    println("Svar uten conversationRef: ${json.toPrettyString()}")
}

private fun håndterSvarMedConversationRef(
    transactionProvider: TransactionProvider,
    json: JsonNode,
) {
    println("Svar med conversationRef: ${json.toPrettyString()}")
}
