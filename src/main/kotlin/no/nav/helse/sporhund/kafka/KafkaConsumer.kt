package no.nav.helse.sporhund.kafka

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.kafka.poll
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.application.logg.loggWarn
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.errors.WakeupException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class KafkaConsumer(
    private val topics: ReadTopics,
    consumerGroupId: String,
    private val readyToConsume: AtomicBoolean,
    consumerProducerFactory: ConsumerProducerFactory,
    private val transactionProvider: TransactionProvider,
) {
    private val defaultConsumerProperties =
        Properties().apply {
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        }
    private val consumer = consumerProducerFactory.createConsumer(consumerGroupId, defaultConsumerProperties)

    fun start() {
        consumer.use {
            consumer.subscribe(topics.alleTopics)

            try {
                loggInfo("Etablerer consumer på følgende topics: ${topics.alleTopics.joinToString(", ")}")
                consumer.poll(readyToConsume::get) { records ->
                    records.forEach { record ->
                        if (record.topic() == topics.dialogmeldingFraBehandlerTopic) this.håndterSvarFraBehandler(transactionProvider, record)
                        // lytt på og oppdater status
                        // lytt på melding fra behandler og knytt til dialog
                        // lytt på legeerklæringer og knytt til dialog
                    }
                }
            } catch (err: WakeupException) {
                loggWarn(err.message ?: "Ukjent melding i exception", err)
                loggInfo("Lukker kafka consumer som følge av ${if (!readyToConsume.get()) "mottatt shutdown signal" else "exception" }")
            }
        }
    }
}
