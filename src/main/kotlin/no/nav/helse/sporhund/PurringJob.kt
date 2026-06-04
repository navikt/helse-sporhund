package no.nav.helse.sporhund

import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.infrastructure.db.DataSourceBuilder
import no.nav.helse.sporhund.infrastructure.db.DbConfig
import no.nav.helse.sporhund.infrastructure.db.PgTransactionProvider
import org.slf4j.LoggerFactory
import java.time.Instant

private val log = LoggerFactory.getLogger("PurringJobMain")

fun main() {
    val env = System.getenv()
    val dbConfig =
        DbConfig(
            jdbcUrl = env.getValue("DATABASE_JDBC_URL"),
            username = env.getValue("DATABASE_USERNAME"),
            password = env.getValue("DATABASE_PASSWORD"),
        )

    val dataSourceBuilder = DataSourceBuilder(dbConfig)
    val transactionProvider = PgTransactionProvider(dataSourceBuilder.build())

    log.info("Starter purring-jobb")
    sendPurringerForUtlopteFrister(transactionProvider)
    log.info("Purring-jobb ferdig")
}

internal fun sendPurringerForUtlopteFrister(transactionProvider: TransactionProvider) {
    transactionProvider.transaction {
        val now = Instant.now()
        dialogRepository
            .finnIkkeLukkedeDialoger()
            .filter { dialog ->
                dialog.status == Dialogstatus.ForespørselSendt &&
                    !dialog.harFåttSvar() &&
                    dialog.frist() <= now
            }.forEach { dialog ->
                log.info("Sender purring for dialog ${dialog.conversationRef.value}")
                dialog.sendPurring()
                val events = dialog.events()
                dialogRepository.lagre(dialog)
                events.forEach { outbox.nyMelding(OutboxMelding.nyDialogmeldingFraNav(it)) }
            }
    }
}
