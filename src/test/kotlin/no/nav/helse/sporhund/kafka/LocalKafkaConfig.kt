package no.nav.helse.sporhund.kafka

import com.github.navikt.tbd_libs.kafka.Config
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import java.util.*

class LocalKafkaConfig(
    private val brokers: String = "localhost:9092",
) : Config {
    override fun producerConfig(properties: Properties) =
        Properties().apply {
            putAll(baseConfig())
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.LINGER_MS_CONFIG, "0")
            putAll(properties)
        }

    override fun consumerConfig(
        groupId: String,
        properties: Properties,
    ) = Properties().apply {
        putAll(baseConfig())
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
        put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
        putAll(properties)
    }

    override fun adminConfig(properties: Properties) =
        Properties().apply {
            putAll(baseConfig())
            putAll(properties)
        }

    private fun baseConfig() =
        Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
        }
}
