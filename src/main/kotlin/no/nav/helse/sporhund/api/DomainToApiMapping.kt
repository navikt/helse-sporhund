package no.nav.helse.sporhund.api

import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding

fun Dialog.tilApiDialogmeldingerOversikt(): ApiDialogOppsummering {
    val forsteFraNav = meldinger.filterIsInstance<Dialogmelding.FraNav>().firstOrNull()
    val behandler = forsteFraNav?.behandler
    val behandlerRef = forsteFraNav?.behandlerRef
    val navnDeler = behandler?.navn?.split(" ") ?: emptyList()
    return ApiDialogOppsummering(
        id = conversationRef.value.toString(),
        behandler =
            ApiBehandler(
                id = behandlerRef?.value ?: "",
                navn =
                    ApiBehandlerNavn(
                        fornavn = navnDeler.firstOrNull() ?: "",
                        mellomnavn = if (navnDeler.size > 2) navnDeler.drop(1).dropLast(1).joinToString(" ") else null,
                        etternavn = if (navnDeler.size > 1) navnDeler.last() else "",
                    ),
                type = null,
                kategori = ApiBehandlerKategori.LEGE,
                legekontor =
                    ApiLegekontor(
                        kontor = behandler?.kontor,
                        orgnummer = behandler?.kontorOrganisasjonsnummer?.value,
                        adresse = null,
                        postnummer = null,
                        poststed = null,
                    ),
                telefonnummer = behandler?.telefonnummer?.value,
            ),
        tittel = meldinger.firstOrNull()?.melding?.take(60) ?: "Tittel", // Venter på liste under fagområdekategoriene for å lage tittel
        tid = meldinger.firstOrNull()?.tidspunkt?.toString() ?: "",
        antallMeldinger = meldinger.size,
        antallVedlegg = meldinger.filterIsInstance<Dialogmelding.FraBehandler>().sumOf { it.antallVedlegg },
    )
}
