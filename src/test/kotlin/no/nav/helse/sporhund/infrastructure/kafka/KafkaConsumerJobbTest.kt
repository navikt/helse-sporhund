package no.nav.helse.sporhund.infrastructure.kafka

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import kotlinx.coroutines.*
import no.nav.helse.sporhund.application.InMemoryTransactionProvider
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagFraNavMelding
import no.nav.helse.sporhund.domain.testhelpers.lagFraSystemMelding
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.TestcontainersKafka
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.lagDialogmeldingFraBehandlerKafkaDto
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class KafkaConsumerJobbTest {
    private val kafka = TestcontainersKafka("test-kafka")
    private val consumerProducerFactory = ConsumerProducerFactory(kafka.config)
    private val dialogmeldingFraBehandlerTopic = "dialogmelding-fra-behandler-topic"
    private val dialogmeldingStatusTopic = "dialogmelding-status"
    private val producer = consumerProducerFactory.createProducer()
    private val transactionProvider = InMemoryTransactionProvider()
    val readyToConsume = AtomicBoolean(true)
    private val consumer =
        KafkaConsumerJobb(
            topics =
                ReadTopics(
                    dialogmeldingFraBehandlerTopic,
                    dialogmeldingStatusTopic,
                ),
            consumerGroupId = "test-consumer",
            readyToConsume = readyToConsume,
            consumerProducerFactory = consumerProducerFactory,
            transactionProvider = transactionProvider,
        )

    @Test
    fun `knytter gyldig innkommende melding til dialog`() {
        // given
        val dialogSomSkalIgnoreres = lagDialog()
        val dialog = lagDialog()
        transactionProvider.dialogRepository.lagre(dialogSomSkalIgnoreres)
        transactionProvider.dialogRepository.lagre(dialog)

        // melding produsert FØR consumer starter skal ikke leses (auto.offset.reset = latest)
        producer.send(
            ProducerRecord(
                dialogmeldingFraBehandlerTopic,
                objectMapper.writeValueAsString(lagDialogmeldingFraBehandlerKafkaDto(conversationRef = dialogSomSkalIgnoreres.conversationRef.value.toString())),
            ),
        )

        // when
        lesTilEndenAvTopicetOgStoppConsumer {
            producer.send(
                ProducerRecord(
                    dialogmeldingFraBehandlerTopic,
                    objectMapper.writeValueAsString(lagDialogmeldingFraBehandlerKafkaDto(conversationRef = dialog.conversationRef.value.toString())),
                ),
            )
        }

        // then
        val ignorert = transactionProvider.dialogRepository.finnDialog(dialogSomSkalIgnoreres.conversationRef)
        assertNotNull(ignorert)
        assertEquals(1, ignorert.meldinger.size, "Melding produsert før consumer startet skal ikke ha blitt lest")

        val funnet = transactionProvider.dialogRepository.finnDialog(dialog.conversationRef)
        assertNotNull(funnet)
        assertEquals(2, funnet.meldinger.size)
    }

    @Test
    fun `OK status setter kvitteringMottatt på FraNav-melding`() {
        // given
        val fraNavMelding = lagFraNavMelding()
        val dialog = lagDialog(melding = fraNavMelding)
        transactionProvider.dialogRepository.lagre(dialog)

        // when
        lesTilEndenAvTopicetOgStoppConsumer(dialogmeldingStatusTopic) {
            producer.send(
                ProducerRecord(
                    dialogmeldingStatusTopic,
                    lagStatusDto(bestillingUuid = fraNavMelding.id.value, status = "OK"),
                ),
            )
        }

        // then
        val lagretDialog = transactionProvider.dialogRepository.finnDialog(dialog.conversationRef)
        assertNotNull(lagretDialog)
        val melding = lagretDialog.meldinger.first() as Dialogmelding.FraNav
        assertTrue(melding.kvitteringMottatt)
        assertEquals(Dialogstatus.ForespørselSendt, lagretDialog.status)
    }

    @Test
    fun `AVVIST status setter kvitteringMottatt og endrer status til Avvist`() {
        // given
        val fraNavMelding = lagFraNavMelding()
        val dialog = lagDialog(melding = fraNavMelding)
        transactionProvider.dialogRepository.lagre(dialog)

        // when
        lesTilEndenAvTopicetOgStoppConsumer(dialogmeldingStatusTopic) {
            producer.send(
                ProducerRecord(
                    dialogmeldingStatusTopic,
                    lagStatusDto(bestillingUuid = fraNavMelding.id.value, status = "AVVIST"),
                ),
            )
        }

        // then
        val lagretDialog = transactionProvider.dialogRepository.finnDialog(dialog.conversationRef)
        assertNotNull(lagretDialog)
        val melding = lagretDialog.meldinger.first() as Dialogmelding.FraNav
        assertTrue(melding.kvitteringMottatt)
        assertEquals(Dialogstatus.Avvist, lagretDialog.status)
    }

    @Test
    fun `OK status setter kvitteringMottatt på FraSystem-melding (purring)`() {
        // given
        val fraSystemMelding = lagFraSystemMelding()
        val dialog = lagDialog(melding = lagFraNavMelding())
        dialog.nyMelding(fraSystemMelding)
        transactionProvider.dialogRepository.lagre(dialog)

        // when
        lesTilEndenAvTopicetOgStoppConsumer(dialogmeldingStatusTopic) {
            producer.send(
                ProducerRecord(
                    dialogmeldingStatusTopic,
                    lagStatusDto(bestillingUuid = fraSystemMelding.id.value, status = "OK"),
                ),
            )
        }

        // then
        val lagretDialog = transactionProvider.dialogRepository.finnDialog(dialog.conversationRef)
        assertNotNull(lagretDialog)
        val purring = lagretDialog.meldinger.filterIsInstance<Dialogmelding.FraSystem>().first()
        assertTrue(purring.kvitteringMottatt)
    }

    @Test
    fun `AVVIST status på purring endrer status til Avvist`() {
        // given
        val fraSystemMelding = lagFraSystemMelding()
        val dialog = lagDialog(melding = lagFraNavMelding())
        dialog.nyMelding(fraSystemMelding)
        transactionProvider.dialogRepository.lagre(dialog)

        // when
        lesTilEndenAvTopicetOgStoppConsumer(dialogmeldingStatusTopic) {
            producer.send(
                ProducerRecord(
                    dialogmeldingStatusTopic,
                    lagStatusDto(bestillingUuid = fraSystemMelding.id.value, status = "AVVIST"),
                ),
            )
        }

        // then
        val lagretDialog = transactionProvider.dialogRepository.finnDialog(dialog.conversationRef)
        assertNotNull(lagretDialog)
        assertEquals(Dialogstatus.Avvist, lagretDialog.status)
    }

    @Test
    fun `ukjent status ignoreres og endrer ikke dialog`() {
        // given
        val fraNavMelding = lagFraNavMelding()
        val dialog = lagDialog(melding = fraNavMelding)
        transactionProvider.dialogRepository.lagre(dialog)

        // when
        lesTilEndenAvTopicetOgStoppConsumer(dialogmeldingStatusTopic) {
            producer.send(
                ProducerRecord(
                    dialogmeldingStatusTopic,
                    lagStatusDto(bestillingUuid = fraNavMelding.id.value, status = "SENDT"),
                ),
            )
        }

        // then
        val lagretDialog = transactionProvider.dialogRepository.finnDialog(dialog.conversationRef)
        assertNotNull(lagretDialog)
        val melding = lagretDialog.meldinger.first() as Dialogmelding.FraNav
        assertTrue(!melding.kvitteringMottatt)
        assertEquals(Dialogstatus.ForespørselSendt, lagretDialog.status)
    }

    @Test
    fun `bestillingUuid uten match ignoreres uten feil`() {
        // when - should not throw
        lesTilEndenAvTopicetOgStoppConsumer(dialogmeldingStatusTopic) {
            producer.send(
                ProducerRecord(
                    dialogmeldingStatusTopic,
                    lagStatusDto(bestillingUuid = UUID.randomUUID(), status = "OK"),
                ),
            )
        }

        // then - ingen endringer i repository
        assertTrue(transactionProvider.dialogRepository.finnÅpneDialoger().isEmpty())
    }

    private fun lagStatusDto(
        bestillingUuid: UUID,
        status: String,
    ): String =
        objectMapper.writeValueAsString(
            DialogmeldingStatusKafkaDto(
                uuid = UUID.randomUUID().toString(),
                createdAt = OffsetDateTime.now(),
                status = status,
                tekst = null,
                bestillingUuid = bestillingUuid.toString(),
            ),
        )

    private fun lesTilEndenAvTopicetOgStoppConsumer(
        topic: String = dialogmeldingFraBehandlerTopic,
        onConsumerReady: () -> Unit = {},
    ) {
        runBlocking {
            val consumerJob = launch(Dispatchers.IO) { consumer.start() }

            kafka.waitForConsumerGroupAssignment("test-consumer")
            delay(500.milliseconds) // gi consumer tid til å fullføre første poll og sette posisjon til latest
            onConsumerReady()

            val done = CompletableDeferred<Unit>()
            val watchJob =
                launch {
                    while (isActive) {
                        if (kafka.isOnLatestOffset("test-consumer", topic)) {
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
