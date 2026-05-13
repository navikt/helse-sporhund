package no.nav.helse.sporhund.api.auth

import no.nav.helse.sporhund.domain.Saksbehandler

data class SaksbehandlerPrincipal(
    val saksbehandler: Saksbehandler,
)
