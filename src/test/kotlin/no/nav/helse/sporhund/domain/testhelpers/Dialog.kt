package no.nav.helse.sporhund.domain.testhelpers

import no.nav.helse.sporhund.domain.*
import java.time.Instant
import java.util.*

fun lagDialog(
    identitetsnummer: Identitetsnummer = lagIdentitetsnummer(),
    søkernavn: Navn = lagNavn(),
    status: Dialogstatus = Dialogstatus.ForespørselSendt,
    melding: Dialogmelding.FraNav = lagFraNavMelding(),
    fagområde: Fagområde = Fagområde.EnkeltståendeBehandlingsdager,
): Dialog =
    Dialog.fraLagring(
        conversationRef = ConversationRef(UUID.randomUUID()),
        identitetsnummer = identitetsnummer,
        søkernavn = søkernavn,
        meldinger = listOf(melding),
        status = status,
        fagområde = fagområde,
    )

fun lagFraNavMelding(
    ident: NavIdent = lagNavIdent(),
    behandler: Behandler = lagBehandler(),
    behandlerRef: BehandlerRef = lagBehandlerRef(),
    melding: String = "En melding til behandler",
    opprettet: Instant = Instant.now(),
): Dialogmelding.FraNav =
    Dialogmelding.FraNav.fraLagring(
        id = DialogmeldingId(UUID.randomUUID()),
        saksbehandler = ident,
        behandler = behandler,
        behandlerRef = behandlerRef,
        melding = melding,
        tidspunkt = opprettet,
    )

fun lagFraBehandlerMelding(
    msgId: UUID = UUID.randomUUID(),
    opprettet: Instant = Instant.now(),
    melding: String = "Svar fra behandler",
    behandler: Behandler = lagBehandler(),
    antallVedlegg: Int = 0,
): Dialogmelding.FraBehandler =
    Dialogmelding.FraBehandler(
        id = DialogmeldingId(msgId.toString()),
        tidspunkt = opprettet,
        melding = melding,
        behandler = behandler,
        antallVedlegg = antallVedlegg,
    )

fun lagFraSystemMelding(
    behandler: Behandler = lagBehandler(),
    behandlerRef: BehandlerRef = lagBehandlerRef(),
    melding: String = "En melding til behandler fra systemet",
    opprettet: Instant = Instant.now(),
): Dialogmelding.FraSystem =
    Dialogmelding.FraSystem.fraLagring(
        id = DialogmeldingId(UUID.randomUUID()),
        behandler = behandler,
        behandlerRef = behandlerRef,
        melding = melding,
        tidspunkt = opprettet,
    )
