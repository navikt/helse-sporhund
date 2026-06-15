package no.nav.helse.sporhund.tilgangskontroll

import no.nav.helse.sporhund.application.tilgangskontroll.TilgangsgrupperTilTilganger
import java.util.UUID

fun tilgangsgrupperTilTilganger(
    lesetilgang: List<UUID> = listOf(UUID.randomUUID()),
    skrivetilgang: List<UUID> = listOf(UUID.randomUUID()),
): TilgangsgrupperTilTilganger =
    TilgangsgrupperTilTilganger(
        lesetilgang = lesetilgang,
        skrivetilgang = skrivetilgang,
    )
