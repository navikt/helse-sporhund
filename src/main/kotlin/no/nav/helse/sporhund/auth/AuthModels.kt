package no.nav.helse.sporhund.auth

import no.nav.helse.sporhund.domain.Saksbehandler

data class SaksbehandlerPrincipal(
    val saksbehandler: Saksbehandler,
)

data class AzureAdConfig(
    val clientId: String,
    val issuerUrl: String,
    val jwkProviderUri: String,
)
