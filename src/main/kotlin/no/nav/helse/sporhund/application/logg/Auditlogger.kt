package no.nav.helse.sporhund.application.logg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.helse.sporhund.domain.Identitetsnummer
import no.nav.helse.sporhund.domain.Saksbehandler
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

internal object Auditlogger {
    private val auditLog = LoggerFactory.getLogger("auditLogger")

    private val teller: Counter =
        Counter
            .builder("auditlog_total")
            .description("Teller antall auditlogginnslag")
            .register(Metrics.globalRegistry)

    fun auditlogge(
        saksbehandler: Saksbehandler,
        identitetsnummer: Identitetsnummer,
        meldingOmHvaSomSkjedd: String,
    ) {
        logg(Level.INFO, saksbehandler, identitetsnummer.value, meldingOmHvaSomSkjedd, "")
    }

    fun auditloggeManglendeTilgang(
        saksbehandler: Saksbehandler,
        identitetsnummer: Identitetsnummer,
    ) {
        logg(Level.WARN, saksbehandler, identitetsnummer.value, "", " flexString1=Deny")
    }

    private fun logg(
        level: Level,
        saksbehandler: Saksbehandler,
        duid: String,
        meldingOmHvaSomSkjedd: String,
        suffix: String,
    ) {
        teller.increment()
        val message =
            "end=${System.currentTimeMillis()}" +
                " suid=${saksbehandler.ident.value}" +
                " duid=$duid" +
                " msg=$meldingOmHvaSomSkjedd" +
                suffix
        auditLog.atLevel(level).log(message)
        teamLogs.debug("audit-logget: $level - $message")
    }
}
