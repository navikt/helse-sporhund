import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.naisful.naisApp
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.LoggerFactory

fun main() {
    naisApp(
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        objectMapper = jacksonObjectMapper(),
        applicationLogger = LoggerFactory.getLogger("Application"),
        callLogger = LoggerFactory.getLogger("CallLogger"),
        applicationModule = {}
    ).start(wait = true)
}
