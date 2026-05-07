import application.Outbox
import application.OutboxMelding
import application.OutboxMeldingId
import application.SessionContext
import application.TransactionProvider
import db.DbConfig
import kafka.KafkaConfig
import kafka.LocalKafkaConfig
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer

fun main() {
    val kafka = KafkaContainer("apache/kafka:3.7.1").apply { start() }
    val postgres = PostgreSQLContainer("postgres:17").apply { start() }

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
        dbConfig =
            DbConfig(
                jdbcUrl = postgres.jdbcUrl,
                username = postgres.username,
                password = postgres.password,
            ),
        port = 8282,
        transactionProviderOverride =
            object : TransactionProvider {
                override fun <T> transaction(session: SessionContext.() -> T): T =
                    session(
                        object : SessionContext {
                            override val outbox =
                                object : Outbox {
                                    override fun nyMelding(melding: OutboxMelding) = Unit

                                    override fun meldinger() = emptyList<OutboxMelding>()

                                    override fun meldingSendt(id: OutboxMeldingId) = Unit
                                }
                        },
                    )
            },
    )
}
