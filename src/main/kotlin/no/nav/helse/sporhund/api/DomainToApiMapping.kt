package no.nav.helse.sporhund.api

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
        tittel = meldinger.firstOrNull()?.melding?.take(60) ?: "Tittel", // Venter på liste under fagområdekategoriene for å lage tittel
        tid = meldinger.firstOrNull()?.tidspunkt?.toString() ?: "",
        antallMeldinger = meldinger.size,
        antallVedlegg = meldinger.filterIsInstance<Dialogmelding.FraBehandler>().sumOf { it.antallVedlegg },
    )
}
