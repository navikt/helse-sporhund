package no.nav.helse.sporhund.infrastructure.api.mapping

import no.nav.helse.sporhund.application.PersonPseudoId
import no.nav.helse.sporhund.domain.*
import no.nav.helse.sporhund.infrastructure.api.*

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
                dialogmelding.tilApiDialogmelding(tilApiFagomrade(), tilApiDialogmeldingType())
            },
    )
}

private fun Dialogmelding<*>.tilApiDialogmelding(
    fagomrade: ApiFagomrade,
    meldingstype: ApiDialogmeldingType,
): ApiDialogmelding =
    when (this) {
        is Dialogmelding.FraBehandler ->
            ApiDialogmelding.FraBehandler(
                fagomrade = fagomrade,
                meldingstype = meldingstype,
                melding = this.melding,
                msgId = this.id.value,
                sendtTidspunkt = this.tidspunkt,
                antallVedlegg = this.antallVedlegg,
            )

        is Dialogmelding.FraNav ->
            ApiDialogmelding.FraNav(
                fagomrade = fagomrade,
                meldingstype = meldingstype,
                melding = this.melding,
                msgId = this.id.value.toString(),
                sendtTidspunkt = this.tidspunkt,
                saksbehandler = this.saksbehandler.value,
            )

        is Dialogmelding.FraSystem ->
            ApiDialogmelding.FraSystem(
                fagomrade = fagomrade,
                meldingstype = meldingstype,
                melding = this.melding,
                msgId = this.id.value.toString(),
                sendtTidspunkt = this.tidspunkt,
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
        hprNummer = this.hprNummer.value,
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
