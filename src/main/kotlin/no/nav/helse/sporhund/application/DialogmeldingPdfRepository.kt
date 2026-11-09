package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.DialogmeldingId
import java.util.UUID

interface DialogmeldingPdfRepository {
    fun lagre(
        meldingId: DialogmeldingId<UUID>,
        pdf: ByteArray,
    )

    fun finn(meldingId: DialogmeldingId<UUID>): ByteArray?
}
