package no.nav.helse.sporhund.db

import no.nav.helse.sporhund.db.testhelpers.DbTest
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.domain.testhelpers.lagBehandler
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagFraBehandlerMelding
import no.nav.helse.sporhund.domain.testhelpers.lagFraNavMelding
import org.junit.jupiter.api.AfterEach
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PgDialogRepositoryTest : DbTest() {
    @AfterEach
    fun afterEach() {
        tømDatabase()
    }

    @Test
    fun `kan lagre og hente dialog med FraNav-melding`() {
        // given
        val repository = sessionContext.dialogRepository
        val dialog = lagDialog()

        // when
        repository.lagre(dialog)
        val funnet = repository.finnDialog(dialog.conversationRef)

        // then
        assertNotNull(funnet)
        assertEquals(dialog.conversationRef, funnet.conversationRef)
        assertEquals(dialog.identitetsnummer, funnet.identitetsnummer)
        assertEquals(dialog.fagområde, funnet.fagområde)
        assertEquals(dialog.dialogtype, funnet.dialogtype)
        assertEquals(dialog.søkernavn, funnet.søkernavn)
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
        val dialog = lagDialog()
        val fraBehandlerMelding =
            lagFraBehandlerMelding(
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
        val dialog = lagDialog()
        repository.lagre(dialog)

        // when
        dialog.nyMelding(lagFraNavMelding())
        repository.lagre(dialog)
        val funnet = repository.finnDialog(dialog.conversationRef)

        // then
        assertNotNull(funnet)
        assertEquals(2, funnet.meldinger.size)
    }

    @Test
    fun `finnIkkeLukkedeDialoger returnerer tom liste når det ikke finnes noen dialoger`() {
        // given
        val repository = sessionContext.dialogRepository

        // when
        val funnet = repository.finnIkkeLukkedeDialoger()

        // then
        assertTrue(funnet.isEmpty())
    }

    @Test
    fun `finnIkkeLukkedeDialoger returnerer dialog som ikke er lukket`() {
        // given
        val repository = sessionContext.dialogRepository
        val dialog = lagDialog()
        repository.lagre(dialog)

        // when
        val funnet = repository.finnIkkeLukkedeDialoger()

        // then
        assertEquals(1, funnet.size)
        assertEquals(dialog.conversationRef, funnet.first().conversationRef)
    }

    @Test
    fun `finnIkkeLukkedeDialoger returnerer ikke dialog med status DialogLukket`() {
        // given
        val repository = sessionContext.dialogRepository
        val lukketDialog = lagDialog(status = Dialogstatus.DialogLukket)
        repository.lagre(lukketDialog)

        // when
        val funnet = repository.finnIkkeLukkedeDialoger()

        // then
        assertTrue(funnet.isEmpty())
    }

    @Test
    fun `finnIkkeLukkedeDialoger returnerer kun ikke-lukkede dialoger`() {
        // given
        val repository = sessionContext.dialogRepository
        val åpenDialog = lagDialog()
        val lukketDialog = lagDialog(status = Dialogstatus.DialogLukket)
        repository.lagre(åpenDialog)
        repository.lagre(lukketDialog)

        // when
        val funnet = repository.finnIkkeLukkedeDialoger()

        // then
        assertEquals(1, funnet.size)
        assertEquals(åpenDialog.conversationRef, funnet.first().conversationRef)
    }

    @Test
    fun `finnIkkeLukkedeDialoger returnerer alle statuser unntatt DialogLukket`() {
        // given
        val repository = sessionContext.dialogRepository
        val statuser =
            listOf(
                Dialogstatus.ForespørselSendt,
                Dialogstatus.SvarMottatt,
                Dialogstatus.PurringSendt,
            )
        val dialoger =
            statuser.map { status ->
                lagDialog(status = status).also { repository.lagre(it) }
            }

        // when
        val funnet = repository.finnIkkeLukkedeDialoger()

        // then
        assertEquals(statuser.size, funnet.size)
        val funnedeRefs = funnet.map { it.conversationRef }.toSet()
        dialoger.forEach { dialog ->
            assertTrue(dialog.conversationRef in funnedeRefs)
        }
    }
}
