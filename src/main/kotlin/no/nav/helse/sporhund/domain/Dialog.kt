package no.nav.helse.sporhund.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@JvmInline
value class ConversationRef(
    val value: UUID,
)

enum class Dialogstatus {
    ForespørselSendt,
    SvarMottatt,
    PurringSendt,
    DialogLukket,
    Avvist,
}

enum class Fagområde {
    EnkeltståendeBehandlingsdager,
    Tilbakedatering,
    Yrkesskade,
    Bestridelse,
}

class Dialog private constructor(
    val conversationRef: ConversationRef,
    val identitetsnummer: Identitetsnummer,
    val søker: Søker,
    status: Dialogstatus,
    val fagområde: Fagområde,
    meldinger: List<Dialogmelding<*>>,
) {
    var status: Dialogstatus = status
        private set

    private val _meldinger = meldinger.toMutableList()
    val meldinger get() = _meldinger.toList()

    private val events = mutableListOf<NyDialogmeldingFraNavEvent>()

    fun events() = events.toList().also { events.clear() }

    fun nyesteMelding() = meldinger.last()

    fun ferdigstill() {
        status = Dialogstatus.DialogLukket
    }

    fun sendPurring() {
        val nyesteFraNav = nyesteMeldingFraNav()
        val opprinneligDato =
            nyesteFraNav.tidspunkt
                .atZone(ZoneId.of("Europe/Oslo"))
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val purringTekst =
            """
            Hei,    
                
            Vi viser til tidligere forespørsel av $opprinneligDato angående din pasient.

            Vi kan ikke se å ha mottatt svar på vår forespørsel og ber om at denne besvares snarest.

            Hvis opplysningene er sendt oss i løpet av de siste dagene, kan du se bort fra denne meldingen.
            
            Med vennlig hilsen
            Nav arbeid og ytelser sykepenger
            """.trimIndent()
        val purringMelding =
            Dialogmelding.FraSystem.ny(
                behandler = nyesteFraNav.behandler,
                behandlerRef = nyesteFraNav.behandlerRef,
                melding = purringTekst,
            )
        nyMelding(purringMelding)
    }

    fun gjenåpne() {
        status =
            when (nyesteMelding()) {
                is Dialogmelding.FraBehandler -> Dialogstatus.SvarMottatt
                is Dialogmelding.FraNav -> Dialogstatus.ForespørselSendt
                is Dialogmelding.FraSystem -> Dialogstatus.PurringSendt
            }
    }

    fun mottaKvittering(
        meldingId: DialogmeldingId<UUID>,
        avvist: Boolean,
    ) {
        val melding =
            _meldinger.firstOrNull { it.id == meldingId }
                ?: return
        when (melding) {
            is Dialogmelding.FraNav -> melding.kvitteringMottatt = true
            is Dialogmelding.FraSystem -> melding.kvitteringMottatt = true
            else -> return
        }
        if (avvist && (status == Dialogstatus.ForespørselSendt || status == Dialogstatus.PurringSendt)) {
            status = Dialogstatus.Avvist
        }
    }

    fun frist(): Instant = førsteMeldingFraNav().tidspunkt + Duration.ofDays(21)

    fun antallVedleggTotalt() = meldinger.filterIsInstance<Dialogmelding.FraBehandler>().sumOf { it.antallVedlegg }

    fun harFåttSvar(): Boolean = meldinger.any { it is Dialogmelding.FraBehandler }

    fun opprinneligBehandler() = førsteMeldingFraNav().behandler to førsteMeldingFraNav().behandlerRef

    fun nyMelding(dialogmelding: Dialogmelding<*>) {
        _meldinger.add(dialogmelding)
        when (dialogmelding) {
            is Dialogmelding.FraBehandler -> status = Dialogstatus.SvarMottatt
            is Dialogmelding.FraNav -> {
                status = Dialogstatus.ForespørselSendt
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

            is Dialogmelding.FraSystem -> {
                status = Dialogstatus.PurringSendt
                events.add(
                    NyDialogmeldingFraNavEvent(
                        conversationRef = conversationRef,
                        behandlerRef = dialogmelding.behandlerRef,
                        identitetsnummer = identitetsnummer,
                        meldingId = dialogmelding.id,
                        tekst = dialogmelding.melding,
                        erPurring = true,
                    ),
                )
            }
        }
    }

    fun nyesteMeldingFraNav(): Dialogmelding.FraNav = meldinger.filterIsInstance<Dialogmelding.FraNav>().last()

    private fun førsteMeldingFraNav(): Dialogmelding.FraNav = meldinger.filterIsInstance<Dialogmelding.FraNav>().first()

    fun meldingFinnes(dialogmeldingId: DialogmeldingId<*>): Boolean = meldinger.any { it.id == dialogmeldingId }

    companion object {
        fun ny(
            identitetsnummer: Identitetsnummer,
            søker: Søker,
            melding: Dialogmelding.FraNav,
            fagområde: Fagområde,
        ): Dialog =
            Dialog(
                conversationRef = ConversationRef(UUID.randomUUID()),
                identitetsnummer = identitetsnummer,
                søker = søker,
                meldinger = emptyList(),
                status = Dialogstatus.ForespørselSendt,
                fagområde = fagområde,
            ).also { it.nyMelding(melding) }

        fun fraLagring(
            conversationRef: ConversationRef,
            identitetsnummer: Identitetsnummer,
            søker: Søker,
            meldinger: List<Dialogmelding<*>>,
            status: Dialogstatus,
            fagområde: Fagområde,
        ): Dialog = Dialog(conversationRef, identitetsnummer, søker, status, fagområde, meldinger)
    }
}

@JvmInline
value class DialogmeldingId<ID_TYPE>(
    val value: ID_TYPE,
)

sealed interface Dialogmelding<ID_TYPE> {
    val id: DialogmeldingId<ID_TYPE>
    val tidspunkt: Instant
    val melding: String
    val behandler: Behandler

    class FraNav private constructor(
        override val id: DialogmeldingId<UUID>,
        override val tidspunkt: Instant,
        override val melding: String,
        override val behandler: Behandler,
        val saksbehandler: NavIdent,
        val behandlerRef: BehandlerRef,
        var kvitteringMottatt: Boolean = false,
    ) : Dialogmelding<UUID> {
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
                    kvitteringMottatt = false,
                )

            fun fraLagring(
                id: DialogmeldingId<UUID>,
                tidspunkt: Instant,
                melding: String,
                saksbehandler: NavIdent,
                behandler: Behandler,
                behandlerRef: BehandlerRef,
                kvitteringMottatt: Boolean = false,
            ) = FraNav(
                id = id,
                tidspunkt = tidspunkt,
                melding = melding,
                saksbehandler = saksbehandler,
                behandler = behandler,
                behandlerRef = behandlerRef,
                kvitteringMottatt = kvitteringMottatt,
            )
        }
    }

    class FraBehandler(
        override val id: DialogmeldingId<String>,
        override val tidspunkt: Instant,
        override val melding: String,
        override val behandler: Behandler,
        val antallVedlegg: Int,
    ) : Dialogmelding<String> {
        companion object {
            fun ny(
                meldingId: String,
                behandler: Behandler,
                tidspunkt: Instant,
                antallVedlegg: Int,
                melding: String,
            ): FraBehandler =
                FraBehandler(
                    id = DialogmeldingId(meldingId),
                    tidspunkt = tidspunkt,
                    melding = melding,
                    behandler = behandler,
                    antallVedlegg = antallVedlegg,
                )

            fun fraLagring(
                id: DialogmeldingId<String>,
                tidspunkt: Instant,
                melding: String,
                behandler: Behandler,
                antallVedlegg: Int,
            ) = FraBehandler(
                id = id,
                tidspunkt = tidspunkt,
                melding = melding,
                behandler = behandler,
                antallVedlegg = antallVedlegg,
            )
        }
    }

    class FraSystem(
        override val id: DialogmeldingId<UUID>,
        override val tidspunkt: Instant,
        override val melding: String,
        override val behandler: Behandler,
        val behandlerRef: BehandlerRef,
        var kvitteringMottatt: Boolean = false,
    ) : Dialogmelding<UUID> {
        companion object {
            fun ny(
                behandler: Behandler,
                melding: String,
                behandlerRef: BehandlerRef,
            ): FraSystem =
                FraSystem(
                    id = DialogmeldingId(UUID.randomUUID()),
                    tidspunkt = Instant.now(),
                    melding = melding,
                    behandler = behandler,
                    behandlerRef = behandlerRef,
                    kvitteringMottatt = false,
                )

            fun fraLagring(
                id: DialogmeldingId<UUID>,
                tidspunkt: Instant,
                melding: String,
                behandler: Behandler,
                behandlerRef: BehandlerRef,
                kvitteringMottatt: Boolean = false,
            ) = FraSystem(
                id = id,
                tidspunkt = tidspunkt,
                melding = melding,
                behandler = behandler,
                behandlerRef = behandlerRef,
                kvitteringMottatt = kvitteringMottatt,
            )
        }
    }
}
