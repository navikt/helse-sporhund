package kafka

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.kafka.poll
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.errors.WakeupException
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.set

class KafkaConsumer(
    private val topics: List<String>,
    consumerGroupId: String,
    private val readyToConsume: AtomicBoolean,
    consumerProducerFactory: ConsumerProducerFactory,
) {
    private val defaultConsumerProperties =
        Properties().apply {
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        }
    private val consumer = consumerProducerFactory.createConsumer(consumerGroupId, defaultConsumerProperties)

    fun start() {
        consumer.use {
            consumer.subscribe(topics)

            try {
                consumer.poll(readyToConsume::get) { records ->
                    records.forEach { record ->
                        // lytt på og oppdater status
                        // lytt på melding fra behandler og knytt til dialog
                        // lytt på legeerklæringer og knytt til dialog
                    }
                }
            } catch (err: WakeupException) {
//            log.info("Exiting consumer after ${if (!running.get()) "receiving shutdown signal" else "being interrupted by someone" }")
            }
        }
    }
}
