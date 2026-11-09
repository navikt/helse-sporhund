package no.nav.helse.sporhund.infrastructure.db

import kotliquery.Session
import no.nav.helse.sporhund.application.DialogmeldingPdfRepository
import no.nav.helse.sporhund.domain.DialogmeldingId
import java.util.UUID

class PgDialogmeldingPdfRepository(
    private val session: Session,
) : DialogmeldingPdfRepository {
    override fun lagre(
        meldingId: DialogmeldingId<UUID>,
        pdf: ByteArray,
    ) {
        asSQL(
            """INSERT INTO dialogmelding_pdf(melding_id, pdf) VALUES (:melding_id, :pdf)""",
            "melding_id" to meldingId.value,
            "pdf" to pdf,
        ).update(session)
    }

    override fun finn(meldingId: DialogmeldingId<UUID>): ByteArray? =
        asSQL(
            """SELECT pdf FROM dialogmelding_pdf WHERE melding_id = :melding_id""",
            "melding_id" to meldingId.value,
        ).single(session) { it.bytes("pdf") }
}
