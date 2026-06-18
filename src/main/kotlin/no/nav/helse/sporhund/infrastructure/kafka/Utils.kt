package no.nav.helse.sporhund.infrastructure.kafka

import java.util.*

fun String.erUuid(): Boolean =
    runCatching {
        UUID.fromString(this)
    }.isSuccess

fun DialogmeldingFraBehandlerKafkaDto.erRelevant(): Boolean = (this.conversationRef == null || this.conversationRef.erUuid()) && this.dialogmelding.foresporselFraSaksbehandlerForesporselSvar != null

fun DialogmeldingFraBehandlerKafkaDto.medMaskertForesporselSvar(): DialogmeldingFraBehandlerKafkaDto =
    copy(
        dialogmelding =
            dialogmelding.copy(
                foresporselFraSaksbehandlerForesporselSvar =
                    dialogmelding.foresporselFraSaksbehandlerForesporselSvar?.copy(
                        tekstNotatInnhold = "***",
                    ),
            ),
    )
