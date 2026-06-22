package no.nav.helse.sporhund.infrastructure.kafka

import java.time.OffsetDateTime

data class DialogmeldingStatusKafkaDto(
    val uuid: String,
    val createdAt: OffsetDateTime,
    val status: String,
    val tekst: String?,
    val bestillingUuid: String,
)
