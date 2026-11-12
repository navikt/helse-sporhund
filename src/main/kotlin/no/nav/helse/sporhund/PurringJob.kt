package no.nav.helse.sporhund

import ch.qos.logback.classic.LoggerContext
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.kommando.LagDialogmeldingPdf
import no.nav.helse.sporhund.application.logg.loggError
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.infrastructure.db.DataSourceBuilder
import no.nav.helse.sporhund.infrastructure.db.DbConfig
import no.nav.helse.sporhund.infrastructure.db.PgTransactionProvider
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.system.exitProcess

/** Mottaker for logg-hjelperne, slik at loggeren får navnet PurringJob. */
private object PurringJob

fun main() {
    val vellykket =
        try {
            kjørPurringJobb()
            true
        } catch (e: Throwable) {
            PurringJob.loggError("Purring-jobb feilet: ${e::class.qualifiedName}", e)
            false
        }
    // team-logs-appenderen sender asynkront; stopp logback så bufferen tømmes før JVM-en avslutter
    (LoggerFactory.getILoggerFactory() as? LoggerContext)?.stop()
    if (!vellykket) exitProcess(1)
}

private fun kjørPurringJobb() {
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

    PurringJob.loggInfo("Starter purring-jobb")
    sendPurringerForUtlopteFrister(transactionProvider)
    PurringJob.loggInfo("Purring-jobb ferdig")
}

internal fun sendPurringerForUtlopteFrister(
    transactionProvider: TransactionProvider,
    nå: Instant = Instant.now(),
) {
    transactionProvider.transaction {
        dialogRepository
            .finnÅpneDialoger()
            .filter { dialog ->
                dialog.status == Dialogstatus.ForespørselSendt &&
                    !dialog.harFåttSvar() &&
                    dialog.frist() <= nå
            }.forEach { dialog ->
                val conversationRef = "conversationRef" to dialog.conversationRef.value
                PurringJob.loggInfo("Sender purring for dialog", conversationRef)
                try {
                    val purring = dialog.sendPurring()
                    dialogRepository.lagre(dialog)
                    kommandoKø.leggTil(LagDialogmeldingPdf.nyPurring(purring, dialog))
                } catch (e: Exception) {
                    PurringJob.loggError("Klarte ikke å sende purring for dialog", e, conversationRef)
                    throw e
                }
            }
    }
}
