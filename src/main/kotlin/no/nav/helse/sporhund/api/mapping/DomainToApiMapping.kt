package no.nav.helse.sporhund.api.mapping

import no.nav.helse.sporhund.api.*
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding

fun Dialog.tilApiDialogmeldingerOversikt(): ApiDialogOppsummering {
    val forsteFraNav = meldinger.filterIsInstance<Dialogmelding.FraNav>().first()
    val behandler = forsteFraNav.behandler
    val behandlerRef = forsteFraNav.behandlerRef
    return ApiDialogOppsummering(
        id = conversationRef.value.toString(),
        behandler =
            ApiBehandler(
                id = behandlerRef.value,
                navn =
                    ApiBehandlerNavn(
                        fornavn = behandler.navn.fornavn,
                        mellomnavn = behandler.navn.mellomnavn,
                        etternavn = behandler.navn.etternavn,
                    ),
                type = null,
                kategori = ApiBehandlerKategori.LEGE,
                legekontor =
                    ApiLegekontor(
                        kontor = behandler.kontor.navn,
                        orgnummer = behandler.kontor.organisasjonsnummer?.value,
                        adresse = behandler.kontor.adresse?.veiadresse,
                        postnummer = behandler.kontor.adresse?.postnummer,
                        poststed = behandler.kontor.adresse?.poststed,
                    ),
                telefonnummer = behandler.telefonnummer?.value,
            ),
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
        id = conversationRef.value.toString(),
        behandler =
            ApiBehandler(
                id = behandlerRef.value,
                navn =
                    ApiBehandlerNavn(
                        fornavn = behandler.navn.fornavn,
                        mellomnavn = behandler.navn.mellomnavn,
                        etternavn = behandler.navn.etternavn,
                    ),
                type = null,
                kategori = ApiBehandlerKategori.LEGE,
                legekontor =
                    ApiLegekontor(
                        kontor = behandler.kontor.navn,
                        orgnummer = behandler.kontor.organisasjonsnummer?.value,
                        adresse = behandler.kontor.adresse?.veiadresse,
                        postnummer = behandler.kontor.adresse?.postnummer,
                        poststed = behandler.kontor.adresse?.poststed,
                    ),
                telefonnummer = behandler.telefonnummer?.value,
            ),
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
