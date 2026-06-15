package no.nav.helse.sporhund.infrastructure.api.auth

import no.nav.helse.sporhund.domain.Saksbehandler
import no.nav.helse.sporhund.domain.tilgangskontroll.Tilgang

data class SaksbehandlerPrincipal(
    val saksbehandler: Saksbehandler,
    val accessToken: String,
    val tilganger: Set<Tilgang>,
)
