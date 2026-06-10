package no.nav.helse.sporhund.infrastructure.clients.dokarkiv

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.application.KnyttInnkommendeJournalpost
import no.nav.helse.sporhund.application.OpprettUtgåendeJournalpost
import no.nav.helse.sporhund.application.TransactionProvider
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
        runBlocking {
            while (readyToProduce.get()) {
                transactionProvider.transaction {
                    val opprettUtgåendeJournalposter = outbox.meldinger<OpprettUtgåendeJournalpost>()
                    val innkommendeJournalposter = outbox.meldinger<KnyttInnkommendeJournalpost>()
                    if (innkommendeJournalposter.isEmpty() && opprettUtgåendeJournalposter.isEmpty()) {
                        loggInfo("JournalførerJobb: Ingen journalposter å journalføre, sover $antallMillisekunderLoopenSover ms")
                        return@transaction
                    }
                    loggInfo("JournalførerJobb: Fant ${opprettUtgåendeJournalposter.size} utgående journalposter og ${innkommendeJournalposter.size} innkommende journalposter som skal journalføres")
                    opprettUtgåendeJournalposter.forEach { melding ->
                        dokarkivClient.journalførUtgåendeDialogmelding(melding)
                        outbox.meldingSendt(melding.id)
                    }
                    innkommendeJournalposter.forEach { melding ->
                        dokarkivClient.feilregistrerOgKnyttJournalpost(melding)
                        outbox.meldingSendt(melding.id)
                    }
                    loggInfo("JournalførerJobb: Journalført ${opprettUtgåendeJournalposter.size} utgående journalposter og ${innkommendeJournalposter.size} innkommende journalposter")
                }
                delay(antallMillisekunderLoopenSover.milliseconds)
            }
        }
    }
}
