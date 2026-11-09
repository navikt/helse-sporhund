package no.nav.helse.sporhund.infrastructure.db

import no.nav.helse.sporhund.application.kommando.JournalførDialogmelding
import no.nav.helse.sporhund.application.kommando.Kommando
import no.nav.helse.sporhund.application.kommando.KommandoId
import no.nav.helse.sporhund.application.kommando.KommandoJobb
import no.nav.helse.sporhund.application.kommando.LagDialogmeldingPdf
import no.nav.helse.sporhund.application.kommando.SendDialogmeldingPåKafka
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagSaksbehandler
import no.nav.helse.sporhund.infrastructure.db.testhelpers.TestDb
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PgKommandoKøTest {
    private val transactionProvider = PgTransactionProvider(TestDb.dataSource)
    private val dialog = lagDialog()
    private val lagPdf = LagDialogmeldingPdf.ny(dialog.nyesteMeldingFraNav(), dialog, lagSaksbehandler())

    @BeforeTest
    fun setUp() {
        TestDb.truncate()
    }

    @Test
    fun `kommando som feiler blir liggende og utføres i neste runde, og neste steg lagres`() {
        transactionProvider.transaction { kommandoKø.leggTil(lagPdf) }
        val mottatt = mutableListOf<Kommando>()
        var feilet = false
        val jobb =
            KommandoJobb(transactionProvider) { kommando, _ ->
                mottatt.add(kommando)
                if (!feilet) {
                    feilet = true
                    error("Sprinter er nede")
                }
                if (kommando is LagDialogmeldingPdf) {
                    listOf(
                        SendDialogmeldingPåKafka(KommandoId(UUID.randomUUID()), kommando.conversationRef, kommando.meldingId),
                        JournalførDialogmelding(KommandoId(UUID.randomUUID()), kommando.conversationRef, kommando.meldingId),
                    )
                } else {
                    emptyList()
                }
            }

        runCatching { jobb.utførNeste() }
        while (jobb.utførNeste()) {
            // Fortsetter til det ikke er flere kommandoer i køen
        }

        assertEquals(4, mottatt.size)
        val (førsteForsøk, andreForsøk) = mottatt.take(2).map { assertIs<LagDialogmeldingPdf>(it) }
        assertEquals(lagPdf, førsteForsøk)
        assertEquals(lagPdf, andreForsøk)

        val nesteSteg = mottatt.drop(2)
        assertEquals(1, nesteSteg.filterIsInstance<SendDialogmeldingPåKafka>().count { it.meldingId == lagPdf.meldingId })
        assertEquals(1, nesteSteg.filterIsInstance<JournalførDialogmelding>().count { it.meldingId == lagPdf.meldingId })
        assertTrue(nesteSteg.all { it.conversationRef == dialog.conversationRef })
    }

    @Test
    fun `en kommando som er låst av en annen transaksjon hoppes over`() {
        val annen = LagDialogmeldingPdf.ny(dialog.nyesteMeldingFraNav(), dialog, lagSaksbehandler())
        transactionProvider.transaction { kommandoKø.leggTil(lagPdf) }
        transactionProvider.transaction { kommandoKø.leggTil(annen) }

        transactionProvider.transaction {
            val låst = kommandoKø.nesteKommando()
            val iAnnenTransaksjon = transactionProvider.transaction { kommandoKø.nesteKommando() }
            assertEquals(lagPdf.id, låst?.id)
            assertEquals(annen.id, iAnnenTransaksjon?.id)
            assertNotEquals(låst?.id, iAnnenTransaksjon?.id)
        }
    }
}
