package no.nav.helse.sporhund.infrastructure.clients.accesstokenprovider

data class AccessTokenProviderConfig(
    val tokenEndpoint: String,
    val exchangeEndpoint: String
)
