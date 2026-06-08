package no.nav.helse.sporhund.infrastructure.db.testhelpers

import no.nav.helse.sporhund.infrastructure.db.DbConfig
import org.testcontainers.postgresql.PostgreSQLContainer

class TestcontainersDatabase(
    moduleLabel: String,
) {
    private val postgres =
        PostgreSQLContainer("postgres:17")
            .withReuse(true)
            .withLabel("app", "sporhund")
            .withLabel("module", moduleLabel)
            .withLabel("code-location", javaClass.canonicalName)
            .apply {
                start()
                println("Database startet opp.\nUrl: $jdbcUrl\nBrukernavn: $username\nPassord: $password")
            }

    val dbConfig =
        DbConfig(
            databaseName = postgres.databaseName,
            username = postgres.username,
            password = postgres.password,
            jdbcUrl = postgres.jdbcUrl,
        )

    fun stop() = postgres.stop()
}
