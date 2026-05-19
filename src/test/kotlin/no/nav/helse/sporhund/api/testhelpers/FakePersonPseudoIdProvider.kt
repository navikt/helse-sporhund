package no.nav.helse.sporhund.api.testhelpers

import no.nav.helse.sporhund.application.PersonPseudoId
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.domain.Identitetsnummer
import java.util.UUID

class FakePersonPseudoIdProvider : PersonPseudoIdProvider {
    private val store = mutableMapOf<PersonPseudoId, Identitetsnummer>()

    fun registrer(
        identitetsnummer: Identitetsnummer,
    ): PersonPseudoId {
        val pseudoId = PersonPseudoId(UUID.randomUUID())
        store[pseudoId] = identitetsnummer
        return pseudoId
    }

    override fun nyPersonPseudoId(identitetsnummer: Identitetsnummer): PersonPseudoId = store.entries.find { it.value == identitetsnummer }?.key ?: registrer(identitetsnummer)

    override fun hentIdentitetsnummer(personPseudoId: PersonPseudoId): Identitetsnummer? = store[personPseudoId]
}
