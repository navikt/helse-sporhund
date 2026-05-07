package kafka

import com.github.navikt.tbd_libs.kafka.Config

data class KafkaConfig(
    val aivenConfig: Config,
    val readTopics: List<String>,
    val writeTopic: String,
)
