package no.nav.helse.sporhund.tilgangskontroll

import no.nav.helse.sporhund.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import java.util.UUID

fun tilgangsgrupperTilBrukerroller(
    dialogmelding: List<UUID> = listOf(UUID.randomUUID()),
): TilgangsgrupperTilBrukerroller =
    TilgangsgrupperTilBrukerroller(
        dialogmelding = dialogmelding,
    )
