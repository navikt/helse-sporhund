package no.nav.helse.sporhund.infrastructure.kafka

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import kotlinx.coroutines.*
import no.nav.helse.sporhund.application.InMemoryTransactionProvider
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.TestcontainersKafka
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.lagDialogmeldingFraBehandlerKafkaDto
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class KafkaConsumerTest {
    private val kafka = TestcontainersKafka("test-kafka")
    private val consumerProducerFactory = ConsumerProducerFactory(kafka.config)
    private val dialogmeldingFraBehandlerTopic = "dialogmelding-fra-behandler-topic"
    private val producer = consumerProducerFactory.createProducer()
    private val transactionProvider = InMemoryTransactionProvider()
    val readyToConsume = AtomicBoolean(true)
    private val consumer =
        KafkaConsumer(
            topics =
                ReadTopics(
                    dialogmeldingFraBehandlerTopic,
                    "et-status-topic",
                    "et-legeerklaering-topic",
                ),
            consumerGroupId = "test-consumer",
            readyToConsume = readyToConsume,
            consumerProducerFactory = consumerProducerFactory,
            transactionProvider = transactionProvider,
        )

    @Test
    fun `knytter gyldig innkommende melding til dialog`() {
        // given
        val dialog = lagDialog()
        transactionProvider.dialogRepository.lagre(dialog)

        // when
        producer.send(
            ProducerRecord(
                dialogmeldingFraBehandlerTopic,
                objectMapper.writeValueAsString(lagDialogmeldingFraBehandlerKafkaDto(conversationRef = dialog.conversationRef.value.toString())),
            ),
        )

        lesTilEndenAvTopicetOgStoppConsumer()

        // then
        val funnet = transactionProvider.dialogRepository.finnDialog(dialog.conversationRef)
        assertNotNull(funnet)
        assertEquals(2, funnet.meldinger.size)
    }

    private fun lesTilEndenAvTopicetOgStoppConsumer() {
        runBlocking {
            val consumerJob = launch(Dispatchers.IO) { consumer.start() }

            val done = CompletableDeferred<Unit>()
            val watchJob =
                launch {
                    while (isActive) {
                        if (kafka.isOnLatestOffset("test-consumer", dialogmeldingFraBehandlerTopic)) {
                            done.complete(Unit)
                            break
                        }
                        delay(50.milliseconds)
                    }
                }

            withTimeout(10.seconds) { done.await() }
            watchJob.cancelAndJoin()
            readyToConsume.set(false)
            consumerJob.join()
        }
    }
}
