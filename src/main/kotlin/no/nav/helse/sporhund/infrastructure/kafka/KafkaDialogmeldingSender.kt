package no.nav.helse.sporhund.infrastructure.kafka

import no.nav.helse.sporhund.application.DialogmeldingTilBehandlerSender
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.domain.NyDialogmeldingFraNavEvent
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class KafkaDialogmeldingSender(
    private val topic: String,
    private val producer: Producer<String, String>,
) : DialogmeldingTilBehandlerSender {
    override fun send(
        melding: NyDialogmeldingFraNavEvent,
        vedlegg: ByteArray,
    ) {
        loggInfo(
            "Sender dialogmelding med vedlegg på kafka",
            "dialogmeldingUuid" to melding.meldingId.value,
            "conversationRef" to melding.conversationRef.value,
            "identitetsnummer" to melding.identitetsnummer.value,
        )
        producer
            .send(
                ProducerRecord(
                    topic,
                    melding.conversationRef.value.toString(),
                    objectMapper.writeValueAsString(melding.toKafkaDto(vedlegg)),
                ),
            ).get()
    }
}
