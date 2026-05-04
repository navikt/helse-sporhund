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

    private val events = mutableListOf<NyDialogmeldingFraNavEvent>()

    fun events() = events.toList().also { events.clear() }

    fun nyMelding(dialogmelding: Dialogmelding) {
        _meldinger.add(dialogmelding)
        if (dialogmelding is Dialogmelding.FraNav) {
            events.add(
                NyDialogmeldingFraNavEvent(
                    conversationRef = conversationRef,
                    behandlerRef = dialogmelding.mottaker,
                    identitetsnummer = identitetsnummer,
                    meldingId = dialogmelding.id,
                    type = "", // TODO: Finne ut hvilke typer vi skal ha og sende de med her
                    tekst = dialogmelding.melding,
                ),
            )
        }
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

    class FraNav private constructor(
        override val id: DialogmeldingId,
        override val tidspunkt: Instant,
        override val melding: String,
        val navIdent: NavIdent,
        val mottaker: BehandlerRef,
    ) : Dialogmelding {
        companion object {
            fun ny(
                navIdent: NavIdent,
                mottaker: BehandlerRef,
                melding: String,
            ): FraNav =
                FraNav(
                    id = DialogmeldingId(UUID.randomUUID()),
                    tidspunkt = Instant.now(),
                    melding = melding,
                    navIdent = navIdent,
                    mottaker = mottaker,
                )

            fun fraLagring(
                id: DialogmeldingId,
                tidspunkt: Instant,
                melding: String,
                navIdent: NavIdent,
                mottaker: BehandlerRef,
            ) = FraNav(
                id = id,
                tidspunkt = tidspunkt,
                melding = melding,
                navIdent = navIdent,
                mottaker = mottaker,
            )
        }
    }

    class FraBehandler(
        override val id: DialogmeldingId,
        override val tidspunkt: Instant,
        override val melding: String,
        val behandlerRef: BehandlerRef,
        val vedleggsreferanse: Vedleggsreferanse?,
    ) : Dialogmelding
}
