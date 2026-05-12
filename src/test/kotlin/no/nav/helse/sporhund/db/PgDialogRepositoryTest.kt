package no.nav.helse.sporhund.db

import no.nav.helse.sporhund.db.testhelpers.DbTest
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.testhelpers.lagBehandler
import no.nav.helse.sporhund.domain.testhelpers.lagBehandlerRef
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.domain.testhelpers.lagNavIdent
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PgDialogRepositoryTest : DbTest() {
    @Test
    fun `kan lagre og hente dialog med FraNav-melding`() {
        // given
        val repository = sessionContext.dialogRepository
        val dialog = nyDialogMedFraNavMelding()

        // when
        repository.lagre(dialog)
        val funnet = repository.finnDialog(dialog.conversationRef)

        // then
        assertNotNull(funnet)
        assertEquals(dialog.conversationRef, funnet.conversationRef)
        assertEquals(dialog.identitetsnummer, funnet.identitetsnummer)
        assertEquals(1, funnet.meldinger.size)
        val lagret = funnet.meldinger.first()
        val original = dialog.meldinger.first()
        assertIs<Dialogmelding.FraNav>(lagret)
        assertIs<Dialogmelding.FraNav>(original)
        assertEquals(original.id, lagret.id)
        assertEquals(original.melding, lagret.melding)
        assertEquals(original.saksbehandler, lagret.saksbehandler)
        assertEquals(original.behandlerRef, lagret.behandlerRef)
        assertEquals(original.behandler, lagret.behandler)
    }

    @Test
    fun `kan lagre og hente dialog med FraBehandler-melding`() {
        // given
        val repository = sessionContext.dialogRepository
        val dialog = nyDialogMedFraNavMelding()
        val fraBehandlerMelding =
            Dialogmelding.FraBehandler(
                id = DialogmeldingId(UUID.randomUUID()),
                tidspunkt = Instant.now(),
                melding = "Svar fra behandler",
                behandler = lagBehandler(),
                antallVedlegg = 2,
            )
        dialog.nyMelding(fraBehandlerMelding)

        // when
        repository.lagre(dialog)
        val funnet = repository.finnDialog(dialog.conversationRef)

        // then
        assertNotNull(funnet)
        assertEquals(2, funnet.meldinger.size)
        val lagret = funnet.meldinger.last()
        assertIs<Dialogmelding.FraBehandler>(lagret)
        assertEquals(fraBehandlerMelding.id, lagret.id)
        assertEquals(fraBehandlerMelding.melding, lagret.melding)
        assertEquals(fraBehandlerMelding.antallVedlegg, lagret.antallVedlegg)
        assertEquals(fraBehandlerMelding.behandler, lagret.behandler)
    }

    @Test
    fun `returnerer null for dialog som ikke finnes`() {
        // given
        val repository = sessionContext.dialogRepository

        // when
        val funnet = repository.finnDialog(ConversationRef(UUID.randomUUID()))

        // then
        assertNull(funnet)
    }

    @Test
    fun `oppdaterer eksisterende dialog ved ny lagring (upsert)`() {
        // given
        val repository = sessionContext.dialogRepository
        val dialog = nyDialogMedFraNavMelding()
        repository.lagre(dialog)

        // when
        dialog.nyMelding(nyFraNavMelding())
        repository.lagre(dialog)
        val funnet = repository.finnDialog(dialog.conversationRef)

        // then
        assertNotNull(funnet)
        assertEquals(2, funnet.meldinger.size)
    }

    private fun nyDialogMedFraNavMelding(): Dialog =
        Dialog.ny(
            identitetsnummer = lagIdentitetsnummer(),
            melding = nyFraNavMelding(),
        )

    private fun nyFraNavMelding(): Dialogmelding.FraNav =
        Dialogmelding.FraNav.ny(
            saksbehandler = lagNavIdent(),
            behandler = lagBehandler(),
            behandlerRef = lagBehandlerRef(),
            melding = "En melding til behandler",
        )
}
