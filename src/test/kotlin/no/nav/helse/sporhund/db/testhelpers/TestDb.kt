package no.nav.helse.sporhund.db.testhelpers

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.sporhund.db.DataSourceBuilder
import org.intellij.lang.annotations.Language

object TestDb {
    val db = TestcontainersDatabase("test-db")

    private val dataSourceBuilder = DataSourceBuilder(db.dbConfig)
    val dataSource = dataSourceBuilder.build()

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

    init {
        dataSourceBuilder.migrate()
        truncate()
    }
}
