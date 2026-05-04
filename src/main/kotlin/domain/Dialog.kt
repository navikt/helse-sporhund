package domain

import java.time.Instant
import java.util.UUID

@JvmInline
value class DialogId(
    val value: UUID,
)

class Dialog private constructor(
    val id: DialogId,
    val conversationRef: UUID,
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
            Dialog(DialogId(UUID.randomUUID()), UUID.randomUUID(), identitetsnummer, emptyList())
                .also { it.nyMelding(melding) }

        fun fraLagring(
            id: DialogId,
            conversationRef: UUID,
            identitetsnummer: Identitetsnummer,
            meldinger: List<Dialogmelding>,
        ): Dialog = Dialog(id, conversationRef, identitetsnummer, meldinger)
    }
}

interface Dialogmelding {
    val tidspunkt: Instant
    val melding: String

    class FraNav(
        override val tidspunkt: Instant,
        override val melding: String,
        val navIdent: NavIdent,
    ) : Dialogmelding

    class FraBehandler(
        override val tidspunkt: Instant,
        override val melding: String,
        val behandlerId: BehandlerId,
        val vedleggsreferanse: Vedleggsreferanse?,
    ) : Dialogmelding
}
