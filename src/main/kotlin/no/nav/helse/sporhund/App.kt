package no.nav.helse.sporhund

import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.naisful.naisApp
import io.github.smiley4.ktoropenapi.OpenApi
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import no.nav.helse.sporhund.api.appRoutes
import no.nav.helse.sporhund.api.configureOpenApiPlugin
import no.nav.helse.sporhund.db.DataSourceBuilder
import no.nav.helse.sporhund.db.DbConfig
import no.nav.helse.sporhund.db.PgTransactionProvider
import no.nav.helse.sporhund.db.objectMapper
import no.nav.helse.sporhund.kafka.KafkaConfig
import no.nav.helse.sporhund.kafka.KafkaConsumer
import no.nav.helse.sporhund.kafka.KafkaProducer
import no.nav.helse.sporhund.kafka.ReadTopics
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

const val DIALOGMELDING_FRA_NAY_TOPIC = "teamsykefravr.isdialogmelding-behandler-dialogmelding-bestilling"
const val DIALOGMELDING_STATUS_TOPIC = "teamsykefravr.behandler-dialogmelding-status"
const val DIALOGMELDING_FRA_BEHANDLER_TOPIC = "teamsykefravr.dialogmelding"
const val LEGEERKLÆRING_TOPIC = "teamsykmelding.legeerklaering"

fun main() {
    val env = System.getenv()
    val kafkaConfig =
        KafkaConfig(
            aivenConfig = AivenConfig.default,
            readTopics =
                ReadTopics(
                    dialogmeldingFraBehandlerTopic = DIALOGMELDING_FRA_BEHANDLER_TOPIC,
                    dialogmeldingStatusTopic = DIALOGMELDING_STATUS_TOPIC,
                    legeerklæringTopic = DIALOGMELDING_STATUS_TOPIC, // TODO: Endre tilbake til legeerklæring-topicet når vi har fått tilgang
                ),
            writeTopic = DIALOGMELDING_FRA_NAY_TOPIC,
        )
    val dbConfig =
        DbConfig(
            jdbcUrl = env.getValue("DATABASE_JDBC_URL"),
            username = env.getValue("DATABASE_USERNAME"),
            password = env.getValue("DATABASE_PASSWORD"),
        )
    app(
        kafkaConfig = kafkaConfig,
        dbConfig = dbConfig,
    )
}

fun app(
    kafkaConfig: KafkaConfig,
    dbConfig: DbConfig,
    port: Int = 8080,
) {
    val factory = ConsumerProducerFactory(kafkaConfig.aivenConfig)
    val running = AtomicBoolean(false)

    val dataSourceBuilder = DataSourceBuilder(dbConfig)

    val transactionProvider = PgTransactionProvider(dataSourceBuilder.build())
    val kafkaConsumer =
        KafkaConsumer(
            topics = kafkaConfig.readTopics,
            consumerGroupId = System.getenv("KAFKA_CONSUMER_GROUP_ID") ?: "local-group",
            readyToConsume = running,
            consumerProducerFactory = factory,
            transactionProvider = transactionProvider,
        )
    val kafkaProducer = KafkaProducer(kafkaConfig.writeTopic, factory, transactionProvider)

    naisApp(
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        objectMapper = objectMapper,
        applicationLogger = LoggerFactory.getLogger("tjenestekall"),
        callLogger = LoggerFactory.getLogger("tjenestekall"),
        port = port,
        developmentMode = false,
        gracefulShutdownDelay = 10.seconds,
        applicationModule = {
            this.monitor.subscribe(ApplicationStarted) {
                dataSourceBuilder.migrate()
                running.set(true)
                val exceptionHandler =
                    CoroutineExceptionHandler { _, throwable ->
                        it.log.error("Feil i coroutine, terminerer appen", throwable)
                        it.engine.stop()
                    }
                launch(exceptionHandler) { kafkaConsumer.start() }
                launch(exceptionHandler) { kafkaProducer.start() }
            }
            this.monitor.subscribe(ApplicationStopping) {
                running.set(false)
            }

            install(OpenApi) { configureOpenApiPlugin() }
            // TODO auth???
            routing {
                appRoutes()
            }
        },
    ).start(wait = true)
}
