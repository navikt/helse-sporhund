package no.nav.helse.sporhund.infrastructure.db

import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.infrastructure.db.testhelpers.DbTest
import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class PgDialogmeldingPdfRepositoryTest : DbTest() {
    @Test
    fun `lagret PDF hentes ut byte for byte`() =
        test {
            val meldingId = DialogmeldingId(UUID.randomUUID())
            val pdf = Random.nextBytes(1024)

            dialogmeldingPdfRepository.lagre(meldingId, pdf)

            assertContentEquals(pdf, dialogmeldingPdfRepository.finn(meldingId))
            assertNull(dialogmeldingPdfRepository.finn(DialogmeldingId(UUID.randomUUID())))
        }
}
