package no.nav.helse.sporhund.application.kommando

import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.Saksbehandler
import java.util.UUID

@JvmInline
value class KommandoId(
    val value: UUID,
)

sealed interface Kommando {
    val id: KommandoId
    val conversationRef: ConversationRef
}

/**
 * Meldingen og dialogen endres ikke etter at de er lagret, så PDF-en lages fra dem.
 * Navnet på avsenderen lagres ikke i dialogen og må derfor følge med kommandoen.
 */
data class LagDialogmeldingPdf(
    override val id: KommandoId,
    override val conversationRef: ConversationRef,
    val meldingId: DialogmeldingId<UUID>,
    val avsenderNavn: String,
) : Kommando {
    companion object {
        fun ny(
            melding: Dialogmelding.FraNav,
            dialog: Dialog,
            avsender: Saksbehandler,
        ) = LagDialogmeldingPdf(
            id = KommandoId(UUID.randomUUID()),
            conversationRef = dialog.conversationRef,
            meldingId = melding.id,
            avsenderNavn = avsender.navn,
        )
    }
}

data class SendDialogmeldingPåKafka(
    override val id: KommandoId,
    override val conversationRef: ConversationRef,
    val meldingId: DialogmeldingId<UUID>,
) : Kommando

data class JournalførDialogmelding(
    override val id: KommandoId,
    override val conversationRef: ConversationRef,
    val meldingId: DialogmeldingId<UUID>,
) : Kommando
