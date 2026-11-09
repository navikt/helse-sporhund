package no.nav.helse.sporhund.e2e

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Kaller [hent] til den returnerer noe annet enn null, og returnerer verdien.
 * En [AssertionError] fra [hent] tolkes som «ikke ennå» og blir med i feilmeldingen hvis tiden går ut.
 * Andre exceptions avbryter ventingen med en gang.
 */
suspend fun <T : Any> ventPåVerdi(
    beskrivelse: String,
    timeout: Duration = 10.seconds,
    intervall: Duration = 50.milliseconds,
    hent: suspend () -> T?,
): T {
    val frist = TimeSource.Monotonic.markNow() + timeout
    var sisteFeil: AssertionError? = null
    while (true) {
        try {
            hent()?.let { return it }
        } catch (feil: AssertionError) {
            sisteFeil = feil
        }
        if (frist.hasPassedNow()) {
            throw AssertionError("Ventet $timeout på $beskrivelse, men det skjedde ikke" + (sisteFeil?.message?.let { ". Siste feil: $it" } ?: ""), sisteFeil)
        }
        delay(intervall)
    }
}

/** Kaller [sjekk] til den returnerer true. Se [ventPåVerdi]. */
suspend fun ventPå(
    beskrivelse: String,
    timeout: Duration = 10.seconds,
    intervall: Duration = 50.milliseconds,
    sjekk: suspend () -> Boolean,
) {
    ventPåVerdi(beskrivelse, timeout, intervall) { sjekk().takeIf { it } }
}
