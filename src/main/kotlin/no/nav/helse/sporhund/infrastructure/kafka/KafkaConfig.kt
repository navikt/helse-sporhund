package no.nav.helse.sporhund.infrastructure.kafka

import com.github.navikt.tbd_libs.kafka.Config

data class KafkaConfig(
    val aivenConfig: Config,
    val readTopics: ReadTopics,
    val writeTopic: String,
)

class ReadTopics(
    val dialogmeldingFraBehandlerTopic: String,
) {
    val alleTopics = listOf(dialogmeldingFraBehandlerTopic)
}
