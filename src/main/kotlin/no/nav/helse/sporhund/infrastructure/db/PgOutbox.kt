package no.nav.helse.sporhund.infrastructure.db

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass
import kotliquery.Session
import no.nav.helse.sporhund.application.NyDialogmeldingFraNav
import no.nav.helse.sporhund.application.OpprettJournalpost
import no.nav.helse.sporhund.application.Outbox
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.OutboxMeldingId
import no.nav.helse.sporhund.domain.*

class PgOutbox(
    private val session: Session
) : Outbox {
    override fun nyMelding(melding: OutboxMelding) {
        val dto =
            when (melding) {
                is NyDialogmeldingFraNav ->
                    NyDialogmeldingFraNavDto(
                        outboxMeldingId = melding.id.value,
                        conversationRef = melding.nyDialogmeldingFraNavEvent.conversationRef.value,
                        behandlerRef = melding.nyDialogmeldingFraNavEvent.behandlerRef.value,
                        identitetsnummer = melding.nyDialogmeldingFraNavEvent.identitetsnummer.value,
                        meldingId = melding.nyDialogmeldingFraNavEvent.meldingId.value,
                        tekst = melding.nyDialogmeldingFraNavEvent.tekst
                    )
                is OpprettJournalpost ->
                    OpprettJournalpostDto(
                        outboxMeldingId = melding.id.value,
                        conversationRef = melding.conversationRef.value
                    )
            }
        asSQL(
            """
            INSERT INTO outbox(id, event, opprettet, sendt_tidspunkt)
            VALUES (:id, :event::jsonb, now(), null)
            """.trimIndent(),
            "id" to melding.id.value,
            "event" to objectMapper.writeValueAsString(dto)
        ).update(session)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : OutboxMelding> meldinger(type: KClass<T>): List<T> =
        asSQL(
            """SELECT id, event FROM outbox WHERE sendt_tidspunkt IS NULL"""
        ).list(session) { row ->
            when (val dto = objectMapper.readValue<OutboxMeldingDto>(row.string("event"))) {
                is NyDialogmeldingFraNavDto ->
                    NyDialogmeldingFraNav(
                        id = OutboxMeldingId(dto.outboxMeldingId),
                        nyDialogmeldingFraNavEvent =
                            NyDialogmeldingFraNavEvent(
                                conversationRef = ConversationRef(dto.conversationRef),
                                behandlerRef = BehandlerRef(dto.behandlerRef),
                                identitetsnummer = Identitetsnummer.fraString(dto.identitetsnummer),
                                meldingId = DialogmeldingId(dto.meldingId),
                                tekst = dto.tekst
                            )
                    )
                is OpprettJournalpostDto ->
                    OpprettJournalpost(
                        id = OutboxMeldingId(dto.outboxMeldingId),
                        conversationRef = ConversationRef(dto.conversationRef)
                    )
            }
        }.filter(type::isInstance) as List<T>

    override fun meldingSendt(id: OutboxMeldingId) {
        asSQL(
            """UPDATE outbox SET sendt_tidspunkt = :tidspunkt WHERE id = :id""",
            "tidspunkt" to Instant.now(),
            "id" to id.value
        ).update(session)
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = NyDialogmeldingFraNavDto::class, name = "NyDialogmeldingFraNav"),
        JsonSubTypes.Type(value = OpprettJournalpostDto::class, name = "OpprettJournalpost")
    )
    private sealed interface OutboxMeldingDto {
        val outboxMeldingId: UUID
    }

    private data class NyDialogmeldingFraNavDto(
        override val outboxMeldingId: UUID,
        val conversationRef: UUID,
        val behandlerRef: String,
        val identitetsnummer: String,
        val meldingId: UUID,
        val tekst: String?
    ) : OutboxMeldingDto

    private data class OpprettJournalpostDto(
        override val outboxMeldingId: UUID,
        val conversationRef: UUID
    ) : OutboxMeldingDto
}
