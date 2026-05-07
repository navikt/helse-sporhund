package db

import application.Outbox
import application.OutboxMelding
import application.OutboxMeldingId
import com.fasterxml.jackson.module.kotlin.readValue
import domain.*
import kotliquery.Session
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
                type = melding.event.type,
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
                            type = it.type,
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
        val type: String,
        val tekst: String?,
    )
}
