package no.nav.helse.sporhund

import no.nav.helse.sporhund.db.testhelpers.TestcontainersDatabase
import no.nav.helse.sporhund.kafka.KafkaConfig
import no.nav.helse.sporhund.kafka.LocalKafkaConfig
import no.nav.helse.sporhund.kafka.ReadTopics
import org.testcontainers.kafka.KafkaContainer

fun main() {
    val kafka = KafkaContainer("apache/kafka:3.7.1").apply { start() }
    val postgres = TestcontainersDatabase("local-app")

    Runtime.getRuntime().addShutdownHook(
        Thread {
            kafka.stop()
            postgres.stop()
        },
    )

    val topics =
        ReadTopics(
            dialogmeldingFraBehandlerTopic = DIALOGMELDING_FRA_BEHANDLER_TOPIC,
            dialogmeldingStatusTopic = DIALOGMELDING_STATUS_TOPIC,
            legeerklæringTopic = LEGEERKLÆRING_TOPIC,
        )

    app(
        kafkaConfig =
            KafkaConfig(
                aivenConfig = LocalKafkaConfig(kafka.bootstrapServers),
                readTopics = topics,
                writeTopic = DIALOGMELDING_FRA_NAY_TOPIC,
            ),
        dbConfig = postgres.dbConfig,
        port = 8282,
    )
}
