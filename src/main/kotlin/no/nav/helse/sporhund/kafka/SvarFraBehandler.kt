package no.nav.helse.sporhund.kafka

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sporhund.domain.BehandlerRef
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Identitetsnummer

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
