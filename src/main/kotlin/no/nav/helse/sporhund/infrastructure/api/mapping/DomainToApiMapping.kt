package no.nav.helse.sporhund.infrastructure.api.mapping

import no.nav.helse.sporhund.application.PersonPseudoId
import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.BehandlerRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.domain.Dialogtype
import no.nav.helse.sporhund.domain.Fagområde
import no.nav.helse.sporhund.domain.Navn
import no.nav.helse.sporhund.infrastructure.api.ApiBehandler
import no.nav.helse.sporhund.infrastructure.api.ApiBehandlerKategori
import no.nav.helse.sporhund.infrastructure.api.ApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.ApiDialogOppsummering
import no.nav.helse.sporhund.infrastructure.api.ApiDialogmelding
import no.nav.helse.sporhund.infrastructure.api.ApiDialogmeldingOppgave
import no.nav.helse.sporhund.infrastructure.api.ApiDialogmeldingStatus
import no.nav.helse.sporhund.infrastructure.api.ApiDialogmeldingType
import no.nav.helse.sporhund.infrastructure.api.ApiFagomrade
import no.nav.helse.sporhund.infrastructure.api.ApiLegekontor
import no.nav.helse.sporhund.infrastructure.api.ApiNavn

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
                    avsender =
                        when (dialogmelding) {
                            is Dialogmelding.FraBehandler -> ApiDialogmelding.Avsender.BEHANDLER
                            is Dialogmelding.FraNav -> ApiDialogmelding.Avsender.NAV
                            is Dialogmelding.FraSystem -> ApiDialogmelding.Avsender.SYSTEM
                        },
                    msgId =
                        when (dialogmelding) {
                            is Dialogmelding.FraBehandler -> dialogmelding.id.value
                            is Dialogmelding.FraNav -> dialogmelding.id.value.toString()
                            is Dialogmelding.FraSystem -> dialogmelding.id.value.toString()
                        },
                    antallVedlegg =
                        when (dialogmelding) {
                            is Dialogmelding.FraBehandler -> dialogmelding.antallVedlegg
                            is Dialogmelding.FraNav -> 0
                            is Dialogmelding.FraSystem -> 0
                        },
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
