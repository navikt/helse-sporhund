package no.nav.helse.sporhund.infrastructure.api.mapping

import no.nav.helse.sporhund.domain.*
import no.nav.helse.sporhund.infrastructure.api.ApiNyDialogmelding

fun ApiNyDialogmelding.tilBehandler(): Behandler {
    val legekontor = this.behandler.legekontor
    return Behandler(
        HprNummer(behandler.id),
        navn =
            Navn(
                fornavn = this.behandler.navn.fornavn,
                mellomnavn = this.behandler.navn.mellomnavn,
                etternavn = this.behandler.navn.etternavn
            ),
        kontor =
            Kontor(
                navn = legekontor.kontor,
                organisasjonsnummer = legekontor.orgnummer?.let { Organisasjonsnummer(it) },
                adresse =
                    legekontor.adresse?.let {
                        Adresse(
                            veiadresse = it,
                            postnummer = legekontor.postnummer,
                            poststed = legekontor.poststed
                        )
                    }
            ),
        telefonnummer = behandler.telefonnummer?.let { Telefonnummer(it) }
    )
}
