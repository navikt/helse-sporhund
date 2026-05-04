package domain

data class NyDialogmeldingFraNavEvent(
    val conversationRef: String,
    val behandlerRef: String,
    val identitetsnummer: Identitetsnummer,
)
