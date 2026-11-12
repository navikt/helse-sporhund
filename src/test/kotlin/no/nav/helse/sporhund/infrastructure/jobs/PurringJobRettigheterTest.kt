package no.nav.helse.sporhund.infrastructure.jobs

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagFraNavMelding
import no.nav.helse.sporhund.infrastructure.db.DataSourceBuilder
import no.nav.helse.sporhund.infrastructure.db.PgTransactionProvider
import no.nav.helse.sporhund.infrastructure.db.testhelpers.TestcontainersDatabase
import no.nav.helse.sporhund.sendPurringerForUtlopteFrister
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/** Kjører jobben som purring-rollen, med bare rettighetene migreringene gir den. */
class PurringJobRettigheterTest {
    @Test
    fun `purring-rollen har rettighetene jobben trenger`() {
        val dialog = lagDialog(melding = lagFraNavMelding(opprettet = Instant.now() - Duration.ofDays(22)))
        eier.transaction { dialogRepository.lagre(dialog) }

        sendPurringerForUtlopteFrister(purring)

        val lagret = eier.transaction { dialogRepository.finnDialog(dialog.conversationRef) }
        assertEquals(Dialogstatus.PurringSendt, lagret?.status)
    }

    private companion object {
        val db = TestcontainersDatabase("purring-rolle")
        private val eierBuilder = DataSourceBuilder(db.dbConfig)
        private val eierDataSource = eierBuilder.build()
        val eier = PgTransactionProvider(eierDataSource)
        val purring: PgTransactionProvider

        init {
            // Rollen må finnes før migreringene kjører, ellers hopper de over grantene
            sessionOf(eierDataSource).use {
                it.run(
                    queryOf(
                        """
                        DO $$
                        BEGIN
                            IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'purring') THEN
                                CREATE ROLE purring LOGIN PASSWORD 'purring';
                            END IF;
                        END
                        $$;
                        """.trimIndent(),
                    ).asExecute,
                )
            }
            eierBuilder.migrate()
            purring = PgTransactionProvider(DataSourceBuilder(db.dbConfig.copy(username = "purring", password = "purring")).build())
        }
    }
}
