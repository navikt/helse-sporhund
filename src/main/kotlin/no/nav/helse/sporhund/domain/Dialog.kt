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
    val søkernavn: Navn,
    status: Dialogstatus,
    val fagområde: Fagområde,
    val dialogtype: Dialogtype,
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
            Vi viser til tidligere forespørsel av $opprinneligDato angående din pasient.

            Vi kan ikke se å ha mottatt svar på vår forespørsel og ber om at denne besvares snarest.

            Hvis opplysningene er sendt oss i løpet av de siste dagene, kan du se bort fra denne meldingen.
            """.trimIndent()
        val purringMelding =
            Dialogmelding.FraNav.ny(
                saksbehandler = NavIdent("Speil"),
                behandler = nyesteFraNav.behandler,
                behandlerRef = nyesteFraNav.behandlerRef,
                melding = purringTekst,
            )
        _meldinger.add(purringMelding)
        status = Dialogstatus.PurringSendt
        events.add(
            NyDialogmeldingFraNavEvent(
                conversationRef = conversationRef,
                behandlerRef = purringMelding.behandlerRef,
                identitetsnummer = identitetsnummer,
                meldingId = purringMelding.id,
                tekst = purringMelding.melding,
                erPurring = true,
            ),
        )
    }

    fun gjenåpne() {
        status = if (nyesteMelding() is Dialogmelding.FraNav) Dialogstatus.ForespørselSendt else Dialogstatus.SvarMottatt
    }

    fun frist(): Instant = førsteMeldingFraNav().tidspunkt + Duration.ofDays(21)

    fun opprettetTidspunkt(): Instant = førsteMelding().tidspunkt

    fun antallVedleggTotalt() = meldinger.filterIsInstance<Dialogmelding.FraBehandler>().sumOf { it.antallVedlegg }

    fun harFåttSvar(): Boolean = meldinger.any { it is Dialogmelding.FraBehandler }

    fun opprinneligBehandler() = førsteMeldingFraNav().behandler to førsteMeldingFraNav().behandlerRef

    fun nyMelding(dialogmelding: Dialogmelding<*>) {
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

    private fun førsteMelding() = meldinger.first()

    fun nyesteMeldingFraNav(): Dialogmelding.FraNav = meldinger.filterIsInstance<Dialogmelding.FraNav>().last()

    private fun førsteMeldingFraNav(): Dialogmelding.FraNav = meldinger.filterIsInstance<Dialogmelding.FraNav>().first()

    companion object {
        fun ny(
            identitetsnummer: Identitetsnummer,
            søkernavn: Navn,
            melding: Dialogmelding.FraNav,
            fagområde: Fagområde,
            dialogtype: Dialogtype,
        ): Dialog =
            Dialog(
                conversationRef = ConversationRef(UUID.randomUUID()),
                identitetsnummer = identitetsnummer,
                søkernavn = søkernavn,
                meldinger = emptyList(),
                status = Dialogstatus.ForespørselSendt,
                fagområde = fagområde,
                dialogtype = dialogtype,
            ).also { it.nyMelding(melding) }

        fun fraLagring(
            conversationRef: ConversationRef,
            identitetsnummer: Identitetsnummer,
            søkernavn: Navn,
            meldinger: List<Dialogmelding<*>>,
            status: Dialogstatus,
            fagområde: Fagområde,
            dialogtype: Dialogtype,
        ): Dialog = Dialog(conversationRef, identitetsnummer, søkernavn, status, fagområde, dialogtype, meldinger)
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
                )

            fun fraLagring(
                id: DialogmeldingId<UUID>,
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
}
