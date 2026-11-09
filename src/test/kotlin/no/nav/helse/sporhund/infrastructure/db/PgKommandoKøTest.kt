package no.nav.helse.sporhund.infrastructure.db

import no.nav.helse.sporhund.application.kommando.JournalførDialogmelding
import no.nav.helse.sporhund.application.kommando.Kommando
import no.nav.helse.sporhund.application.kommando.KommandoId
import no.nav.helse.sporhund.application.kommando.KommandoJobb
import no.nav.helse.sporhund.application.kommando.KøetKommando
import no.nav.helse.sporhund.application.kommando.LagDialogmeldingPdf
import no.nav.helse.sporhund.application.kommando.SendDialogmeldingPåKafka
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagSaksbehandler
import no.nav.helse.sporhund.infrastructure.db.testhelpers.TestDb
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PgKommandoKøTest {
    private val transactionProvider = PgTransactionProvider(TestDb.dataSource)
    private val nå = Instant.parse("2026-01-01T12:00:00Z")
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
        var klokke = nå
        val jobb =
            KommandoJobb(transactionProvider, nå = { klokke }) { kommando, _ ->
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

        jobb.utførNeste()
        klokke = klokke.plusSeconds(10)
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
    fun `en kommando venter på eldre kommandoer av samme type i samme dialog`() {
        val annenDialog = lagDialog()
        val nyereISammeDialog = LagDialogmeldingPdf.ny(dialog.nyesteMeldingFraNav(), dialog, lagSaksbehandler())
        val annenType = SendDialogmeldingPåKafka(KommandoId(UUID.randomUUID()), dialog.conversationRef, lagPdf.meldingId)
        val iAnnenDialog = LagDialogmeldingPdf.ny(annenDialog.nyesteMeldingFraNav(), annenDialog, lagSaksbehandler())
        transactionProvider.transaction {
            kommandoKø.leggTil(lagPdf)
            kommandoKø.leggTil(nyereISammeDialog)
            kommandoKø.leggTil(annenType)
            kommandoKø.leggTil(iAnnenDialog)
        }

        transactionProvider.transaction {
            assertEquals(lagPdf.id, kommandoKø.nesteKommando(nå)?.kommando?.id)
            transactionProvider.transaction {
                assertEquals(annenType.id, kommandoKø.nesteKommando(nå)?.kommando?.id)
                transactionProvider.transaction {
                    assertEquals(iAnnenDialog.id, kommandoKø.nesteKommando(nå)?.kommando?.id)
                    transactionProvider.transaction {
                        assertNull(kommandoKø.nesteKommando(nå), "Nyere kommando av samme type i samme dialog skal vente")
                    }
                }
            }
        }
    }

    @Test
    fun `en kommando som har feilet hentes ikke før neste forsøk, og blokkerer nyere i samme dialog så lenge`() {
        val nyereISammeDialog = LagDialogmeldingPdf.ny(dialog.nyesteMeldingFraNav(), dialog, lagSaksbehandler())
        transactionProvider.transaction {
            kommandoKø.leggTil(lagPdf)
            kommandoKø.leggTil(nyereISammeDialog)
        }
        val nesteForsøk = nå.plusSeconds(10)
        transactionProvider.transaction {
            kommandoKø.feilet(lagPdf.id, antallForsøk = 1, feilklasse = "IllegalStateException", nesteForsøk = nesteForsøk)
        }

        assertNull(transactionProvider.transaction { kommandoKø.nesteKommando(nesteForsøk.minusMillis(1)) })
        val hentet = transactionProvider.transaction { kommandoKø.nesteKommando(nesteForsøk) }
        assertEquals(KøetKommando(lagPdf, antallForsøk = 1), hentet)
    }

    @Test
    fun `en kommando som er låst av en annen transaksjon hoppes over`() {
        val annenDialog = lagDialog()
        val annen = LagDialogmeldingPdf.ny(annenDialog.nyesteMeldingFraNav(), annenDialog, lagSaksbehandler())
        transactionProvider.transaction { kommandoKø.leggTil(lagPdf) }
        transactionProvider.transaction { kommandoKø.leggTil(annen) }

        transactionProvider.transaction {
            val låst = kommandoKø.nesteKommando(nå)?.kommando
            val iAnnenTransaksjon = transactionProvider.transaction { kommandoKø.nesteKommando(nå)?.kommando }
            assertEquals(lagPdf.id, låst?.id)
            assertEquals(annen.id, iAnnenTransaksjon?.id)
            assertNotEquals(låst?.id, iAnnenTransaksjon?.id)
        }
    }
}
