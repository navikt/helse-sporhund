package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.Identitetsnummer

interface PersonPseudoIdProvider {
    fun nyPersonPseudoId(identitetsnummer: Identitetsnummer): PersonPseudoId

    fun hentIdentitetsnummer(personPseudoId: PersonPseudoId): Identitetsnummer?
}
