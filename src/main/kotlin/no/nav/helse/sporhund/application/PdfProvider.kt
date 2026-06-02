package no.nav.helse.sporhund.application

interface PdfProvider {
    fun genererPdf(meldingTilBehandlerPdfInput: MeldingTilBehandlerPdfInput): ByteArray
}

data class MeldingTilBehandlerPdfInput(
    val conversationRef: String,
    val fra: Fra,
    val til: Til,
    val tidspunkt: String,
    val gjelder: Gjelder,
    val meldingstype: String,
    val fagområde: String,
    val melding: String,
) {
    data class Fra(
        val NAVIdent: String,
        val navn: String,
    )

    data class Til(
        val navn: String,
        val kontor: Kontor,
    ) {
        data class Kontor(
            val navn: String,
            val organisasjonsnummer: String,
            val adresse: Adresse,
        ) {
            data class Adresse(
                val gate: String,
                val postnummer: String,
                val poststed: String,
            )
        }
    }

    data class Gjelder(
        val fødselsnummer: String,
        val navn: String,
    )
}
