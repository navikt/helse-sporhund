package db.testhelpers

import db.DbConfig
import org.testcontainers.postgresql.PostgreSQLContainer

class TestcontainersDatabase(
    moduleLabel: String,
) {
    private val postgres =
        PostgreSQLContainer("postgres:17")
            .withReuse(true)
            .withLabel("app", "spesialist")
            .withLabel("module", moduleLabel)
            .withLabel("code-location", javaClass.canonicalName)
            .apply {
                start()
                println("Database startet opp.\nUrl: $jdbcUrl\nBrukernavn: $username\nPassord: $password")
            }

    val dbConfig =
        DbConfig(
            jdbcUrl = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password,
        )

    fun stop() = postgres.stop()
}
