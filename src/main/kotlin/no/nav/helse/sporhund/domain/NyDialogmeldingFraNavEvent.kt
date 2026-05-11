package no.nav.helse.sporhund.domain

data class NyDialogmeldingFraNavEvent(
    val conversationRef: ConversationRef,
    val behandlerRef: BehandlerRef,
    val identitetsnummer: Identitetsnummer,
    val meldingId: DialogmeldingId,
    val tekst: String?,
)
