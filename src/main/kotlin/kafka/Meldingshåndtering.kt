package kafka

import application.TransactionProvider
import com.fasterxml.jackson.databind.JsonNode
import db.objectMapper
import domain.BehandlerRef
import domain.ConversationRef
import domain.Identitetsnummer
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.util.UUID

fun håndterSvarFraBehandler(
    transactionProvider: TransactionProvider,
    record: ConsumerRecord<String, String>,
) {
    when (val dialogmeldingFraBehandler = record.parseDialogmeldingFraBehandler()) {
        is SvarFraBehandler.MedConversationRef -> {
            håndterSvarMedConversationRef(transactionProvider, dialogmeldingFraBehandler.json)
        }

        is SvarFraBehandler.UtenConversationRef -> {
            håndterSvarUtenConversationRef(transactionProvider, dialogmeldingFraBehandler.json)
        }
    }
}

private fun ConsumerRecord<String, String>.parseDialogmeldingFraBehandler(): SvarFraBehandler {
    val json = objectMapper.readTree(this.value())
    val behandlerRef = BehandlerRef(json["personIdentBehandler"].textValue())
    val identitetsnummer = Identitetsnummer.fraString(json["personIdentPasient"].textValue())

    return if (json["conversationRef"] != null) {
        SvarFraBehandler.MedConversationRef(
            conversationRef = ConversationRef(UUID.fromString(json["conversationRef"].textValue())),
            behandlerRef = behandlerRef,
            identitetsnummer = identitetsnummer,
            json = json,
        )
    } else {
        SvarFraBehandler.UtenConversationRef(
            behandlerRef = behandlerRef,
            identitetsnummer = identitetsnummer,
            json = json,
        )
    }
}

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
