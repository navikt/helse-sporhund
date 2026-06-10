package no.nav.helse.sporhund.infrastructure.clients.dokarkiv

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.application.KnyttInnkommendeJournalpost
import no.nav.helse.sporhund.application.OpprettUtgåendeJournalpost
import no.nav.helse.sporhund.application.PdfProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.meldinger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class JournalførerJobb(
    private val readyToProduce: AtomicBoolean,
    private val transactionProvider: TransactionProvider,
    dokarkivConfig: DokarkivConfig,
    pdfProvider: PdfProvider,
    accessTokenProvider: AccessTokenProvider,
) {
    private val dokarkivClient = DokarkivClient(dokarkivConfig, pdfProvider, accessTokenProvider)

    fun start() {
        runBlocking {
            while (readyToProduce.get()) {
                transactionProvider.transaction {
                    outbox.meldinger<OpprettUtgåendeJournalpost>().forEach { melding ->
                        dokarkivClient.journalførUtgåendeDialogmelding(melding)
                        outbox.meldingSendt(melding.id)
                    }
                    outbox.meldinger<KnyttInnkommendeJournalpost>().forEach { melding ->
                        dokarkivClient.feilregistrerOgKnyttJournalpost(melding)
                        outbox.meldingSendt(melding.id)
                    }
                }
                delay(500.milliseconds)
            }
        }
    }
}
