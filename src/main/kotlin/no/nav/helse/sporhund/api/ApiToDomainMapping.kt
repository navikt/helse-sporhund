package no.nav.helse.sporhund.api

import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.HprNummer
import no.nav.helse.sporhund.domain.Organisasjonsnummer
import no.nav.helse.sporhund.domain.Telefonnummer

fun ApiNyDialogmelding.tilBehandler(): Behandler {
    val behandler =
        Behandler(
            HprNummer(this.behandler.id),
            navn = this.behandler.navn.fornavn + " " + this.behandler.navn.mellomnavn + " " + this.behandler.navn.etternavn, // TODO: håndtere at mellomnavn kan være null
            kontor = this.behandler.legekontor.kontor!!, // TODO: Burde forvente at denne ikke kan være null fra Speil
            kontorOrganisasjonsnummer = Organisasjonsnummer(this.behandler.legekontor.orgnummer!!), // TODO: Burde forvente at denne ikke kan være null fra Speil
            telefonnummer =
                if (this.behandler.telefonnummer != null) {
                    Telefonnummer(
                        this.behandler.telefonnummer,
                    )
                } else {
                    null
                },
        )
    return behandler
}
