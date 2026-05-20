package no.nav.helse.sporhund.domain.testhelpers

import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.BehandlerRef
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.domain.Identitetsnummer
import no.nav.helse.sporhund.domain.NavIdent
import java.time.Instant
import java.util.UUID

fun lagDialog(
    identitetsnummer: Identitetsnummer = lagIdentitetsnummer(),
    status: Dialogstatus = Dialogstatus.ForespørselSendt,
    melding: Dialogmelding.FraNav = lagFraNavMelding(),
): Dialog =
    Dialog.fraLagring(
        conversationRef = ConversationRef(UUID.randomUUID()),
        identitetsnummer = identitetsnummer,
        meldinger = listOf(melding),
        status = status,
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
    opprettet: Instant = Instant.now(),
    melding: String = "Svar fra behandler",
    behandler: Behandler = lagBehandler(),
    antallVedlegg: Int = 0,
): Dialogmelding.FraBehandler =
    Dialogmelding.FraBehandler(
        id = DialogmeldingId(UUID.randomUUID()),
        tidspunkt = opprettet,
        melding = melding,
        behandler = behandler,
        antallVedlegg = antallVedlegg,
    )
