package no.nav.helse.sporhund.application.kommando

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagSaksbehandler
import no.nav.helse.sporhund.infrastructure.db.PgTransactionProvider
import no.nav.helse.sporhund.infrastructure.db.testhelpers.TestDb
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KommandoJobbTest {
    private val transactionProvider = PgTransactionProvider(TestDb.dataSource)
    private var nå = Instant.parse("2026-01-01T12:00:00Z")
    private var antallKall = 0
    private val jobb =
        KommandoJobb(transactionProvider, nå = { nå }) { _, _ ->
            antallKall++
            error("Sprinter er nede")
        }

    private val logger = LoggerFactory.getLogger(KommandoJobb::class.java) as Logger
    private val loggmeldinger = ListAppender<ILoggingEvent>().also { it.start() }

    @BeforeTest
    fun setUp() {
        logger.addAppender(loggmeldinger)
        TestDb.truncate()
        val dialog = lagDialog()
        transactionProvider.transaction {
            kommandoKø.leggTil(LagDialogmeldingPdf.ny(dialog.nyesteMeldingFraNav(), dialog, lagSaksbehandler()))
        }
    }

    @AfterTest
    fun tearDown() {
        logger.detachAppender(loggmeldinger)
    }

    @Test
    fun `feil logges som advarsel de ni første forsøkene og som feil fra det tiende`() {
        repeat(11) {
            jobb.utførNeste()
            nå = nå.plusSeconds(3600)
        }

        val nivåer = loggmeldinger.list.map { it.level }
        assertEquals(List(9) { Level.WARN } + List(2) { Level.ERROR }, nivåer)
    }

    @Test
    fun `to jobber som kjører samtidig utfører aldri samme kommando`() {
        transactionProvider.transaction {
            repeat(20) {
                val dialog = lagDialog()
                kommandoKø.leggTil(LagDialogmeldingPdf.ny(dialog.nyesteMeldingFraNav(), dialog, lagSaksbehandler()))
            }
        }
        val utført = ConcurrentLinkedQueue<KommandoId>()
        val vellykketJobb =
            KommandoJobb(transactionProvider, nå = { nå }) { kommando, _ ->
                utført.add(kommando.id)
                Thread.sleep(10)
                emptyList()
            }

        val tråder = List(2) { thread { while (vellykketJobb.utførNeste()) Unit } }
        tråder.forEach { it.join() }

        assertEquals(21, utført.size)
        assertEquals(utført.size, utført.toSet().size)
    }

    @Test
    fun `en kommando som feiler prøves igjen med doblet ventetid opp til én time`() {
        val forventedeVentetiderISekunder = listOf(10L, 20, 40, 80, 160, 320, 640, 1280, 2560, 3600, 3600)

        assertTrue(jobb.utførNeste())
        forventedeVentetiderISekunder.forEachIndexed { indeks, ventetid ->
            nå = nå.plusSeconds(ventetid).minusMillis(1)
            assertFalse(jobb.utførNeste(), "Forsøk ${indeks + 2} skal ikke kjøres før det har gått $ventetid sekunder")
            nå = nå.plusMillis(1)
            assertTrue(jobb.utførNeste())
        }
        assertEquals(forventedeVentetiderISekunder.size + 1, antallKall)
    }
}
