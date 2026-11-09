package no.nav.helse.sporhund.application.kommando

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.loggError
import no.nav.helse.sporhund.application.logg.loggInfo
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

class KommandoJobb(
    private val transactionProvider: TransactionProvider,
    private val håndterer: KommandoHåndterer,
) {
    /**
     * Utfører én kommando i én transaksjon. Kommandoene håndtereren returnerer, lagres i samme transaksjon.
     * Hvis håndteringen feiler, rulles transaksjonen tilbake og kommandoen blir liggende i køen.
     *
     * @return true hvis det fantes en kommando å utføre
     */
    fun utførNeste(): Boolean =
        transactionProvider.transaction {
            val kommando = kommandoKø.nesteKommando() ?: return@transaction false
            håndterer.håndter(kommando, this).forEach { kommandoKø.leggTil(it) }
            kommandoKø.utført(kommando.id)
            true
        }

    fun start(kjører: AtomicBoolean) {
        loggInfo("Starter kommandojobb")
        runBlocking {
            while (kjører.get()) {
                val fantKommando =
                    runCatching { utførNeste() }
                        .onFailure { loggError("Kommandojobb: Klarte ikke utføre kommando, prøver igjen i neste runde", it) }
                        .getOrDefault(false)
                if (!fantKommando) delay(5.seconds)
            }
        }
        loggInfo("Stopper kommandojobb")
    }
}
