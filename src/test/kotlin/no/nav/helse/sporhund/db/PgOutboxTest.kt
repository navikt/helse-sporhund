package no.nav.helse.sporhund.db

import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.OutboxMeldingId
import no.nav.helse.sporhund.db.testhelpers.DbTest
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.NyDialogmeldingFraNavEvent
import no.nav.helse.sporhund.domain.testhelpers.lagBehandlerRef
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*
import kotlin.test.Test
import kotlin.test.assertTrue

class PgOutboxTest : DbTest() {
    @Test
    fun `kan lagre og hente outbox-meldinger`() {
        // given
        val outbox = sessionContext.outbox

        // when
        outbox.nyMelding(
            nyOutboxMelding(OutboxMeldingId(UUID.randomUUID())),
        )
        val funnet = outbox.meldinger()

        // then
        assertEquals(1, funnet.size)
    }

    @Test
    fun `henter kun usendte meldinger`() {
        // given
        val outbox = sessionContext.outbox
        val outboxMeldingIdForMeldingSomErSendt = OutboxMeldingId(UUID.randomUUID())
        val outboxMeldingIdForMeldingSomIkkeErSendt = OutboxMeldingId(UUID.randomUUID())

        outbox.nyMelding(nyOutboxMelding(outboxMeldingIdForMeldingSomErSendt))
        outbox.meldingSendt(outboxMeldingIdForMeldingSomErSendt)
        outbox.nyMelding(nyOutboxMelding(outboxMeldingIdForMeldingSomIkkeErSendt))

        // when
        val funnet = outbox.meldinger()

        // then
        assertTrue(funnet.none { it.id == outboxMeldingIdForMeldingSomErSendt })
        assertTrue(funnet.any { it.id == outboxMeldingIdForMeldingSomIkkeErSendt })
    }

    private fun nyOutboxMelding(outboxMeldingIdForMeldingSomErSendt: OutboxMeldingId): OutboxMelding =
        OutboxMelding(
            id = outboxMeldingIdForMeldingSomErSendt,
            event =
                NyDialogmeldingFraNavEvent(
                    ConversationRef(UUID.randomUUID()),
                    lagBehandlerRef(),
                    identitetsnummer = lagIdentitetsnummer(),
                    meldingId = DialogmeldingId(UUID.randomUUID()),
                    tekst = "En tekst",
                ),
        )
}
