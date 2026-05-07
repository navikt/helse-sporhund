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
    val dialogmeldingFraBehandler = record.parseDialogmeldingFraBehandler()

    when (dialogmeldingFraBehandler) {
        is SvarFraBehandler.MedConversationRef -> TODO()
        is SvarFraBehandler.UtenConversationRef -> håndterSvarUtenConversationRef(transactionProvider, dialogmeldingFraBehandler.json)
    }
}

private fun ConsumerRecord<String, String>.parseDialogmeldingFraBehandler(): SvarFraBehandler {
    val json = objectMapper.readTree(this.value())
    val behandlerRef = BehandlerRef(json["behandlerRef"].textValue())
    val identitetsnummer = Identitetsnummer.fraString(json["personIdentPasient"].textValue())

    return if (json["conversationRef"].textValue() != null) {
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
}
