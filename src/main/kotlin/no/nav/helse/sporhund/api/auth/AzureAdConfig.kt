package no.nav.helse.sporhund.api.auth

data class AzureAdConfig(
    val clientId: String,
    val issuerUrl: String,
    val jwkProviderUri: String,
)
