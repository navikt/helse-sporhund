package no.nav.helse.sporhund.domain

import java.util.*

data class NyDialogmeldingFraNavEvent(
    val conversationRef: ConversationRef,
    val behandlerRef: BehandlerRef,
    val identitetsnummer: Identitetsnummer,
    val meldingId: DialogmeldingId<UUID>,
    val tekst: String?,
)
