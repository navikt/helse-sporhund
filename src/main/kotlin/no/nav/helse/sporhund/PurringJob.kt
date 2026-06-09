package no.nav.helse.sporhund

import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.teamLogs
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.infrastructure.db.DataSourceBuilder
import no.nav.helse.sporhund.infrastructure.db.DbConfig
import no.nav.helse.sporhund.infrastructure.db.PgTransactionProvider
import java.time.Instant

fun main() {
    val env = System.getenv()
    val dbConfig =
        DbConfig(
            jdbcUrl = env.getValue("DATABASE_JDBC_URL"),
            username = env.getValue("DATABASE_PURRING_USERNAME"),
            password = env.getValue("DATABASE_PURRING_PASSWORD"),
        )

    val dataSourceBuilder = DataSourceBuilder(dbConfig)
    dataSourceBuilder.migrate()
    val transactionProvider = PgTransactionProvider(dataSourceBuilder.build())

    teamLogs.info("Starter purring-jobb")
    println("Starter purring-jobb")
    sendPurringerForUtlopteFrister(transactionProvider)
    teamLogs.info("Purring-jobb ferdig")
    println("Purring-jobb ferdig")
}

internal fun sendPurringerForUtlopteFrister(transactionProvider: TransactionProvider) {
    transactionProvider.transaction {
        val now = Instant.now()
        dialogRepository
            .finnIkkeLukkedeDialoger()
            .also { println("Fant ${it.size} ikke-lukkede dialoger") }
            .filter { dialog ->
                dialog.status == Dialogstatus.ForespørselSendt &&
                    !dialog.harFåttSvar() &&
                    dialog.frist() <= now
            }.also { println("Sender purring for ${it.size} dialoger") }
            .forEach { dialog ->
                teamLogs
                    .info("Sender purring for dialog ${dialog.conversationRef.value}")
                    .also { println("Sender purring for dialog ${dialog.conversationRef.value}") }
                dialog.sendPurring()
                val events = dialog.events()
                dialogRepository.lagre(dialog)
                events.forEach {
                    outbox.nyMelding(
                        OutboxMelding.nyDialogmeldingFraNav(it).also {
                            println("nymelding i outbox")
                        },
                    )
                }
            }
    }
}
