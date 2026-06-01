package no.nav.helse.sporhund.infrastructure.kafka

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.application.NyDialogmeldingFraNav
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.application.meldinger
import no.nav.helse.sporhund.domain.NyDialogmeldingFraNavEvent
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import org.apache.kafka.clients.producer.ProducerRecord
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
                    outbox.meldinger<NyDialogmeldingFraNav>().forEach {
                        val event = it.nyDialogmeldingFraNavEvent
                        val partitionKey = event.conversationRef.value.toString()
                        val kafkaDto = event.toKafkaDto()
                        producer.send(ProducerRecord(dialogmeldingFraNayTopic, partitionKey, objectMapper.writeValueAsString(kafkaDto)))
                        outbox.meldingSendt(it.id)
                    }
                }
                delay(500.milliseconds)
            }
        }
    }
}

private fun NyDialogmeldingFraNavEvent.toKafkaDto(): DialogmeldingTilBehandlerKafkaDto =
    DialogmeldingTilBehandlerKafkaDto(
        behandlerRef = behandlerRef.value,
        personIdent = identitetsnummer.value,
        dialogmeldingUuid = meldingId.value.toString(),
        dialogmeldingRefParent = null,
        dialogmeldingRefConversation = conversationRef.value.toString(),
        dialogmeldingType = "DIALOG_FORESPORSEL",
        dialogmeldingKodeverk = "FORESPORSEL",
        dialogmeldingKode = 1,
        dialogmeldingTekst = tekst,
        dialogmeldingVedlegg = null,
        kilde = "sykepenger",
    )
