package no.nav.helse.sporhund.api

import no.nav.helse.sporhund.domain.Adresse
import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.HprNummer
import no.nav.helse.sporhund.domain.Kontor
import no.nav.helse.sporhund.domain.Navn
import no.nav.helse.sporhund.domain.Organisasjonsnummer
import no.nav.helse.sporhund.domain.Telefonnummer

fun ApiNyDialogmelding.tilBehandler(): Behandler {
    val legekontor = this.behandler.legekontor
    return Behandler(
        HprNummer(behandler.id),
        navn =
            Navn(
                fornavn = this.behandler.navn.fornavn,
                mellomnavn = this.behandler.navn.mellomnavn,
                etternavn = this.behandler.navn.etternavn,
            ),
        kontor =
            Kontor(
                navn = legekontor.kontor,
                organisasjonsnummer = legekontor.orgnummer?.let { Organisasjonsnummer(it) },
                adresse =
                    legekontor.adresse?.let {
                        Adresse(
                            veiadresse = it,
                            postnummer = requireNotNull(legekontor.postnummer),
                            poststed = requireNotNull(legekontor.poststed),
                        )
                    },
            ),
        telefonnummer = behandler.telefonnummer?.let { Telefonnummer(it) },
    )
}
