package no.nav.helse.sporhund.api.mapping

import no.nav.helse.sporhund.api.*
import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.BehandlerRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding

fun Dialog.tilApiDialogmeldingerOversikt(): ApiDialogOppsummering {
    val forsteFraNav = meldinger.filterIsInstance<Dialogmelding.FraNav>().first()
    val behandler = forsteFraNav.behandler
    val behandlerRef = forsteFraNav.behandlerRef
    return ApiDialogOppsummering(
        conversationRef = conversationRef.value,
        behandler =
            behandler.tilApiBehandler(behandlerRef),
        fagomrade = ApiFagomrade.TILBAKEDATERING,
        meldingstype = ApiDialogmeldingType.TILLEGGSOPPLYSNINGER,
        tid = meldinger.firstOrNull()?.tidspunkt?.toString() ?: "",
        antallMeldinger = meldinger.size,
        antallVedlegg = meldinger.filterIsInstance<Dialogmelding.FraBehandler>().sumOf { it.antallVedlegg },
    )
}

fun Dialog.tilApiDialogDetails(): ApiDialogDetails {
    val forsteFraNav = meldinger.filterIsInstance<Dialogmelding.FraNav>().first()
    val behandler = forsteFraNav.behandler
    val behandlerRef = forsteFraNav.behandlerRef
    return ApiDialogDetails(
        conversationRef = conversationRef.value,
        behandler =
            behandler.tilApiBehandler(behandlerRef),
        tid = meldinger.firstOrNull()?.tidspunkt?.toString() ?: "",
        dialogmeldinger =
            meldinger.map { dialogmelding ->
                ApiDialogmelding(
                    fagomrade = ApiFagomrade.TILBAKEDATERING,
                    meldingstype = ApiDialogmeldingType.TILLEGGSOPPLYSNINGER,
                    melding = dialogmelding.melding,
                    tid = dialogmelding.tidspunkt.toString(),
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
