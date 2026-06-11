package no.nav.helse.sporhund.infrastructure.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant
import java.util.*

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

data class ApiNavn(
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
    val hprNummer: Int,
    val navn: ApiNavn,
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

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "avsender")
@JsonSubTypes(
    JsonSubTypes.Type(value = ApiDialogmelding.FraBehandler::class, name = "BEHANDLER"),
    JsonSubTypes.Type(value = ApiDialogmelding.FraNav::class, name = "NAV"),
    JsonSubTypes.Type(value = ApiDialogmelding.FraSystem::class, name = "SYSTEM"),
)
sealed class ApiDialogmelding {
    abstract val fagomrade: ApiFagomrade
    abstract val meldingstype: ApiDialogmeldingType
    abstract val melding: String
    abstract val msgId: String
    abstract val sendtTidspunkt: Instant

    data class FraBehandler(
        override val fagomrade: ApiFagomrade,
        override val meldingstype: ApiDialogmeldingType,
        override val melding: String,
        override val msgId: String,
        override val sendtTidspunkt: Instant,
        val antallVedlegg: Int,
    ) : ApiDialogmelding()

    data class FraNav(
        override val fagomrade: ApiFagomrade,
        override val meldingstype: ApiDialogmeldingType,
        override val melding: String,
        override val msgId: String,
        override val sendtTidspunkt: Instant,
        val saksbehandler: String,
    ) : ApiDialogmelding()

    data class FraSystem(
        override val fagomrade: ApiFagomrade,
        override val meldingstype: ApiDialogmeldingType,
        override val melding: String,
        override val msgId: String,
        override val sendtTidspunkt: Instant,
    ) : ApiDialogmelding()
}

data class ApiDialogDetails(
    val conversationRef: UUID,
    val behandler: ApiBehandler,
    val status: ApiDialogmeldingStatus,
    val dialogmeldinger: List<ApiDialogmelding>,
)

// === Summary types (inbox list) ===

data class ApiDialogOppsummering(
    val conversationRef: UUID,
    val behandler: ApiBehandler,
    val fagomrade: ApiFagomrade,
    val meldingstype: ApiDialogmeldingType,
    val sisteAktivitetTidspunkt: Instant,
    val antallMeldinger: Int,
    val antallVedlegg: Int,
    val status: ApiDialogmeldingStatus,
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
    val sisteAktivitetTidspunkt: Instant,
    val fristTidspunkt: Instant,
    val fagomrade: ApiFagomrade,
    val soker: ApiNavn,
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
    JOURNALNOTAT,
    MEDISINSKE_OPPLYSNINGER,
    EKSTRA_UTTALELSER_FRA_LEGE,
    SPESIALISTERKLAERING,
    UTVIDET_SPESIALISTERKLAERING,
}

data class ApiNyDialogmelding(
    val behandler: ApiBehandler,
    val sokernavn: ApiNavn,
    val fagomrade: ApiFagomrade,
    val meldingstype: ApiDialogmeldingType,
    val melding: String,
)

data class ApiSvarPaDialog(
    val melding: String,
)

data class ApiOppdaterDialogStatus(
    val ferdigstilt: Boolean,
)
