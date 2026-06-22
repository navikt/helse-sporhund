package no.nav.helse.sporhund.infrastructure.db

import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.domain.testhelpers.lagBehandler
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagFraBehandlerMelding
import no.nav.helse.sporhund.domain.testhelpers.lagFraNavMelding
import no.nav.helse.sporhund.domain.testhelpers.lagFraSystemMelding
import no.nav.helse.sporhund.infrastructure.db.testhelpers.DbTest
import java.util.*
import kotlin.test.*

class PgDialogRepositoryTest : DbTest() {
    @Test
    fun `kan lagre og hente dialog med FraNav-melding`() =
        test {
            // given
            val repository = dialogRepository
            val dialog = lagDialog()

            // when
            repository.lagre(dialog)
            val funnet = repository.finnDialog(dialog.conversationRef)

            // then
            assertNotNull(funnet)
            assertEquals(dialog.conversationRef, funnet.conversationRef)
            assertEquals(dialog.identitetsnummer, funnet.identitetsnummer)
            assertEquals(dialog.fagområde, funnet.fagområde)
            assertEquals(dialog.søker, funnet.søker)
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
    fun `kan lagre og hente dialog med FraBehandler-melding`() =
        test {
            // given
            val repository = dialogRepository
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
    fun `returnerer null for dialog som ikke finnes`() =
        test {
            // given
            val repository = dialogRepository

            // when
            val funnet = repository.finnDialog(ConversationRef(UUID.randomUUID()))

            // then
            assertNull(funnet)
        }

    @Test
    fun `oppdaterer eksisterende dialog ved ny lagring (upsert)`() =
        test {
            // given
            val repository = dialogRepository
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
    fun `finnÅpneDialoger returnerer tom liste når det ikke finnes noen dialoger`() =
        test {
            // given
            val repository = dialogRepository

            // when
            val funnet = repository.finnÅpneDialoger()

            // then
            assertTrue(funnet.isEmpty())
        }

    @Test
    fun `finnÅpneDialoger returnerer dialog som ikke er lukket`() =
        test {
            // given
            val repository = dialogRepository
            val dialog = lagDialog()
            repository.lagre(dialog)

            // when
            val funnet = repository.finnÅpneDialoger()

            // then
            assertEquals(1, funnet.size)
            assertEquals(dialog.conversationRef, funnet.first().conversationRef)
        }

    @Test
    fun `finnÅpneDialoger returnerer ikke dialog med status DialogLukket`() =
        test {
            // given
            val repository = dialogRepository
            val lukketDialog = lagDialog(status = Dialogstatus.DialogLukket)
            repository.lagre(lukketDialog)

            // when
            val funnet = repository.finnÅpneDialoger()

            // then
            assertTrue(funnet.isEmpty())
        }

    @Test
    fun `finnÅpneDialoger returnerer kun ikke-lukkede dialoger`() =
        test {
            // given
            val repository = dialogRepository
            val åpenDialog = lagDialog()
            val lukketDialog = lagDialog(status = Dialogstatus.DialogLukket)
            repository.lagre(åpenDialog)
            repository.lagre(lukketDialog)

            // when
            val funnet = repository.finnÅpneDialoger()

            // then
            assertEquals(1, funnet.size)
            assertEquals(åpenDialog.conversationRef, funnet.first().conversationRef)
        }

    @Test
    fun `finnÅpneDialoger returnerer alle statuser unntatt DialogLukket`() =
        test {
            // given
            val repository = dialogRepository
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
            val funnet = repository.finnÅpneDialoger()

            // then
            assertEquals(statuser.size, funnet.size)
            val funnedeRefs = funnet.map { it.conversationRef }.toSet()
            dialoger.forEach { dialog ->
                assertTrue(dialog.conversationRef in funnedeRefs)
            }
        }

    @Test
    fun `finnDialogVedMeldingId finner dialog med FraNav-melding som ikke er kvittert`() =
        test {
            // given
            val fraNavMelding = lagFraNavMelding()
            val dialog = lagDialog(melding = fraNavMelding)
            dialogRepository.lagre(dialog)

            // when
            val funnet = dialogRepository.finnDialogVedMeldingId(fraNavMelding.id.value)

            // then
            assertNotNull(funnet)
            assertEquals(dialog.conversationRef, funnet.conversationRef)
        }

    @Test
    fun `finnDialogVedMeldingId finner dialog med FraSystem-melding som ikke er kvittert`() =
        test {
            // given
            val fraSystemMelding = lagFraSystemMelding()
            val dialog = lagDialog()
            dialog.nyMelding(fraSystemMelding)
            dialogRepository.lagre(dialog)

            // when
            val funnet = dialogRepository.finnDialogVedMeldingId(fraSystemMelding.id.value)

            // then
            assertNotNull(funnet)
            assertEquals(dialog.conversationRef, funnet.conversationRef)
        }

    @Test
    fun `finnDialogVedMeldingId returnerer null når melding allerede er kvittert`() =
        test {
            // given
            val fraNavMelding = lagFraNavMelding()
            val dialog = lagDialog(melding = fraNavMelding)
            dialog.mottaKvittering(fraNavMelding.id, avvist = false)
            dialogRepository.lagre(dialog)

            // when
            val funnet = dialogRepository.finnDialogVedMeldingId(fraNavMelding.id.value)

            // then
            assertNull(funnet)
        }

    @Test
    fun `finnDialogVedMeldingId returnerer null når meldingId ikke finnes`() =
        test {
            // given
            dialogRepository.lagre(lagDialog())

            // when
            val funnet = dialogRepository.finnDialogVedMeldingId(UUID.randomUUID())

            // then
            assertNull(funnet)
        }

    @Test
    fun `finnDialogVedMeldingId returnerer ikke dialog der en annen melding i samme dialog er kvittert`() =
        test {
            // given - dialog with two FraNav messages, first is kvittert, second is not
            val kvittertMelding = lagFraNavMelding()
            val ukvittertMelding = lagFraNavMelding()
            val dialog = lagDialog(melding = kvittertMelding)
            dialog.nyMelding(ukvittertMelding)
            dialog.mottaKvittering(kvittertMelding.id, avvist = false)
            dialogRepository.lagre(dialog)

            // when
            val søkPåKvittert = dialogRepository.finnDialogVedMeldingId(kvittertMelding.id.value)
            val søkPåUkvittert = dialogRepository.finnDialogVedMeldingId(ukvittertMelding.id.value)

            // then
            assertNull(søkPåKvittert)
            assertNotNull(søkPåUkvittert)
        }
}
