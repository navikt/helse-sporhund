package no.nav.helse.sporhund.infrastructure.api.auth

import no.nav.helse.sporhund.domain.Saksbehandler

data class SaksbehandlerPrincipal(
    val saksbehandler: Saksbehandler,
    val accessToken: String,
)
