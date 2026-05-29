package no.nav.helse.sporhund.application

import java.util.UUID
import no.nav.helse.sporhund.domain.Identitetsnummer

class InMemoryPersonPseudoIdProvider : PersonPseudoIdProvider {
    private val store = mutableMapOf<PersonPseudoId, Identitetsnummer>()

    override fun nyPersonPseudoId(identitetsnummer: Identitetsnummer): PersonPseudoId {
        val pseudoId = PersonPseudoId(UUID.randomUUID())
        store[pseudoId] = identitetsnummer
        return pseudoId
    }

    override fun hentIdentitetsnummer(personPseudoId: PersonPseudoId): Identitetsnummer? = store[personPseudoId]
}
