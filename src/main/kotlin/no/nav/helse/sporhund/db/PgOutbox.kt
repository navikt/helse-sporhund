package no.nav.helse.sporhund.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.sporhund.application.Outbox
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.OutboxMeldingId
import no.nav.helse.sporhund.domain.BehandlerRef
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.Identitetsnummer
import no.nav.helse.sporhund.domain.NyDialogmeldingFraNavEvent
import java.time.Instant
import java.util.*

class PgOutbox(
    private val session: Session,
) : Outbox {
    override fun nyMelding(melding: OutboxMelding) {
        val dto =
            NyDialogmeldingFraNavDto(
                conversationRef = melding.event.conversationRef.value,
                behandlerRef = melding.event.behandlerRef.value,
                identitetsnummer = melding.event.identitetsnummer.value,
                meldingId = melding.event.meldingId.value,
                tekst = melding.event.tekst,
            )
        asSQL(
            """
            INSERT INTO outbox(id, event, opprettet, sendt_tidspunkt)
            VALUES (:id, :event::jsonb, now(), null)
            """.trimIndent(),
            "id" to melding.id.value,
            "event" to objectMapper.writeValueAsString(dto),
            "opprettet" to Instant.now(),
            "sendt_tidspunkt" to null,
        ).update(session)
    }

    override fun meldinger(): List<OutboxMelding> =
        asSQL(
            """SELECT id, event FROM outbox WHERE sendt_tidspunkt IS NULL""",
        ).list(session) { row ->
            OutboxMelding(
                id = OutboxMeldingId(row.uuid("id")),
                event =
                    objectMapper.readValue<NyDialogmeldingFraNavDto>(row.string("event")).let {
                        NyDialogmeldingFraNavEvent(
                            conversationRef = ConversationRef(it.conversationRef),
                            behandlerRef = BehandlerRef(it.behandlerRef),
                            identitetsnummer = Identitetsnummer.fraString(it.identitetsnummer),
                            meldingId = DialogmeldingId(it.meldingId),
                            tekst = it.tekst,
                        )
                    },
            )
        }

    override fun meldingSendt(id: OutboxMeldingId) {
        asSQL(
            """UPDATE outbox SET sendt_tidspunkt = :tidspunkt WHERE id = :id""",
            "tidspunkt" to Instant.now(),
            "id" to id.value,
        ).update(session)
    }

    private data class NyDialogmeldingFraNavDto(
        val conversationRef: UUID,
        val behandlerRef: String,
        val identitetsnummer: String,
        val meldingId: UUID,
        val tekst: String?,
    )
}
