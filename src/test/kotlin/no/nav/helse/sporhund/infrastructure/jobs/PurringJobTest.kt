package no.nav.helse.sporhund.infrastructure.jobs

import no.nav.helse.sporhund.application.InMemoryTransactionProvider
import no.nav.helse.sporhund.application.NyDialogmeldingFraNav
import no.nav.helse.sporhund.application.meldinger
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.domain.testhelpers.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PurringJobTest {
    private val transactionProvider = InMemoryTransactionProvider()
    private val running = AtomicBoolean(false)
    private val osloZone = ZoneId.of("Europe/Oslo")

    private fun lagJob(clock: Clock = Clock.systemDefaultZone()) = PurringJob(running, transactionProvider, clock = clock)

    // ── erMidnatt ───────────────────────────────────────────────────────────

    @Test
    fun `erMidnatt returnerer true når klokken er 00 30 Oslo-tid`() {
        val clock =
            Clock.fixed(
                Instant.parse("2026-06-02T22:30:00Z"), // 00:30 CEST (UTC+2)
                osloZone,
            )
        assertTrue(lagJob(clock).erMidnatt())
    }

    @Test
    fun `erMidnatt returnerer false når klokken er 12 00 Oslo-tid`() {
        val clock =
            Clock.fixed(
                Instant.parse("2026-06-03T10:00:00Z"), // 12:00 CEST (UTC+2)
                osloZone,
            )
        assertFalse(lagJob(clock).erMidnatt())
    }

    @Test
    fun `erMidnatt returnerer false når klokken er 23 59 Oslo-tid`() {
        val clock =
            Clock.fixed(
                Instant.parse("2026-06-03T21:59:00Z"), // 23:59 CEST (UTC+2)
                osloZone,
            )
        assertFalse(lagJob(clock).erMidnatt())
    }

    // ── sendPurringerForUtlopteFrister ───────────────────────────────────────

    @Test
    fun `sender purring på kafka når frist er passert og status er ForespørselSendt`() {
        val forfallenDialog =
            lagDialog(
                status = Dialogstatus.ForespørselSendt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        transactionProvider.dialogRepository.lagre(forfallenDialog)

        lagJob().sendPurringerForUtlopteFrister()

        val outboxMeldinger = transactionProvider.outbox.meldinger<NyDialogmeldingFraNav>()
        assertEquals(1, outboxMeldinger.size)
        assertEquals(forfallenDialog.conversationRef, outboxMeldinger.single().nyDialogmeldingFraNavEvent.conversationRef)
    }

    @Test
    fun `kafka-melding for purring har dialogmeldingKode 2 (erPurring = true)`() {
        val forfallenDialog =
            lagDialog(
                status = Dialogstatus.ForespørselSendt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        transactionProvider.dialogRepository.lagre(forfallenDialog)

        lagJob().sendPurringerForUtlopteFrister()

        val event =
            transactionProvider.outbox
                .meldinger<NyDialogmeldingFraNav>()
                .single()
                .nyDialogmeldingFraNavEvent
        assertTrue(event.erPurring)
    }

    @Test
    fun `setter status til PurringSendt etter at purring er sendt`() {
        val forfallenDialog =
            lagDialog(
                status = Dialogstatus.ForespørselSendt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        transactionProvider.dialogRepository.lagre(forfallenDialog)

        lagJob().sendPurringerForUtlopteFrister()

        val lagretDialog = transactionProvider.dialogRepository.finnDialog(forfallenDialog.conversationRef)!!
        assertEquals(Dialogstatus.PurringSendt, lagretDialog.status)
    }

    @Test
    fun `sender ikke purring når frist ikke er passert`() {
        val ikkeForfallDialog =
            lagDialog(
                status = Dialogstatus.ForespørselSendt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(20 * 24 * 3600)),
            )
        transactionProvider.dialogRepository.lagre(ikkeForfallDialog)

        lagJob().sendPurringerForUtlopteFrister()

        assertTrue(transactionProvider.outbox.meldinger<NyDialogmeldingFraNav>().isEmpty())
    }

    @Test
    fun `sender ikke purring når status er SvarMottatt`() {
        val dialog =
            lagDialog(
                status = Dialogstatus.SvarMottatt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        transactionProvider.dialogRepository.lagre(dialog)

        lagJob().sendPurringerForUtlopteFrister()

        assertTrue(transactionProvider.outbox.meldinger<NyDialogmeldingFraNav>().isEmpty())
    }

    @Test
    fun `sender ikke purring når status er PurringSendt`() {
        val dialog =
            lagDialog(
                status = Dialogstatus.PurringSendt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        transactionProvider.dialogRepository.lagre(dialog)

        lagJob().sendPurringerForUtlopteFrister()

        assertTrue(transactionProvider.outbox.meldinger<NyDialogmeldingFraNav>().isEmpty())
    }

    @Test
    fun `sender ikke purring når dialog er lukket`() {
        val lukketDialog =
            lagDialog(
                status = Dialogstatus.DialogLukket,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        transactionProvider.dialogRepository.lagre(lukketDialog)

        lagJob().sendPurringerForUtlopteFrister()

        assertTrue(transactionProvider.outbox.meldinger<NyDialogmeldingFraNav>().isEmpty())
    }

    @Test
    fun `sender ikke purring når siste melding er fra behandler`() {
        val dialog =
            lagDialog(
                status = Dialogstatus.ForespørselSendt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        dialog.nyMelding(lagFraBehandlerMelding())
        transactionProvider.dialogRepository.lagre(dialog)

        lagJob().sendPurringerForUtlopteFrister()

        assertTrue(transactionProvider.outbox.meldinger<NyDialogmeldingFraNav>().isEmpty())
    }

    @Test
    fun `sender purring til alle kvalifiserende dialoger`() {
        val forfallen1 =
            lagDialog(
                status = Dialogstatus.ForespørselSendt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        val forfallen2 =
            lagDialog(
                status = Dialogstatus.ForespørselSendt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(30 * 24 * 3600)),
            )
        val ikkeForfalt =
            lagDialog(
                status = Dialogstatus.ForespørselSendt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(5 * 24 * 3600)),
            )
        transactionProvider.dialogRepository.lagre(forfallen1)
        transactionProvider.dialogRepository.lagre(forfallen2)
        transactionProvider.dialogRepository.lagre(ikkeForfalt)

        lagJob().sendPurringerForUtlopteFrister()

        val outboxRefs =
            transactionProvider.outbox
                .meldinger<NyDialogmeldingFraNav>()
                .map { it.nyDialogmeldingFraNavEvent.conversationRef }
        assertEquals(2, outboxRefs.size)
        assertTrue(forfallen1.conversationRef in outboxRefs)
        assertTrue(forfallen2.conversationRef in outboxRefs)
    }
}
