package no.nav.helse.sporhund.kafka

import java.util.UUID

fun String.erUuid(): Boolean =
    runCatching {
        UUID.fromString(this)
    }.isSuccess

fun DialogmeldingFraBehandlerKafkaDto.erRelevant(): Boolean = (this.conversationRef == null || this.conversationRef.erUuid()) && this.dialogmelding.foresporselFraSaksbehandlerForesporselSvar != null
