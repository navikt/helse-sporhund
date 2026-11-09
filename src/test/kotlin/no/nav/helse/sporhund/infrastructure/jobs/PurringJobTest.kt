package no.nav.helse.sporhund.infrastructure.jobs

import no.nav.helse.sporhund.application.InMemoryTransactionProvider
import no.nav.helse.sporhund.application.kommando.LagDialogmeldingPdf
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.domain.testhelpers.*
import no.nav.helse.sporhund.sendPurringerForUtlopteFrister
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PurringJobTest {
    private val transactionProvider = InMemoryTransactionProvider()

    // ── sendPurringerForUtlopteFrister ───────────────────────────────────────

    @Test
    fun `legger purringen i kommandokøen når frist er passert og status er ForespørselSendt`() {
        val forfallenDialog =
            lagDialog(
                status = Dialogstatus.ForespørselSendt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        transactionProvider.dialogRepository.lagre(forfallenDialog)

        sendPurringerForUtlopteFrister(transactionProvider)

        val purring = transactionProvider.dialogRepository.finnDialog(forfallenDialog.conversationRef)!!.nyesteMelding()
        val kommando = lagPdfKommandoer().single()
        assertEquals(forfallenDialog.conversationRef, kommando.conversationRef)
        assertEquals(purring.id, kommando.meldingId)
        assertNull(kommando.avsenderNavn)
    }

    @Test
    fun `setter status til PurringSendt etter at purring er sendt`() {
        val forfallenDialog =
            lagDialog(
                status = Dialogstatus.ForespørselSendt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        transactionProvider.dialogRepository.lagre(forfallenDialog)

        sendPurringerForUtlopteFrister(transactionProvider)

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

        sendPurringerForUtlopteFrister(transactionProvider)

        assertTrue(lagPdfKommandoer().isEmpty())
    }

    @Test
    fun `sender ikke purring når status er SvarMottatt`() {
        val dialog =
            lagDialog(
                status = Dialogstatus.SvarMottatt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        transactionProvider.dialogRepository.lagre(dialog)

        sendPurringerForUtlopteFrister(transactionProvider)

        assertTrue(lagPdfKommandoer().isEmpty())
    }

    @Test
    fun `sender ikke purring når status er PurringSendt`() {
        val dialog =
            lagDialog(
                status = Dialogstatus.PurringSendt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        transactionProvider.dialogRepository.lagre(dialog)

        sendPurringerForUtlopteFrister(transactionProvider)

        assertTrue(lagPdfKommandoer().isEmpty())
    }

    @Test
    fun `sender ikke purring når dialog er lukket`() {
        val lukketDialog =
            lagDialog(
                status = Dialogstatus.DialogLukket,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        transactionProvider.dialogRepository.lagre(lukketDialog)

        sendPurringerForUtlopteFrister(transactionProvider)

        assertTrue(lagPdfKommandoer().isEmpty())
    }

    @Test
    fun `sender ikke purring når behandler har svart`() {
        val dialog =
            lagDialog(
                status = Dialogstatus.SvarMottatt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        dialog.nyMelding(lagFraBehandlerMelding())
        transactionProvider.dialogRepository.lagre(dialog)

        sendPurringerForUtlopteFrister(transactionProvider)

        assertTrue(lagPdfKommandoer().isEmpty())
    }

    @Test
    fun `sender ikke purring når meldingen blitt avvist`() {
        val dialog =
            lagDialog(
                status = Dialogstatus.Avvist,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        transactionProvider.dialogRepository.lagre(dialog)
        sendPurringerForUtlopteFrister(transactionProvider)
        assertTrue(lagPdfKommandoer().isEmpty())
    }

    @Test
    fun `sender ikke purring når behandler har svart og nav har sendt ny melding etterpå`() {
        val dialog =
            lagDialog(
                status = Dialogstatus.ForespørselSendt,
                melding = lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)),
            )
        dialog.nyMelding(lagFraBehandlerMelding())
        dialog.nyMelding(lagFraNavMelding(opprettet = Instant.now().minusSeconds(22 * 24 * 3600)))
        transactionProvider.dialogRepository.lagre(dialog)

        sendPurringerForUtlopteFrister(transactionProvider)

        assertTrue(lagPdfKommandoer().isEmpty())
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

        sendPurringerForUtlopteFrister(transactionProvider)

        val refs = lagPdfKommandoer().map { it.conversationRef }
        assertEquals(2, refs.size)
        assertTrue(forfallen1.conversationRef in refs)
        assertTrue(forfallen2.conversationRef in refs)
    }

    private fun lagPdfKommandoer() = transactionProvider.kommandoKø.kommandoer().filterIsInstance<LagDialogmeldingPdf>()
}
