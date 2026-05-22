package no.nav.helse.sporhund.clients.accesstokenprovider

data class AccessTokenProviderConfig(
    val tokenEndpoint: String,
    val exchangeEndpoint: String,
)
