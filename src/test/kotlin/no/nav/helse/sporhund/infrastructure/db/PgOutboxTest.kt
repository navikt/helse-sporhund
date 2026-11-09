package no.nav.helse.sporhund.infrastructure.db

import no.nav.helse.sporhund.application.KnyttInnkommendeJournalpost
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.meldinger
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.infrastructure.db.testhelpers.DbTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PgOutboxTest : DbTest() {
    @Test
    fun `kan lagre og hente KnyttInnkommendeJournalpost`() =
        test {
            val dialog = lagDialog()
            val journalpostIdFraKafkamelding = UUID.randomUUID().toString()

            outbox.nyMelding(OutboxMelding.knyttInnkommendeJournalpost(journalpostIdFraKafkamelding, dialog))
            val funnet = outbox.meldinger<KnyttInnkommendeJournalpost>()

            assertEquals(1, funnet.size)
            with(funnet.single()) {
                assertEquals(journalpostIdFraKafkamelding, journalpostId)
                assertEquals(dialog.conversationRef, conversationRef)
            }
        }

    @Test
    fun `henter kun usendte meldinger`() =
        test {
            val meldingSomErSendt = OutboxMelding.knyttInnkommendeJournalpost("JP-1", lagDialog())
            val meldingSomIkkeErSendt = OutboxMelding.knyttInnkommendeJournalpost("JP-2", lagDialog())

            outbox.nyMelding(meldingSomErSendt)
            outbox.meldingSendt(meldingSomErSendt.id)
            outbox.nyMelding(meldingSomIkkeErSendt)

            val funnet = outbox.meldinger<KnyttInnkommendeJournalpost>()

            assertTrue(funnet.none { it.id == meldingSomErSendt.id })
            assertTrue(funnet.any { it.id == meldingSomIkkeErSendt.id })
        }
}
