package no.nav.helse.sporhund.application.kommando

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.loggError
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.application.logg.loggWarn
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class KommandoJobb(
    private val transactionProvider: TransactionProvider,
    private val ventetid: Duration = 5.seconds,
    private val nå: () -> Instant = Instant::now,
    private val håndterer: KommandoHåndterer,
) {
    /**
     * Utfører én kommando i én transaksjon. Kommandoene håndtereren returnerer, lagres i samme transaksjon.
     * Hvis håndteringen feiler, rulles transaksjonen tilbake, og kommandoen blir liggende i køen til neste forsøk.
     *
     * @return true hvis det fantes en kommando å utføre
     */
    fun utførNeste(): Boolean {
        val tidspunkt = nå()
        var hentet: KøetKommando? = null
        return try {
            transactionProvider.transaction {
                val køet = kommandoKø.nesteKommando(tidspunkt) ?: return@transaction false
                hentet = køet
                håndterer.håndter(køet.kommando, this).forEach { kommandoKø.leggTil(it) }
                kommandoKø.utført(køet.kommando.id)
                true
            }
        } catch (e: Exception) {
            val køet = hentet ?: throw e
            registrerFeil(køet, e, tidspunkt)
            true
        }
    }

    private fun registrerFeil(
        køet: KøetKommando,
        feil: Exception,
        tidspunkt: Instant,
    ) {
        val forsøk = køet.antallForsøk + 1
        val ventetidFørNesteForsøk = ventetidEtter(forsøk)
        transactionProvider.transaction {
            kommandoKø.feilet(
                id = køet.kommando.id,
                antallForsøk = forsøk,
                feilklasse = feil.javaClass.name,
                nesteForsøk = tidspunkt + ventetidFørNesteForsøk.toJavaDuration(),
            )
        }
        val melding =
            "Kommandojobb: ${køet.kommando.javaClass.simpleName} feilet på forsøk $forsøk " +
                "med ${feil.javaClass.simpleName}, prøver igjen om $ventetidFørNesteForsøk"
        val kommandoId = "kommandoId" to køet.kommando.id.value
        if (forsøk < FORSØK_FØR_ALARM) loggWarn(melding, feil, kommandoId) else loggError(melding, feil, kommandoId)
    }

    fun start(kjører: AtomicBoolean) {
        loggInfo("Starter kommandojobb")
        runBlocking {
            while (kjører.get()) {
                val fantKommando =
                    runCatching { utførNeste() }
                        .onFailure { loggError("Kommandojobb: Klarte ikke hente eller oppdatere kommando, prøver igjen i neste runde", it) }
                        .getOrDefault(false)
                if (!fantKommando) delay(ventetid)
            }
        }
        loggInfo("Stopper kommandojobb")
    }

    private companion object {
        const val FORSØK_FØR_ALARM = 10
        val FØRSTE_VENTETID = 10.seconds
        val LENGSTE_VENTETID = 1.hours

        fun ventetidEtter(forsøk: Int): Duration = (FØRSTE_VENTETID * (1 shl (forsøk - 1).coerceIn(0, 20))).coerceAtMost(LENGSTE_VENTETID)
    }
}
