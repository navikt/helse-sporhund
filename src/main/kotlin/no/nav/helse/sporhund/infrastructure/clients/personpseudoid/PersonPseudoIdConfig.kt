package no.nav.helse.sporhund.infrastructure.clients.personpseudoid

data class PersonPseudoIdConfig(
    val valkeyBrukernavn: String,
    val valkeyPassord: String,
    val valkeyConnectionString: String,
)
