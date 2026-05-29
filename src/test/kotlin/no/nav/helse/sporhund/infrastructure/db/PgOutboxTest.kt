package no.nav.helse.sporhund.infrastructure.db

import java.util.*
import kotlin.test.Test
import kotlin.test.assertTrue
import no.nav.helse.sporhund.application.NyDialogmeldingFraNav
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.meldinger
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.NyDialogmeldingFraNavEvent
import no.nav.helse.sporhund.domain.testhelpers.lagBehandlerRef
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.infrastructure.db.testhelpers.DbTest
import org.junit.jupiter.api.Assertions.assertEquals

class PgOutboxTest : DbTest() {
    @Test
    fun `kan lagre og hente outbox-meldinger`() {
        // given
        val outbox = sessionContext.outbox

        // when
        outbox.nyMelding(nyOutboxMelding())
        val funnet = outbox.meldinger<NyDialogmeldingFraNav>()

        // then
        assertEquals(1, funnet.size)
    }

    @Test
    fun `henter kun usendte meldinger`() {
        // given
        val outbox = sessionContext.outbox
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

    private fun nyOutboxMelding(): NyDialogmeldingFraNav =
        OutboxMelding.nyDialogmeldingFraNav(
            NyDialogmeldingFraNavEvent(
                conversationRef = ConversationRef(UUID.randomUUID()),
                behandlerRef = lagBehandlerRef(),
                identitetsnummer = lagIdentitetsnummer(),
                meldingId = DialogmeldingId(UUID.randomUUID()),
                tekst = "En tekst"
            )
        )
}
