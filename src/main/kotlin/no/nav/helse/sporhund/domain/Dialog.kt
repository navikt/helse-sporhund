package no.nav.helse.sporhund.domain

import java.time.Instant
import java.util.UUID

@JvmInline
value class ConversationRef(
    val value: UUID,
)

class Dialog private constructor(
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
                    behandlerRef = dialogmelding.behandlerRef,
                    identitetsnummer = identitetsnummer,
                    meldingId = dialogmelding.id,
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
                conversationRef = ConversationRef(UUID.randomUUID()),
                identitetsnummer = identitetsnummer,
                meldinger = emptyList(),
            ).also { it.nyMelding(melding) }

        fun fraLagring(
            conversationRef: ConversationRef,
            identitetsnummer: Identitetsnummer,
            meldinger: List<Dialogmelding>,
        ): Dialog = Dialog(conversationRef, identitetsnummer, meldinger)
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
        val saksbehandler: NavIdent,
        val behandlerRef: BehandlerRef,
        val behandler: Behandler,
    ) : Dialogmelding {
        companion object {
            fun ny(
                saksbehandler: NavIdent,
                behandler: Behandler,
                behandlerRef: BehandlerRef,
                melding: String,
            ): FraNav =
                FraNav(
                    id = DialogmeldingId(UUID.randomUUID()),
                    tidspunkt = Instant.now(),
                    melding = melding,
                    saksbehandler = saksbehandler,
                    behandler = behandler,
                    behandlerRef = behandlerRef,
                )

            fun fraLagring(
                id: DialogmeldingId,
                tidspunkt: Instant,
                melding: String,
                saksbehandler: NavIdent,
                behandler: Behandler,
                behandlerRef: BehandlerRef,
            ) = FraNav(
                id = id,
                tidspunkt = tidspunkt,
                melding = melding,
                saksbehandler = saksbehandler,
                behandler = behandler,
                behandlerRef = behandlerRef,
            )
        }
    }

    class FraBehandler(
        override val id: DialogmeldingId,
        override val tidspunkt: Instant,
        override val melding: String,
        val behandler: Behandler,
        val antallVedlegg: Int,
    ) : Dialogmelding
}
