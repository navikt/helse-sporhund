package kafka

import com.github.navikt.tbd_libs.kafka.AivenConfig

data class KafkaConfig(
    val aivenConfig: AivenConfig,
    val readTopics: List<String>,
    val writeTopic: String,
)
