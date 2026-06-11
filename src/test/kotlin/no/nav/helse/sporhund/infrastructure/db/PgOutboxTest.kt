package no.nav.helse.sporhund.infrastructure.db

import no.nav.helse.sporhund.application.*
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.NyDialogmeldingFraNavEvent
import no.nav.helse.sporhund.domain.testhelpers.*
import no.nav.helse.sporhund.infrastructure.db.testhelpers.DbTest
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PgOutboxTest : DbTest() {
    @Test
    fun `kan lagre og hente outbox-meldinger`() =
        test {
            // when
            outbox.nyMelding(nyOutboxMelding())
            val funnet = outbox.meldinger<NyDialogmeldingFraNav>()

            // then
            assertEquals(1, funnet.size)
        }

    @Test
    fun `henter kun usendte meldinger`() =
        test {
            // given
            val meldingSomErSendt = nyOutboxMelding()
            val meldingSomIkkeErSendt = nyOutboxMelding()

            outbox.nyMelding(meldingSomErSendt)
            outbox.meldingSendt(meldingSomErSendt.id)
            outbox.nyMelding(meldingSomIkkeErSendt)

            // when
            val funnet = outbox.meldinger<NyDialogmeldingFraNav>()

            // then
            assertTrue(funnet.none { it.id == meldingSomErSendt.id })
            assertTrue(funnet.any { it.id == meldingSomIkkeErSendt.id })
        }

    @Test
    fun `kan lagre og hente OpprettUtgåendeJournalpost`() =
        test {
            val dialog = lagDialog()
            val melding = lagFraNavMelding()
            val avsender = lagSaksbehandler()

            outbox.nyMelding(OutboxMelding.opprettUtgåendeJournalpost(melding, dialog, avsender))
            val funnet = outbox.meldinger<OpprettUtgåendeJournalpost>()

            assertEquals(1, funnet.size)
            with(funnet.single()) {
                assertEquals(dialog.conversationRef, conversationRef)
                assertEquals(melding.id, meldingId)
                assertEquals(melding.melding, tekst)
                assertEquals(avsender.ident, this.avsender.ident)
                assertEquals(avsender.navn, this.avsender.navn)
                assertEquals(melding.behandler.hprNummer, mottaker.hprNummer)
                assertEquals(dialog.identitetsnummer, gjelder)
                assertEquals(dialog.fagområde, fagområde)
            }
        }

    @Test
    fun `OpprettUtgåendeJournalpost filtreres ikke ut av andre meldingstyper`() =
        test {
            outbox.nyMelding(nyOutboxMelding())
            outbox.nyMelding(
                OutboxMelding.opprettUtgåendeJournalpost(
                    lagFraNavMelding(),
                    lagDialog(),
                    lagSaksbehandler(),
                ),
            )

            assertEquals(0, outbox.meldinger<OpprettUtgåendeJournalpost>().count { false })
            assertEquals(1, outbox.meldinger<NyDialogmeldingFraNav>().size)
            assertEquals(1, outbox.meldinger<OpprettUtgåendeJournalpost>().size)
        }

    @Test
    fun `kan lagre og hente KnyttInnkommendeJournalpost`() =
        test {
            val dialog = lagDialog()

            val jourpostIdFraKafkamelding = UUID.randomUUID()

            outbox.nyMelding(OutboxMelding.knyttInnkommendeJournalpost(jourpostIdFraKafkamelding.toString(), dialog))
            val funnet = outbox.meldinger<KnyttInnkommendeJournalpost>()

            assertEquals(1, funnet.size)
            with(funnet.single()) {
                assertEquals(jourpostIdFraKafkamelding.toString(), journalpostId)
                assertEquals(dialog.conversationRef, conversationRef)
            }
        }

    @Test
    fun `KnyttInnkommendeJournalpost filtreres ikke ut av andre meldingstyper`() =
        test {
            val jourpostIdFraKafkamelding = UUID.randomUUID()

            outbox.nyMelding(nyOutboxMelding())
            outbox.nyMelding(
                OutboxMelding.knyttInnkommendeJournalpost(
                    jourpostIdFraKafkamelding.toString(),
                    lagDialog(),
                ),
            )

            assertEquals(1, outbox.meldinger<NyDialogmeldingFraNav>().size)
            assertEquals(0, outbox.meldinger<OpprettUtgåendeJournalpost>().size)
            assertEquals(1, outbox.meldinger<KnyttInnkommendeJournalpost>().size)
        }

    private fun nyOutboxMelding(): NyDialogmeldingFraNav =
        OutboxMelding.nyDialogmeldingFraNav(
            NyDialogmeldingFraNavEvent(
                conversationRef = ConversationRef(UUID.randomUUID()),
                behandlerRef = lagBehandlerRef(),
                identitetsnummer = lagIdentitetsnummer(),
                meldingId = DialogmeldingId(UUID.randomUUID()),
                tekst = "En tekst",
            ),
        )
}
