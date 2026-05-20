package no.nav.helse.sporhund.domain.testhelpers

import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.domain.Identitetsnummer
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

fun lagFraNavMelding(): Dialogmelding.FraNav =
    Dialogmelding.FraNav.ny(
        saksbehandler = lagNavIdent(),
        behandler = lagBehandler(),
        behandlerRef = lagBehandlerRef(),
        melding = "En melding til behandler",
    )
