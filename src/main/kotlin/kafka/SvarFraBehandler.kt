package kafka

import com.fasterxml.jackson.databind.JsonNode
import domain.BehandlerRef
import domain.ConversationRef
import domain.Identitetsnummer

sealed interface SvarFraBehandler {
    val behandlerRef: BehandlerRef
    val identitetsnummer: Identitetsnummer
    val json: JsonNode

    class MedConversationRef(
        val conversationRef: ConversationRef,
        override val behandlerRef: BehandlerRef,
        override val identitetsnummer: Identitetsnummer,
        override val json: JsonNode,
    ) : SvarFraBehandler

    class UtenConversationRef(
        override val behandlerRef: BehandlerRef,
        override val identitetsnummer: Identitetsnummer,
        override val json: JsonNode,
    ) : SvarFraBehandler
}
