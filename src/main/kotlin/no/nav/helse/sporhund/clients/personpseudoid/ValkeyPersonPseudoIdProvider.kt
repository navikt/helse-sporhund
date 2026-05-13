package no.nav.helse.sporhund.clients.personpseudoid

import com.github.navikt.tbd_libs.personpseudoid.PersonPseudoIdClient
import com.github.navikt.tbd_libs.personpseudoid.ValkeyConfig
import no.nav.helse.sporhund.application.PersonPseudoId
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.domain.Identitetsnummer

class ValkeyPersonPseudoIdProvider(
    config: PersonPseudoIdConfig,
) : PersonPseudoIdProvider {
    private val client = PersonPseudoIdClient(ValkeyConfig(username = config.valkeyBrukernavn, password = config.valkeyPassord, connectionString = config.valkeyConnectionString))

    override fun nyPersonPseudoId(identitetsnummer: Identitetsnummer): PersonPseudoId = PersonPseudoId(client.nyPersonPseudoId(identitetsnummer.value))

    override fun hentIdentitetsnummer(personPseudoId: PersonPseudoId): Identitetsnummer? = client.finnIdentitetsnummer(personPseudoId.value)?.let { Identitetsnummer.fraString(it) }
}
