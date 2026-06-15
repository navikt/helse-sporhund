package no.nav.helse.sporhund.application.tilgangskontroll

import no.nav.helse.sporhund.domain.tilgangskontroll.Brukerrolle
import java.util.*

class TilgangsgrupperTilBrukerroller(
    val dialogmelding: List<UUID>,
) {
    fun finnBrukerrollerFraTilgangsgrupper(tilgangsgrupper: Collection<UUID>): Set<Brukerrolle> {
        val roller = mutableSetOf<Brukerrolle>()
        if (tilgangsgrupper.any { it in dialogmelding }) {
            roller.add(Brukerrolle.Dialogmelding)
        }
        return roller
    }
}
