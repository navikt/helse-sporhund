package db.testhelpers

import db.DataSourceBuilder
import db.DbConfig
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
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

    private val testDbConfig =
        DbConfig(
            jdbcUrl = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password,
        )

    private val dataSourceBuilder = DataSourceBuilder(testDbConfig)
    val dataSource = dataSourceBuilder.build()

    init {
        dataSourceBuilder.migrate()
        truncate()
    }

    fun truncate() {
        sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val query =
                """
                CREATE OR REPLACE FUNCTION truncate_tables() RETURNS void AS $$
                DECLARE
                truncate_statement text;
                BEGIN
                    SELECT 'TRUNCATE ' || string_agg(format('%I.%I', schemaname, tablename), ',') || ' RESTART IDENTITY CASCADE'
                        INTO truncate_statement
                    FROM pg_tables
                    WHERE schemaname='public'
                    AND tablename not in ('flyway_schema_history');

                    EXECUTE truncate_statement;
                END;
                $$ LANGUAGE plpgsql;
                """.trimIndent()
            it.run(queryOf(query).asExecute)
            it.run(queryOf("SELECT truncate_tables()").asExecute)
        }
    }
}
