package domain

data class NyDialogmeldingFraNavEvent(
    val conversationRef: ConversationRef,
    val behandlerRef: BehandlerRef,
    val identitetsnummer: Identitetsnummer,
)
