package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.NyDialogmeldingFraNavEvent
import java.util.UUID
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
        fun nyDialogmeldingFraNav(nyDialogmeldingFraNavEvent: NyDialogmeldingFraNavEvent) =
            NyDialogmeldingFraNav(
                id = OutboxMeldingId(UUID.randomUUID()),
                nyDialogmeldingFraNavEvent = nyDialogmeldingFraNavEvent,
            )

        fun opprettJournalpost(conversationRef: ConversationRef) =
            OpprettJournalpost(
                id = OutboxMeldingId(UUID.randomUUID()),
                conversationRef = conversationRef,
            )
    }
}

data class NyDialogmeldingFraNav(
    override val id: OutboxMeldingId,
    val nyDialogmeldingFraNavEvent: NyDialogmeldingFraNavEvent,
) : OutboxMelding

data class OpprettJournalpost(
    override val id: OutboxMeldingId,
    val conversationRef: ConversationRef,
) : OutboxMelding
