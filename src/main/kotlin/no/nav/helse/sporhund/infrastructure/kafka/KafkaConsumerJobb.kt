package no.nav.helse.sporhund.infrastructure.kafka

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.kafka.poll
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.loggError
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.application.logg.loggWarn
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.errors.WakeupException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class KafkaConsumerJobb(
    private val topics: ReadTopics,
    consumerGroupId: String,
    private val readyToConsume: AtomicBoolean,
    consumerProducerFactory: ConsumerProducerFactory,
    private val transactionProvider: TransactionProvider,
) {
    private val defaultConsumerProperties =
        Properties().apply {
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        }
    private val consumer = consumerProducerFactory.createConsumer(consumerGroupId, defaultConsumerProperties)

    fun start() {
        loggInfo("Etablerer kafka consumer-jobb på følgende topics: ${topics.alleTopics.joinToString(", ")}")
        consumer.use {
            consumer.subscribe(topics.alleTopics)

            try {
                consumer.poll(readyToConsume::get) { records ->
                    records.forEach { record ->
                        // lytt på melding fra behandler og knytt til dialog
                        runCatching {
                            if (record.topic() == topics.dialogmeldingFraBehandlerTopic) this.håndterSvarFraBehandler(transactionProvider, record)
                        }.onFailure {
                            loggError("Kafka consumer-jobb: feil ved lesing av melding, committer ikke offsets", it, "recordKey" to record.key(), "recordValue" to record.value())
                            return@poll
                        }.onSuccess {
                            consumer.commitSync()
                        }
                    }
                }
            } catch (err: WakeupException) {
                loggWarn("Kafka consumer-jobb: " + (err.message ?: "ukjent melding i exception"), err)
                loggInfo("Kafka consumer-jobb: lukker kafka consumer som følge av ${if (!readyToConsume.get()) "mottatt shutdown signal" else "exception" }")
            }
        }
        loggInfo("Lukker kafka consumer-jobb")
    }
}
