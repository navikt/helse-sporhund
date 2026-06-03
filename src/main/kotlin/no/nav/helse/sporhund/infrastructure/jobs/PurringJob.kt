package no.nav.helse.sporhund.infrastructure.jobs

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.Dialogstatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes

class PurringJob(
    private val running: AtomicBoolean,
    private val transactionProvider: TransactionProvider,
    private val pollInterval: kotlin.time.Duration = 15.minutes,
    private val clock: Clock = Clock.system(ZoneId.of("Europe/Oslo")),
) {
    fun start() {
        runBlocking {
            this@PurringJob.loggInfo("Starter purring-jobb")
            while (running.get()) {
                if (erMidnatt()) {
                    sendPurringerForUtlopteFrister()
                }
                delay(pollInterval)
            }
        }
    }

    internal fun erMidnatt(): Boolean = ZonedDateTime.now(clock).hour == 0

    internal fun sendPurringerForUtlopteFrister() {
        transactionProvider.transaction {
            val now = Instant.now()
            dialogRepository
                .finnIkkeLukkedeDialoger()
                .filter { dialog ->
                    dialog.status == Dialogstatus.ForespørselSendt &&
                        dialog.nyesteMelding() is Dialogmelding.FraNav &&
                        dialog.frist() <= now
                }.forEach { dialog ->
                    this@PurringJob.loggInfo("Sender purring for dialog ${dialog.conversationRef.value}")
                    dialog.sendPurring()
                    val events = dialog.events()
                    dialogRepository.lagre(dialog)
                    events.forEach { outbox.nyMelding(OutboxMelding.nyDialogmeldingFraNav(it)) }
                }
        }
    }
}
