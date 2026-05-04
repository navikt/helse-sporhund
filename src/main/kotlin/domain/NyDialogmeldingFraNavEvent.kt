package domain

data class NyDialogmeldingFraNavEvent(
    val conversationRef: ConversationRef,
    val behandlerRef: BehandlerRef,
    val identitetsnummer: Identitetsnummer,
    val meldingId: DialogmeldingId,
    val type: String,
    val tekst: String?,
)
