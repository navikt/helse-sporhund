package no.nav.helse.sporhund.infrastructure.db

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotliquery.Session
import no.nav.helse.sporhund.application.kommando.JournalførDialogmelding
import no.nav.helse.sporhund.application.kommando.Kommando
import no.nav.helse.sporhund.application.kommando.KommandoId
import no.nav.helse.sporhund.application.kommando.KommandoKø
import no.nav.helse.sporhund.application.kommando.KøetKommando
import no.nav.helse.sporhund.application.kommando.LagDialogmeldingPdf
import no.nav.helse.sporhund.application.kommando.SendDialogmeldingPåKafka
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.DialogmeldingId
import tools.jackson.module.kotlin.readValue
import java.time.Instant
import java.util.UUID

class PgKommandoKø(
    private val session: Session,
) : KommandoKø {
    override fun leggTil(kommando: Kommando) {
        asSQL(
            """
            INSERT INTO kommando(id, kommando, type, conversation_ref)
            VALUES (:id, :kommando::jsonb, :type, :conversationRef)
            """.trimIndent(),
            "id" to kommando.id.value,
            "kommando" to objectMapper.writeValueAsString(kommando.tilDto()),
            "type" to kommando.typenavn(),
            "conversationRef" to kommando.conversationRef.value,
        ).update(session)
    }

    // Eldre rader som er låst av andre transaksjoner, er fortsatt synlige i NOT EXISTS og blokkerer derfor.
    override fun nesteKommando(nå: Instant): KøetKommando? =
        asSQL(
            """
            SELECT kommando, antall_forsøk FROM kommando k
            WHERE utført_tidspunkt IS NULL
            AND (neste_forsøk IS NULL OR neste_forsøk <= :naa)
            AND NOT EXISTS (
                SELECT 1 FROM kommando eldre
                WHERE eldre.utført_tidspunkt IS NULL
                  AND eldre.type = k.type
                  AND eldre.conversation_ref = k.conversation_ref
                  AND eldre.sekvens < k.sekvens
            )
            ORDER BY sekvens
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """.trimIndent(),
            "naa" to nå,
        ).single(session) { row ->
            KøetKommando(
                kommando = objectMapper.readValue<KommandoDto>(row.string("kommando")).tilDomene(),
                antallForsøk = row.int("antall_forsøk"),
            )
        }

    override fun utført(id: KommandoId) {
        asSQL(
            """UPDATE kommando SET utført_tidspunkt = now() WHERE id = :id""",
            "id" to id.value,
        ).update(session)
    }

    override fun feilet(
        id: KommandoId,
        antallForsøk: Int,
        feilklasse: String,
        nesteForsøk: Instant,
    ) {
        asSQL(
            """
            UPDATE kommando
            SET antall_forsøk = :antallForsok, siste_feil = :feilklasse, neste_forsøk = :nesteForsok
            WHERE id = :id
            """.trimIndent(),
            "id" to id.value,
            "antallForsok" to antallForsøk,
            "feilklasse" to feilklasse,
            "nesteForsok" to nesteForsøk,
        ).update(session)
    }

    private fun Kommando.typenavn(): String =
        when (this) {
            is LagDialogmeldingPdf -> "LagDialogmeldingPdf"
            is SendDialogmeldingPåKafka -> "SendDialogmeldingPåKafka"
            is JournalførDialogmelding -> "JournalførDialogmelding"
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
        val avsenderNavn: String?,
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
