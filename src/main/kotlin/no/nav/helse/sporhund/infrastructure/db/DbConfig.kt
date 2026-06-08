package no.nav.helse.sporhund.infrastructure.db

data class DbConfig(
    val databaseName: String,
    val username: String,
    val password: String,
    val jdbcUrl: String,
)
