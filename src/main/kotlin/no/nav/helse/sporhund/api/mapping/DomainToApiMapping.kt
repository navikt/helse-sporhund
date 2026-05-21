package no.nav.helse.sporhund.api.mapping

import no.nav.helse.sporhund.api.*
import no.nav.helse.sporhund.application.PersonPseudoId
import no.nav.helse.sporhund.domain.*

fun Dialog.tilApiDialogmeldingerOversikt(): ApiDialogOppsummering {
    val (opprinneligBehandler, opprinneligBehandlerRef) = this.opprinneligBehandler()
    return ApiDialogOppsummering(
        conversationRef = conversationRef.value,
        behandler = opprinneligBehandler.tilApiBehandler(opprinneligBehandlerRef),
        fagomrade = this.tilApiFagomrade(),
        meldingstype = this.tilApiDialogmeldingType(),
        sisteAktivitetTidspunkt = this.nyesteMeldingFraNav().tidspunkt,
        antallMeldinger = meldinger.size,
        antallVedlegg = this.antallVedleggTotalt(),
        status = status.tilApiDialogmeldingStatus(),
    )
}

fun Dialog.tilApiDialogmeldingOppgave(personPseudoId: PersonPseudoId): ApiDialogmeldingOppgave =
    ApiDialogmeldingOppgave(
        conversationRef = conversationRef.value,
        personPseudoId = personPseudoId.value,
        sisteAktivitetTidspunkt = this.nyesteMelding().tidspunkt,
        fristTidspunkt = this.frist(),
        fagomrade = this.tilApiFagomrade(),
        meldingstype = this.tilApiDialogmeldingType(),
        soker = this.søkernavn.tilApiNavn(),
        status = status.tilApiDialogmeldingStatus(),
    )

fun Dialog.tilApiDialogDetails(): ApiDialogDetails {
    val (opprinneligBehandler, opprinneligBehandlerRef) = this.opprinneligBehandler()
    return ApiDialogDetails(
        conversationRef = conversationRef.value,
        behandler =
            opprinneligBehandler.tilApiBehandler(opprinneligBehandlerRef),
        status = status.tilApiDialogmeldingStatus(),
        dialogmeldinger =
            meldinger.map { dialogmelding ->
                ApiDialogmelding(
                    fagomrade = this.tilApiFagomrade(),
                    meldingstype = this.tilApiDialogmeldingType(),
                    melding = dialogmelding.melding,
                    sendtTidspunkt = dialogmelding.tidspunkt,
                    fraNav = dialogmelding is Dialogmelding.FraNav,
                    vedlegg = emptyList(),
                )
            },
    )
}

private fun Dialogstatus.tilApiDialogmeldingStatus(): ApiDialogmeldingStatus =
    when (this) {
        Dialogstatus.ForespørselSendt -> ApiDialogmeldingStatus.SENDT
        Dialogstatus.PurringSendt -> ApiDialogmeldingStatus.PURRING_SENDT
        Dialogstatus.SvarMottatt -> ApiDialogmeldingStatus.MOTTATT
        Dialogstatus.DialogLukket -> ApiDialogmeldingStatus.FERDIGSTILT
    }

private fun Dialog.tilApiFagomrade(): ApiFagomrade =
    when (this.fagområde) {
        Fagområde.EnkeltståendeBehandlingsdager -> ApiFagomrade.ENKELTSTAENDE_BEHANDLINGSDAGER
        Fagområde.Tilbakedatering -> ApiFagomrade.TILBAKEDATERING
        Fagområde.Yrkesskade -> ApiFagomrade.YRKESSKADE
        Fagområde.Bestridelse -> ApiFagomrade.BESTRIDELSE
    }

private fun Dialog.tilApiDialogmeldingType(): ApiDialogmeldingType =
    when (this.dialogtype) {
        Dialogtype.Journalnotat -> ApiDialogmeldingType.JOURNALNOTAT
        Dialogtype.MedisinskeOpplysninger -> ApiDialogmeldingType.MEDISINSKE_OPPLYSNINGER
        Dialogtype.EkstraUttalelserFraLege -> ApiDialogmeldingType.EKSTRA_UTTALELSER_FRA_LEGE
        Dialogtype.SpesialistErklæring -> ApiDialogmeldingType.SPESIALISTERKLAERING
        Dialogtype.UtvidetSpesialistErklæring -> ApiDialogmeldingType.UTVIDET_SPESIALISTERKLAERING
    }

private fun Behandler.tilApiBehandler(
    behandlerRef: BehandlerRef,
): ApiBehandler =
    ApiBehandler(
        id = behandlerRef.value,
        navn =
            ApiNavn(
                fornavn = navn.fornavn,
                mellomnavn = navn.mellomnavn,
                etternavn = navn.etternavn,
            ),
        type = null,
        kategori = ApiBehandlerKategori.LEGE,
        legekontor =
            ApiLegekontor(
                kontor = kontor.navn,
                orgnummer = kontor.organisasjonsnummer?.value,
                adresse = kontor.adresse?.veiadresse,
                postnummer = kontor.adresse?.postnummer,
                poststed = kontor.adresse?.poststed,
            ),
        telefonnummer = telefonnummer?.value,
    )

private fun Navn.tilApiNavn(): ApiNavn =
    ApiNavn(
        fornavn = this.fornavn,
        mellomnavn = this.mellomnavn,
        etternavn = this.etternavn,
    )
