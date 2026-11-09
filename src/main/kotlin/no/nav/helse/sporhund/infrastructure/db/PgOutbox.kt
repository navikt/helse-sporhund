package no.nav.helse.sporhund.infrastructure.db

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotliquery.Session
import no.nav.helse.sporhund.application.KnyttInnkommendeJournalpost
import no.nav.helse.sporhund.application.Outbox
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.OutboxMeldingId
import no.nav.helse.sporhund.domain.*
import tools.jackson.module.kotlin.readValue
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

class PgOutbox(
    private val session: Session,
) : Outbox {
    override fun nyMelding(melding: OutboxMelding) {
        val dto =
            when (melding) {
                is KnyttInnkommendeJournalpost ->
                    KnyttInnkommendeJournalpostDto(
                        outboxMeldingId = melding.id.value,
                        journalpostId = melding.journalpostId,
                        conversationRef = melding.conversationRef.value,
                        identitetsnummer = melding.identitetsnummer.value,
                    )
            }
        asSQL(
            """
            INSERT INTO outbox(id, event, opprettet, sendt_tidspunkt)
            VALUES (:id, :event::jsonb, now(), null)
            """.trimIndent(),
            "id" to melding.id.value,
            "event" to objectMapper.writeValueAsString(dto),
        ).update(session)
    }

    override fun <T : OutboxMelding> meldinger(type: KClass<T>): List<T> =
        asSQL(
            """SELECT id, event FROM outbox WHERE sendt_tidspunkt IS NULL FOR UPDATE SKIP LOCKED""",
        ).list(session) { row ->
            when (val dto = objectMapper.readValue<OutboxMeldingDto>(row.string("event"))) {
                is KnyttInnkommendeJournalpostDto ->
                    KnyttInnkommendeJournalpost(
                        id = OutboxMeldingId(dto.outboxMeldingId),
                        journalpostId = dto.journalpostId,
                        conversationRef = ConversationRef(dto.conversationRef),
                        identitetsnummer = Identitetsnummer.fraString(dto.identitetsnummer),
                    )
            }
        }.filterIsInstance(type.java)

    override fun meldingSendt(id: OutboxMeldingId) {
        asSQL(
            """UPDATE outbox SET sendt_tidspunkt = :tidspunkt WHERE id = :id""",
            "tidspunkt" to Instant.now(),
            "id" to id.value,
        ).update(session)
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = KnyttInnkommendeJournalpostDto::class, name = "KnyttInnkommendeJournalpost"),
    )
    private sealed interface OutboxMeldingDto {
        val outboxMeldingId: UUID
    }

    private data class KnyttInnkommendeJournalpostDto(
        override val outboxMeldingId: UUID,
        val journalpostId: String,
        val conversationRef: UUID,
        val identitetsnummer: String,
    ) : OutboxMeldingDto
}
