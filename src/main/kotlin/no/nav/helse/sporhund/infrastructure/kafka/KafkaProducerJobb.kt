package no.nav.helse.sporhund.infrastructure.kafka

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.application.NyDialogmeldingFraNav
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.loggDebug
import no.nav.helse.sporhund.application.logg.loggError
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.application.meldinger
import no.nav.helse.sporhund.domain.NyDialogmeldingFraNavEvent
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class KafkaProducerJobb(
    private val dialogmeldingFraNayTopic: String,
    private val readyToProduce: AtomicBoolean,
    consumerProducerFactory: ConsumerProducerFactory,
    private val transactionProvider: TransactionProvider,
) {
    private val producer = consumerProducerFactory.createProducer()
    private val antallMillisekunderJobbenSkalSove = 5000

    fun start() {
        loggInfo("Etablerer kafka producer-jobb på topic $dialogmeldingFraNayTopic")
        runBlocking {
            while (readyToProduce.get()) {
                runCatching {
                    transactionProvider.transaction {
                        val meldinger = outbox.meldinger<NyDialogmeldingFraNav>()
                        if (meldinger.isEmpty()) {
                            loggDebug("Kafka producer-jobb: Ingen meldinger å sende på kafka, sover $antallMillisekunderJobbenSkalSove ms")
                            return@transaction
                        }
                        loggDebug("Kafka producer-jobb: Fant ${meldinger.size} meldinger som skal sendes på kafka")
                        meldinger.forEach {
                            val event = it.nyDialogmeldingFraNavEvent
                            val partitionKey = event.conversationRef.value.toString()
                            val kafkaDto = event.toKafkaDto()
                            producer.send(
                                ProducerRecord(
                                    dialogmeldingFraNayTopic,
                                    partitionKey,
                                    objectMapper.writeValueAsString(kafkaDto),
                                ),
                            )
                            outbox.meldingSendt(it.id)
                        }
                    }
                }.onFailure {
                    loggError(
                        "Kafka producer-jobb: Klarte ikke produsere meldinger til Kafka, meldinger blir ikke sendt til behandlere. Meldingene er ikke kvittert ut i outbox-en",
                        it,
                    )
                }
                delay(antallMillisekunderJobbenSkalSove.milliseconds)
            }
        }
        loggInfo("Lukker kafka producer-jobb")
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
        dialogmeldingKode = if (erPurring) 2 else 1,
        dialogmeldingTekst = tekst,
        dialogmeldingVedlegg = null,
        kilde = "sykepenger",
    )
