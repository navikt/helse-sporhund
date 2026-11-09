package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.DialogmeldingId
import java.util.UUID

class InMemoryDialogmeldingPdfRepository : DialogmeldingPdfRepository {
    private val pdfer = mutableMapOf<DialogmeldingId<UUID>, ByteArray>()

    override fun lagre(
        meldingId: DialogmeldingId<UUID>,
        pdf: ByteArray,
    ) {
        pdfer[meldingId] = pdf
    }

    override fun finn(meldingId: DialogmeldingId<UUID>): ByteArray? = pdfer[meldingId]
}
