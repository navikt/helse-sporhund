package no.nav.helse.sporhund.e2e

import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.application.PersonPseudoId
import no.nav.helse.sporhund.infrastructure.api.ApiDialogmeldingStatus
import java.util.UUID
import kotlin.test.assertEquals

@DslMarker
annotation class E2EDsl

/**
 * Kjører et E2E-forløp mot den delte E2E-appen med en ny person.
 * Forløpet skrives som `gitt { … }` (valgfri), én `når { … }` og `så { … }`, i den rekkefølgen.
 */
fun e2e(block: E2EKontekst.() -> Unit) {
    val kontekst = E2EKontekst(E2EForløp(E2EMiljø.instans))
    kontekst.block()
    kontekst.sjekkAtForløpetErFullstendig()
}

@E2EDsl
class E2EKontekst internal constructor(
    private val forløp: E2EForløp,
) {
    val personPseudoId: PersonPseudoId get() = forløp.personPseudoId
    val conversationRef: UUID get() = forløp.conversationRef

    private var fase = Fase.START

    fun gitt(block: suspend Gitt.() -> Unit) {
        gåTil(Fase.GITT)
        runBlocking { Gitt(Når(forløp), Så(forløp)).block() }
    }

    fun når(block: suspend Når.() -> Unit) {
        gåTil(Fase.NÅR)
        runBlocking { Når(forløp).block() }
    }

    fun så(block: suspend Så.() -> Unit) {
        gåTil(Fase.SÅ)
        runBlocking { Så(forløp).block() }
    }

    internal fun sjekkAtForløpetErFullstendig() = check(fase == Fase.SÅ) { "Forløpet må ha én når { … } etterfulgt av så { … }" }

    private fun gåTil(neste: Fase) {
        check(fase < neste) { "${neste.navn} { … } kan ikke komme etter ${fase.navn} { … }. Rekkefølgen er gitt (valgfri), én når og så" }
        check(neste != Fase.SÅ || fase == Fase.NÅR) { "så { … } må komme etter når { … }" }
        fase = neste
    }

    private enum class Fase(
        val navn: String,
    ) {
        START("start"),
        GITT("gitt"),
        NÅR("når"),
        SÅ("så"),
    }
}

/** Oppsett: hvert steg kjører handlingen og venter til effekten er ferdig, så testen starter i en kjent tilstand. */
@E2EDsl
class Gitt internal constructor(
    private val når: Når,
    private val så: Så,
) {
    /** Forespørselen er sendt til behandler på Kafka og journalført. */
    suspend fun saksbehandlerHarSendtForespørsel(tekst: String) {
        når.saksbehandlerSenderForespørsel(tekst)
        så.behandlerHarMottattDialogmelding(tekst, medVedlegg = true)
        så.dialogmeldingenErJournalført()
    }

    /** Svaret er mottatt og synlig for saksbehandler. */
    suspend fun behandlerHarSvart(tekst: String) {
        når.behandlerSvarer(tekst)
        så.saksbehandlerSerSvaret(tekst)
    }
}

/** Handlingen testen handler om. */
@E2EDsl
class Når internal constructor(
    private val forløp: E2EForløp,
) {
    suspend fun saksbehandlerSenderForespørsel(tekst: String) = forløp.sendForespørsel(tekst)

    fun behandlerSvarer(tekst: String) = forløp.sendSvarFraBehandler(tekst)

    suspend fun saksbehandlerSvarer(tekst: String) = forløp.sendSvarFraSaksbehandler(tekst)

    suspend fun saksbehandlerFerdigstillerDialogen() = forløp.ferdigstill()

    /** Purringsjobben kjører etter at fristen for svar er ute. */
    fun fristenForSvarGårUt() = forløp.fristenGårUt()
}

/** Forventninger. Stegene venter til forventningen er oppfylt, eller feiler etter en kort timeout. */
@E2EDsl
class Så internal constructor(
    private val forløp: E2EForløp,
) {
    /** Behandler har fått meldingen med [tekst] på Kafka, og hver melding fra saksbehandler er sendt nøyaktig én gang. */
    suspend fun behandlerHarMottattDialogmelding(
        tekst: String,
        medVedlegg: Boolean,
    ) = forløp.sjekkMeldingTilBehandler(tekst, medVedlegg)

    /** Den siste meldingen fra saksbehandler er journalført med samme PDF som vedlegget til behandler. */
    suspend fun dialogmeldingenErJournalført() = forløp.ventPåJournalføring()

    /** Behandler har fått nøyaktig én purring, med PDF fra purring-malen i Sprinter. */
    suspend fun behandlerHarMottattPurring() = forløp.sjekkPurringTilBehandler()

    /** Purringen er journalført på samme sak, bruker og behandler som forespørselen, med samme PDF som vedlegget til behandler. */
    suspend fun purringenErJournalført() = forløp.ventPåJournalføringAvPurring()

    suspend fun saksbehandlerSerSvaret(tekst: String) = forløp.ventPåSvarHosSaksbehandler(tekst)

    suspend fun dialogenErFerdigstilt() = assertEquals(ApiDialogmeldingStatus.FERDIGSTILT, forløp.dialogstatus(), "Dialogen skal være ferdigstilt")
}
