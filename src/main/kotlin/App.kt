import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.naisful.naisApp
import db.PgTransactionProvider
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
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
    app(
        kafkaConfig = kafkaConfig,
    )
}

fun app(
    kafkaConfig: KafkaConfig,
) {
    val factory = ConsumerProducerFactory(kafkaConfig.aivenConfig)
    val running = AtomicBoolean(false)
    val kafkaConsumer =
        KafkaConsumer(
            topics = kafkaConfig.readTopics,
            consumerGroupId = System.getenv("KAFKA_CONSUMER_GROUP_ID"),
            readyToConsume = running,
            factory,
        )

    val transactionProvider = PgTransactionProvider()
    val kafkaProducer = KafkaProducer(kafkaConfig.writeTopic, factory, transactionProvider)

    naisApp(
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        objectMapper = jacksonObjectMapper(),
        applicationLogger = LoggerFactory.getLogger("Application"),
        callLogger = LoggerFactory.getLogger("CallLogger"),
        applicationModule = {
            this.monitor.subscribe(ApplicationStarted) {
                running.set(true)
                launch { kafkaConsumer.start() }
                launch { kafkaProducer.start() }
            }
            this.monitor.subscribe(ApplicationStopping) {
                running.set(false)
            }
        },
    ).start(wait = true)
}
