package no.nav.helse.sporhund.kafka.testhelpers

import org.testcontainers.kafka.KafkaContainer

class TestcontainersKafka(
    moduleLabel: String,
) {
    private val kafka =
        KafkaContainer("apache/kafka:3.7.1")
            .withLabel("app", "sporhund")
            .withLabel("module", moduleLabel)
            .withLabel("code-location", javaClass.canonicalName)
            .apply { start() }

    val config =
        LocalKafkaConfig(
            kafka.bootstrapServers,
        )

    fun stop() = kafka.stop()
}
