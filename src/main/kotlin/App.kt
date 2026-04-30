import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.naisful.naisApp
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kafka.KafkaConsumer
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

const val DIALOGMELDING_STATUS_TOPIC = "teamsykefravr.behandler-dialogmelding-status"
const val DIALOGMELDING_FRA_BEHANDLER_TOPIC = "teamsykefravr.melding-fra-behandler"
const val LEGEERKLÆRING_TOPIC = "teamsykmelding.legeerklaering"

fun main() {
    val topics =
        listOf(
            DIALOGMELDING_STATUS_TOPIC,
            DIALOGMELDING_FRA_BEHANDLER_TOPIC,
            LEGEERKLÆRING_TOPIC,
        )
    val config = AivenConfig.default
    val factory = ConsumerProducerFactory(config)
    val running = AtomicBoolean(false)
    val kafkaConsumer =
        KafkaConsumer(
            topics = topics,
            consumerGroupId = System.getenv("KAFKA_CONSUMER_GROUP_ID"),
            readyToConsume = running,
            factory,
        )

    naisApp(
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        objectMapper = jacksonObjectMapper(),
        applicationLogger = LoggerFactory.getLogger("Application"),
        callLogger = LoggerFactory.getLogger("CallLogger"),
        applicationModule = {
            this.monitor.subscribe(ApplicationStarted) {
                running.set(true)
                kafkaConsumer.start()
            }
            this.monitor.subscribe(ApplicationStopping) {
                running.set(false)
            }
        },
    ).start(wait = true)
}
