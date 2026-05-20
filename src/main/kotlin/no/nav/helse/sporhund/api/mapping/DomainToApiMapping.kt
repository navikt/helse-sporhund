package no.nav.helse.sporhund.api.mapping

import no.nav.helse.sporhund.api.*
import no.nav.helse.sporhund.application.PersonPseudoId
import no.nav.helse.sporhund.domain.*

fun Dialog.tilApiDialogmeldingerOversikt(): ApiDialogOppsummering {
    val (opprinneligBehandler, opprinneligBehandlerRef) = this.opprinneligBehandler()
    return ApiDialogOppsummering(
        conversationRef = conversationRef.value,
        behandler = opprinneligBehandler.tilApiBehandler(opprinneligBehandlerRef),
        fagomrade = ApiFagomrade.TILBAKEDATERING,
        meldingstype = ApiDialogmeldingType.JOURNALNOTAT,
        opprettetTidspunkt = this.opprettetTidspunkt(),
        antallMeldinger = meldinger.size,
        antallVedlegg = this.antallVedleggTotalt(),
    )
}

fun Dialog.tilApiDialogmeldingOppgave(personPseudoId: PersonPseudoId): ApiDialogmeldingOppgave =
    ApiDialogmeldingOppgave(
        conversationRef = conversationRef.value,
        personPseudoId = personPseudoId.value,
        sisteAktivitetTidspunkt = this.nyesteMelding().tidspunkt,
        fristTidspunkt = this.frist(),
        fagomrade = ApiFagomrade.TILBAKEDATERING,
        soker = identitetsnummer.value, // TODO: Må lagre navn person i Dialog og sende det med her
        meldingstype = ApiDialogmeldingType.JOURNALNOTAT,
        status =
            when (status) {
                Dialogstatus.ForespørselSendt -> ApiDialogmeldingStatus.SENDT
                Dialogstatus.PurringSendt -> ApiDialogmeldingStatus.PURRING_SENDT
                Dialogstatus.SvarMottatt -> ApiDialogmeldingStatus.MOTTATT
                Dialogstatus.DialogLukket -> ApiDialogmeldingStatus.FERDIGSTILT
            },
    )

fun Dialog.tilApiDialogDetails(): ApiDialogDetails {
    val (opprinneligBehandler, opprinneligBehandlerRef) = this.opprinneligBehandler()
    return ApiDialogDetails(
        conversationRef = conversationRef.value,
        behandler =
            opprinneligBehandler.tilApiBehandler(opprinneligBehandlerRef),
        opprettetTidspunkt = this.opprettetTidspunkt(),
        dialogmeldinger =
            meldinger.map { dialogmelding ->
                ApiDialogmelding(
                    fagomrade = ApiFagomrade.TILBAKEDATERING,
                    meldingstype = ApiDialogmeldingType.JOURNALNOTAT,
                    melding = dialogmelding.melding,
                    sendtTidspunkt = dialogmelding.tidspunkt,
                    fraNav = dialogmelding is Dialogmelding.FraNav,
                    vedlegg = emptyList(),
                )
            },
    )
}

private fun Behandler.tilApiBehandler(
    behandlerRef: BehandlerRef,
): ApiBehandler =
    ApiBehandler(
        id = behandlerRef.value,
        navn =
            ApiBehandlerNavn(
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
