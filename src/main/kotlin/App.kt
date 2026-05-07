import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.naisful.naisApp
import db.DataSourceBuilder
import db.DbConfig
import db.PgTransactionProvider
import db.objectMapper
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kafka.KafkaConfig
import kafka.KafkaConsumer
import kafka.KafkaProducer
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

const val DIALOGMELDING_FRA_NAY_TOPIC = "teamsykefravr.isdialogmelding-behandler-dialogmelding-bestilling"
const val DIALOGMELDING_STATUS_TOPIC = "teamsykefravr.behandler-dialogmelding-status"
const val DIALOGMELDING_FRA_BEHANDLER_TOPIC = "teamsykefravr.melding-fra-behandler"
const val LEGEERKLÆRING_TOPIC = "teamsykmelding.legeerklaering"

fun main() {
    val env = System.getenv()
    val kafkaConfig =
        KafkaConfig(
            aivenConfig = AivenConfig.default,
            readTopics =
                listOf(
                    DIALOGMELDING_STATUS_TOPIC,
                    DIALOGMELDING_FRA_BEHANDLER_TOPIC,
                    LEGEERKLÆRING_TOPIC,
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
    val kafkaConsumer =
        KafkaConsumer(
            topics = kafkaConfig.readTopics,
            consumerGroupId = System.getenv("KAFKA_CONSUMER_GROUP_ID") ?: "local-group",
            readyToConsume = running,
            factory,
        )

    val dataSourceBuilder = DataSourceBuilder(dbConfig)

    val transactionProvider = PgTransactionProvider(dataSourceBuilder.build())
    val kafkaProducer = KafkaProducer(kafkaConfig.writeTopic, factory, transactionProvider)

    naisApp(
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        objectMapper = objectMapper,
        applicationLogger = LoggerFactory.getLogger("Application"),
        callLogger = LoggerFactory.getLogger("CallLogger"),
        port = port,
        applicationModule = {
            this.monitor.subscribe(ApplicationStarting) {
                dataSourceBuilder.migrate()
            }
            this.monitor.subscribe(ApplicationStarted) {
                running.set(true)
                launch { kafkaConsumer.start() }
                launch { kafkaProducer.start() }
            }
            this.monitor.subscribe(ApplicationStopping) {
                running.set(false)
            }

            install(OpenApi) { configureOpenApiPlugin() }
            // TODO auth???
            routing {
                route("/api") {
                    route("/openapi.json") {
                        openApi()
                    }
                    route("swagger") {
                        swaggerUI("../openapi.json")
                    }

                    get("/dialogmeldinger") {
                        call.respondText("OK")
                    }
                }
            }
        },
    ).start(wait = true)
}
