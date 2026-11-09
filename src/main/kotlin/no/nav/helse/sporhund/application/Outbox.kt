package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.*
import java.util.*
import kotlin.reflect.KClass

interface Outbox {
    fun nyMelding(melding: OutboxMelding)

    fun <T : OutboxMelding> meldinger(type: KClass<T>): List<T>

    fun meldingSendt(id: OutboxMeldingId)
}

inline fun <reified T : OutboxMelding> Outbox.meldinger(): List<T> = meldinger(T::class)

@JvmInline
value class OutboxMeldingId(
    val value: UUID,
)

sealed interface OutboxMelding {
    val id: OutboxMeldingId

    companion object {
        fun knyttInnkommendeJournalpost(
            journalpostId: String,
            dialog: Dialog,
        ) = KnyttInnkommendeJournalpost(
            id = OutboxMeldingId(UUID.randomUUID()),
            journalpostId = journalpostId,
            conversationRef = dialog.conversationRef,
            identitetsnummer = dialog.identitetsnummer,
        )
    }
}

data class KnyttInnkommendeJournalpost(
    override val id: OutboxMeldingId,
    val journalpostId: String,
    val conversationRef: ConversationRef,
    val identitetsnummer: Identitetsnummer,
) : OutboxMelding
