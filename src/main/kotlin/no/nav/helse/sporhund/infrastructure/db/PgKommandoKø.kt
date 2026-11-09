package no.nav.helse.sporhund.infrastructure.db

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotliquery.Session
import no.nav.helse.sporhund.application.kommando.JournalførDialogmelding
import no.nav.helse.sporhund.application.kommando.Kommando
import no.nav.helse.sporhund.application.kommando.KommandoId
import no.nav.helse.sporhund.application.kommando.KommandoKø
import no.nav.helse.sporhund.application.kommando.LagDialogmeldingPdf
import no.nav.helse.sporhund.application.kommando.SendDialogmeldingPåKafka
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.DialogmeldingId
import tools.jackson.module.kotlin.readValue
import java.util.UUID

class PgKommandoKø(
    private val session: Session,
) : KommandoKø {
    override fun leggTil(kommando: Kommando) {
        asSQL(
            """
            INSERT INTO kommando(id, kommando)
            VALUES (:id, :kommando::jsonb)
            """.trimIndent(),
            "id" to kommando.id.value,
            "kommando" to objectMapper.writeValueAsString(kommando.tilDto()),
        ).update(session)
    }

    override fun nesteKommando(): Kommando? =
        asSQL(
            """
            SELECT kommando FROM kommando
            ORDER BY opprettet
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """.trimIndent(),
        ).single(session) { row ->
            objectMapper.readValue<KommandoDto>(row.string("kommando")).tilDomene()
        }

    override fun utført(id: KommandoId) {
        asSQL(
            """DELETE FROM kommando WHERE id = :id""",
            "id" to id.value,
        ).update(session)
    }

    private fun Kommando.tilDto(): KommandoDto =
        when (this) {
            is LagDialogmeldingPdf ->
                LagDialogmeldingPdfDto(
                    id = id.value,
                    conversationRef = conversationRef.value,
                    meldingId = meldingId.value,
                    avsenderNavn = avsenderNavn,
                )
            is SendDialogmeldingPåKafka ->
                SendDialogmeldingPåKafkaDto(id = id.value, conversationRef = conversationRef.value, meldingId = meldingId.value)
            is JournalførDialogmelding ->
                JournalførDialogmeldingDto(id = id.value, conversationRef = conversationRef.value, meldingId = meldingId.value)
        }

    private fun KommandoDto.tilDomene(): Kommando =
        when (this) {
            is LagDialogmeldingPdfDto ->
                LagDialogmeldingPdf(
                    id = KommandoId(id),
                    conversationRef = ConversationRef(conversationRef),
                    meldingId = DialogmeldingId(meldingId),
                    avsenderNavn = avsenderNavn,
                )
            is SendDialogmeldingPåKafkaDto ->
                SendDialogmeldingPåKafka(KommandoId(id), ConversationRef(conversationRef), DialogmeldingId(meldingId))
            is JournalførDialogmeldingDto ->
                JournalførDialogmelding(KommandoId(id), ConversationRef(conversationRef), DialogmeldingId(meldingId))
        }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = LagDialogmeldingPdfDto::class, name = "LagDialogmeldingPdf"),
        JsonSubTypes.Type(value = SendDialogmeldingPåKafkaDto::class, name = "SendDialogmeldingPåKafka"),
        JsonSubTypes.Type(value = JournalførDialogmeldingDto::class, name = "JournalførDialogmelding"),
    )
    private sealed interface KommandoDto

    private data class LagDialogmeldingPdfDto(
        val id: UUID,
        val conversationRef: UUID,
        val meldingId: UUID,
        val avsenderNavn: String,
    ) : KommandoDto

    private data class SendDialogmeldingPåKafkaDto(
        val id: UUID,
        val conversationRef: UUID,
        val meldingId: UUID,
    ) : KommandoDto

    private data class JournalførDialogmeldingDto(
        val id: UUID,
        val conversationRef: UUID,
        val meldingId: UUID,
    ) : KommandoDto
}
