package no.nav.helse.sporhund.api

import java.util.UUID

// === Behandler types ===

enum class ApiBehandlerType {
    FASTLEGE,
    FASTLEGEVIKAR,
    SYKMELDER,
}

enum class ApiBehandlerKategori {
    LEGE,
    FYSIOTERAPEUT,
    KIROPRAKTOR,
    MANUELLTERAPEUT,
    TANNLEGE,
    PSYKOLOG,
}

data class ApiBehandlerNavn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)

data class ApiLegekontor(
    val kontor: String?,
    val orgnummer: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
)

data class ApiBehandler(
    val id: String,
    val navn: ApiBehandlerNavn,
    val type: ApiBehandlerType?,
    val kategori: ApiBehandlerKategori,
    val legekontor: ApiLegekontor,
    val telefonnummer: String?,
)

// === Detail types (full dialog with messages) ===

data class ApiVedlegg(
    val navn: String,
    val url: String,
)

data class ApiDialogmelding(
    val fagomrade: ApiFagomrade,
    val meldingstype: ApiDialogmeldingType,
    val melding: String,
    val tid: String,
    val fraNav: Boolean,
    val vedlegg: List<ApiVedlegg>,
)

data class ApiDialogDetails(
    val conversationRef: UUID,
    val behandler: ApiBehandler,
    val tid: String,
    val dialogmeldinger: List<ApiDialogmelding>,
)

// === Summary types (inbox list) ===

data class ApiDialogOppsummering(
    val conversationRef: UUID,
    val behandler: ApiBehandler,
    val fagomrade: ApiFagomrade,
    val meldingstype: ApiDialogmeldingType,
    val tid: String,
    val antallMeldinger: Int,
    val antallVedlegg: Int,
)

enum class ApiDialogmeldingStatus {
    SENDT,
    PURRING_SENDT,
    MOTTATT,
    FERDIGSTILT,
}

data class ApiDialogmeldingOppgave(
    val conversationRef: UUID,
    val personPseudoId: UUID,
    val dato: String,
    val frist: String,
    val fagomrade: ApiFagomrade,
    val soker: String,
    val meldingstype: ApiDialogmeldingType,
    val status: ApiDialogmeldingStatus,
)

// === Request type ===

enum class ApiFagomrade {
    ENKELTSTAENDE_BEHANDLINGSDAGER,
    TILBAKEDATERING,
    YRKESSKADE,
    BESTRIDELSE,
}

enum class ApiDialogmeldingType {
    TILLEGGSOPPLYSNINGER,
    SPESIALISTERKLAERING,
    GJELDER_PASIENT,
}

data class ApiNyDialogmelding(
    val behandler: ApiBehandler,
    val fagomrade: ApiFagomrade,
    val meldingstype: ApiDialogmeldingType,
    val melding: String,
)

data class ApiSvarPaDialog(
    val melding: String,
)
