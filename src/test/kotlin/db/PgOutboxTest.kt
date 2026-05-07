package db

import application.OutboxMelding
import application.OutboxMeldingId
import db.testhelpers.DbTest
import domain.ConversationRef
import domain.DialogmeldingId
import domain.NyDialogmeldingFraNavEvent
import domain.testhelpers.lagBehandlerRef
import domain.testhelpers.lagIdentitetsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*
import kotlin.test.Test

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
        assertEquals(1, funnet.size)
        assertEquals(outboxMeldingIdForMeldingSomIkkeErSendt, funnet.single().id)
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
                    type = "En type",
                    tekst = "En tekst",
                ),
        )
}
