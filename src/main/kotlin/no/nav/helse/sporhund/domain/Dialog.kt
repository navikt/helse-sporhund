package no.nav.helse.sporhund.domain

import java.time.Duration
import java.time.Instant
import java.util.UUID

@JvmInline
value class ConversationRef(
    val value: UUID,
)

enum class Dialogstatus {
    ForespørselSendt,
    SvarMottatt,
    PurringSendt,
    DialogLukket,
}

enum class Fagområde {
    EnkeltståendeBehandlingsdager,
    Tilbakedatering,
    Yrkesskade,
    Bestridelse,
}

enum class Dialogtype {
    Journalnotat,
    MedisinskeOpplysninger,
    EkstraUttalelserFraLege,
    SpesialistErklæring,
    UtvidetSpesialistErklæring,
}

class Dialog private constructor(
    val conversationRef: ConversationRef,
    val identitetsnummer: Identitetsnummer,
    val status: Dialogstatus,
    val fagområde: Fagområde,
    val dialogtype: Dialogtype,
    meldinger: List<Dialogmelding>,
) {
    private val _meldinger = meldinger.toMutableList()
    val meldinger get() = _meldinger.toList()

    private val events = mutableListOf<NyDialogmeldingFraNavEvent>()

    fun events() = events.toList().also { events.clear() }

    fun nyesteMelding() = meldinger.last()

    private fun førsteMelding() = meldinger.first()

    fun frist(): Instant = nyesteMeldingFraNav().tidspunkt + Duration.ofDays(21)

    fun opprettetTidspunkt(): Instant = førsteMelding().tidspunkt

    fun antallVedleggTotalt() = meldinger.filterIsInstance<Dialogmelding.FraBehandler>().sumOf { it.antallVedlegg }

    fun opprinneligBehandler() = førsteMeldingFraNav().behandler to førsteMeldingFraNav().behandlerRef

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

    private fun nyesteMeldingFraNav(): Dialogmelding.FraNav = meldinger.filterIsInstance<Dialogmelding.FraNav>().last()

    private fun førsteMeldingFraNav(): Dialogmelding.FraNav = meldinger.filterIsInstance<Dialogmelding.FraNav>().first()

    companion object {
        fun ny(
            identitetsnummer: Identitetsnummer,
            melding: Dialogmelding.FraNav,
            fagområde: Fagområde,
            dialogtype: Dialogtype,
        ): Dialog =
            Dialog(
                conversationRef = ConversationRef(UUID.randomUUID()),
                identitetsnummer = identitetsnummer,
                meldinger = emptyList(),
                status = Dialogstatus.ForespørselSendt,
                fagområde = fagområde,
                dialogtype = dialogtype,
            ).also { it.nyMelding(melding) }

        fun fraLagring(
            conversationRef: ConversationRef,
            identitetsnummer: Identitetsnummer,
            meldinger: List<Dialogmelding>,
            status: Dialogstatus,
            fagområde: Fagområde,
            dialogtype: Dialogtype,
        ): Dialog = Dialog(conversationRef, identitetsnummer, status, fagområde, dialogtype, meldinger)
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
