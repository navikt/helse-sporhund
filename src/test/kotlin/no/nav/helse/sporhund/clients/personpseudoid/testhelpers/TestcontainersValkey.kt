package no.nav.helse.sporhund.clients.personpseudoid.testhelpers

import no.nav.helse.sporhund.clients.personpseudoid.PersonPseudoIdConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class TestcontainersValkey(
    moduleLabel: String,
) {
    private val valkey: GenericContainer<*> =
        GenericContainer(DockerImageName.parse("valkey/valkey:latest"))
            .withReuse(true)
            .withLabel("app", "spesialist")
            .withLabel("module", moduleLabel)
            .withLabel("code-location", javaClass.canonicalName)
            .apply {
                withExposedPorts(6379)
                withCommand("valkey-server", "--requirepass", "password")
                start()
            }

    val personPseudoIdConfig =
        PersonPseudoIdConfig(
            valkeyBrukernavn = "default",
            valkeyPassord = "password",
            valkeyConnectionString = "valkey://${valkey.host}:${valkey.getMappedPort(6379)}",
        )
}
