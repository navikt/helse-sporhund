package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.sporhund.infrastructure.api.testhelpers.setupTestApp
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiTest : EndepunktTest() {
    @Test
    fun `eksponerer openapi og swagger når det er aktivert`() =
        testApplication {
            setupOpenApiTestApp(openApiAktivert = true)

            assertEquals(HttpStatusCode.OK, client.get("/api/openapi.json").status)
            assertEquals(HttpStatusCode.OK, client.get("/api/swagger/index.html").status)
        }

    @Test
    fun `eksponerer verken openapi eller swagger når det ikke er aktivert`() =
        testApplication {
            setupOpenApiTestApp(openApiAktivert = false)

            assertEquals(HttpStatusCode.NotFound, client.get("/api/openapi.json").status)
            assertEquals(HttpStatusCode.NotFound, client.get("/api/swagger/index.html").status)
        }

    private fun ApplicationTestBuilder.setupOpenApiTestApp(openApiAktivert: Boolean) =
        setupTestApp(
            personPseudoIdProvider,
            transactionProvider,
            mockOAuth2Server,
            populasjonstilgangskontrollProvider,
            tilgangsgrupperTilTilganger,
            tilgangsgrupperTilBrukerroller,
            openApiAktivert = openApiAktivert,
        )
}
