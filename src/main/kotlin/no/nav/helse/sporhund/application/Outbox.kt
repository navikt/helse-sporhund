package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.NyDialogmeldingFraNavEvent
import java.util.UUID

interface Outbox {
    fun nyMelding(melding: OutboxMelding)

    fun meldinger(): List<OutboxMelding>

    fun meldingSendt(id: OutboxMeldingId)
}

@JvmInline
value class OutboxMeldingId(
    val value: UUID,
)

data class OutboxMelding(
    val id: OutboxMeldingId,
    val event: NyDialogmeldingFraNavEvent,
)
