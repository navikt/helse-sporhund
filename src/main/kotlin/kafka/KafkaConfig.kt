package kafka

import com.github.navikt.tbd_libs.kafka.Config

data class KafkaConfig(
    val aivenConfig: Config,
    val readTopics: ReadTopics,
    val writeTopic: String,
)

class ReadTopics(
    val dialogmeldingFraBehandlerTopic: String,
    val dialogmeldingStatusTopic: String,
    val legeerklæringTopic: String,
) {
    val alleTopics = listOf(dialogmeldingFraBehandlerTopic, dialogmeldingStatusTopic, legeerklæringTopic)
}
