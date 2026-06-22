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
    val cloudSqlInstance =
        "${env.getValue("GCP_TEAM_PROJECT_ID")}:${env.getValue("DATABASE_REGION")}:${env.getValue("DATABASE_INSTANCE")}"
    val dbConfig =
        DbConfig(
            jdbcUrl = "jdbc:postgresql:///${env.getValue("DATABASE_PURRING_DATABASE")}?cloudSqlInstance=$cloudSqlInstance&socketFactory=com.google.cloud.sql.postgres.SocketFactory",
            username = env.getValue("DATABASE_PURRING_USERNAME"),
            password = env.getValue("DATABASE_PURRING_PASSWORD"),
        )

    val dataSourceBuilder = DataSourceBuilder(dbConfig)
    val transactionProvider = PgTransactionProvider(dataSourceBuilder.build())

    teamLogs.info("Starter purring-jobb")
    sendPurringerForUtlopteFrister(transactionProvider)
    teamLogs.info("Purring-jobb ferdig")
}

internal fun sendPurringerForUtlopteFrister(transactionProvider: TransactionProvider) {
    transactionProvider.transaction {
        val now = Instant.now()
        dialogRepository
            .finnÅpneDialoger()
            .filter { dialog ->
                dialog.status == Dialogstatus.ForespørselSendt &&
                    !dialog.harFåttSvar() &&
                    dialog.frist() <= now
            }.forEach { dialog ->
                teamLogs.info("Sender purring for dialog ${dialog.conversationRef.value}")
                dialog.sendPurring()
                val events = dialog.events()
                dialogRepository.lagre(dialog)
                events.forEach {
                    outbox.nyMelding(
                        OutboxMelding.nyDialogmeldingFraNav(it),
                    )
                }
            }
    }
}
