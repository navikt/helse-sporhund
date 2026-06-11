package no.nav.helse.sporhund.infrastructure.clients.dokarkiv

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.application.KnyttInnkommendeJournalpost
import no.nav.helse.sporhund.application.OpprettUtgåendeJournalpost
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.loggDebug
import no.nav.helse.sporhund.application.logg.loggError
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.application.meldinger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class JournalførerJobb(
    private val readyToProduce: AtomicBoolean,
    private val transactionProvider: TransactionProvider,
    private val dokarkivClient: DokarkivClient,
) {
    private val antallMillisekunderLoopenSover = 5000

    fun start() {
        loggInfo("Etablerer journalfører-jobb for dokarkiv")
        runBlocking {
            while (readyToProduce.get()) {
                runCatching {
                    transactionProvider.transaction {
                        val opprettUtgåendeJournalposter = outbox.meldinger<OpprettUtgåendeJournalpost>()
                        val innkommendeJournalposter = outbox.meldinger<KnyttInnkommendeJournalpost>()
                        if (innkommendeJournalposter.isEmpty() && opprettUtgåendeJournalposter.isEmpty()) {
                            loggDebug("Journalfører-jobb: Ingen journalposter å journalføre, sover $antallMillisekunderLoopenSover ms")
                            return@transaction
                        }
                        loggDebug("Journalfører-jobb: Fant ${opprettUtgåendeJournalposter.size} utgående journalposter og ${innkommendeJournalposter.size} innkommende journalposter som skal journalføres")
                        opprettUtgåendeJournalposter.forEach { melding ->
                            dokarkivClient.journalførUtgåendeDialogmelding(melding)
                            outbox.meldingSendt(melding.id)
                        }
                        innkommendeJournalposter.forEach { melding ->
                            dokarkivClient.feilregistrerOgKnyttJournalpost(melding)
                            outbox.meldingSendt(melding.id)
                        }
                        loggDebug("Journalfører-jobb: Journalført ${opprettUtgåendeJournalposter.size} utgående journalposter og ${innkommendeJournalposter.size} innkommende journalposter")
                    }
                }.onFailure {
                    loggError(
                        "Journalfører-jobb: Feil ved journalføring, meldinger blir ikke journalført. Meldingene er ikke kvittert ut i outbox-en",
                        it,
                        "error" to it.message.toString(),
                    )
                }
                delay(antallMillisekunderLoopenSover.milliseconds)
            }
        }
        loggInfo("Lukker journalfører-jobb for dokarkiv")
    }
}
