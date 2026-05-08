package no.nav.helse.sporhund.kafka

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.domain.NyDialogmeldingFraNavEvent
import org.apache.kafka.clients.producer.ProducerRecord
import org.intellij.lang.annotations.Language
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class KafkaProducer(
    private val dialogmeldingFraNayTopic: String,
    private val readyToProduce: AtomicBoolean,
    consumerProducerFactory: ConsumerProducerFactory,
    private val transactionProvider: TransactionProvider,
) {
    private val producer = consumerProducerFactory.createProducer()

    fun start() {
        runBlocking {
            this@KafkaProducer.loggInfo("Etablerer producer for outbox")
            while (readyToProduce.get()) {
                transactionProvider.transaction {
                    // sjekk mot outbox-tabellen
                    // hvis det finnes meldinger,
                    val meldinger = outbox.meldinger()
                    meldinger.forEach {
                        val partitionKey =
                            it.event.conversationRef.value
                                .toString()
                        val kafkaJson = it.event.toKafkaJson()
                        producer.send(ProducerRecord(dialogmeldingFraNayTopic, partitionKey, kafkaJson))
                        outbox.meldingSendt(it.id)
                    }
                }
                delay(500.milliseconds)
            }
        }
    }
}

private fun NyDialogmeldingFraNavEvent.toKafkaJson(): String {
    @Language("JSON")
    val json =
        """
          {
              "behandlerRef": "${this.behandlerRef.value}",
              "personIdent": "${this.identitetsnummer.value}",
              "dialogmeldingUuid": "${this.meldingId.value}",
              "dialogmeldingRefParent": null,
              "dialogmeldingRefConversation": ${this.conversationRef.value},
              "dialogmeldingType": ${this.type},
              "dialogmeldingKodeverk": "",
              "dialogmeldingKode": null, 
              "dialogmeldingTekst": ${this.tekst},
              "dialogmeldingVedlegg": null,
              "kilde": "Sykepenger"
        }
        """.trimIndent()

    return json
}
