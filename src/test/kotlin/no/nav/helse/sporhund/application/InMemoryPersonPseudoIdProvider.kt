package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.Identitetsnummer
import java.util.UUID

class InMemoryPersonPseudoIdProvider : PersonPseudoIdProvider {
    private val store = mutableMapOf<PersonPseudoId, Identitetsnummer>()

    override fun nyPersonPseudoId(identitetsnummer: Identitetsnummer): PersonPseudoId {
        val pseudoId = PersonPseudoId(UUID.randomUUID())
        store[pseudoId] = identitetsnummer
        return pseudoId
    }

    override fun hentIdentitetsnummer(personPseudoId: PersonPseudoId): Identitetsnummer? = store[personPseudoId]
}
