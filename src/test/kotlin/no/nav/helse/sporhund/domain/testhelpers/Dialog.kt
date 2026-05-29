package no.nav.helse.sporhund.domain.testhelpers

import java.time.Instant
import java.util.*
import no.nav.helse.sporhund.domain.*

fun lagDialog(
    identitetsnummer: Identitetsnummer = lagIdentitetsnummer(),
    søkernavn: Navn = lagNavn(),
    status: Dialogstatus = Dialogstatus.ForespørselSendt,
    melding: Dialogmelding.FraNav = lagFraNavMelding(),
    dialogtype: Dialogtype = Dialogtype.MedisinskeOpplysninger,
    fagområde: Fagområde = Fagområde.EnkeltståendeBehandlingsdager
): Dialog =
    Dialog.fraLagring(
        conversationRef = ConversationRef(UUID.randomUUID()),
        identitetsnummer = identitetsnummer,
        søkernavn = søkernavn,
        meldinger = listOf(melding),
        status = status,
        dialogtype = dialogtype,
        fagområde = fagområde
    )

fun lagFraNavMelding(
    ident: NavIdent = lagNavIdent(),
    behandler: Behandler = lagBehandler(),
    behandlerRef: BehandlerRef = lagBehandlerRef(),
    melding: String = "En melding til behandler",
    opprettet: Instant = Instant.now()
): Dialogmelding.FraNav =
    Dialogmelding.FraNav.fraLagring(
        id = DialogmeldingId(UUID.randomUUID()),
        saksbehandler = ident,
        behandler = behandler,
        behandlerRef = behandlerRef,
        melding = melding,
        tidspunkt = opprettet
    )

fun lagFraBehandlerMelding(
    opprettet: Instant = Instant.now(),
    melding: String = "Svar fra behandler",
    behandler: Behandler = lagBehandler(),
    antallVedlegg: Int = 0
): Dialogmelding.FraBehandler =
    Dialogmelding.FraBehandler(
        id = DialogmeldingId(UUID.randomUUID().toString()),
        tidspunkt = opprettet,
        melding = melding,
        behandler = behandler,
        antallVedlegg = antallVedlegg
    )
