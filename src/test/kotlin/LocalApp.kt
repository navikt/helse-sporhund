import db.testhelpers.TestcontainersDatabase
import kafka.KafkaConfig
import kafka.LocalKafkaConfig
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
        listOf(
            DIALOGMELDING_STATUS_TOPIC,
            DIALOGMELDING_FRA_BEHANDLER_TOPIC,
            LEGEERKLÆRING_TOPIC,
        )

    app(
        kafkaConfig =
            KafkaConfig(
                aivenConfig = LocalKafkaConfig(kafka.bootstrapServers),
                readTopics = topics,
                writeTopic = DIALOGMELDING_FRA_NAY_TOPIC,
            ),
        dbConfig = postgres.testDbConfig,
        port = 8282,
    )
}
