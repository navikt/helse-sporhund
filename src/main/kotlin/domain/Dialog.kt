package domain

import java.time.Instant
import java.util.UUID

@JvmInline
value class DialogId(
    val value: UUID,
)

@JvmInline
value class ConversationRef(
    val value: UUID,
)

class Dialog private constructor(
    val id: DialogId,
    val conversationRef: ConversationRef,
    val identitetsnummer: Identitetsnummer,
    meldinger: List<Dialogmelding>,
) {
    private val _meldinger = meldinger.toMutableList()
    val meldinger get() = _meldinger.toList()

    fun nyMelding(dialogmelding: Dialogmelding) {
        _meldinger.add(dialogmelding)
    }

    companion object {
        fun ny(
            identitetsnummer: Identitetsnummer,
            melding: Dialogmelding.FraNav,
        ): Dialog =
            Dialog(
                id = DialogId(UUID.randomUUID()),
                conversationRef = ConversationRef(UUID.randomUUID()),
                identitetsnummer = identitetsnummer,
                meldinger = emptyList(),
            ).also { it.nyMelding(melding) }

        fun fraLagring(
            id: DialogId,
            conversationRef: ConversationRef,
            identitetsnummer: Identitetsnummer,
            meldinger: List<Dialogmelding>,
        ): Dialog = Dialog(id, conversationRef, identitetsnummer, meldinger)
    }
}

@JvmInline
value class DialogmeldingId(
    val value: UUID,
)

interface Dialogmelding {
    val id: DialogmeldingId
    val tidspunkt: Instant
    val melding: String

    class FraNav(
        override val id: DialogmeldingId,
        override val tidspunkt: Instant,
        override val melding: String,
        val navIdent: NavIdent,
        val mottaker: BehandlerRef,
    ) : Dialogmelding

    class FraBehandler(
        override val id: DialogmeldingId,
        override val tidspunkt: Instant,
        override val melding: String,
        val behandlerRef: BehandlerRef,
        val vedleggsreferanse: Vedleggsreferanse?,
    ) : Dialogmelding
}
