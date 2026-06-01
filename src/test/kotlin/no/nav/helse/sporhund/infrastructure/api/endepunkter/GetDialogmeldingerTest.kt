package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.infrastructure.api.ApiDialogOppsummering
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import no.nav.helse.sporhund.infrastructure.api.testhelpers.utstedToken
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDialogmeldingerTest : EndepunktTest() {
    @Test
    fun `returnerer 200 med dialoger for kjent pseudoId`() =
        testApplication {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

            val dialog = lagDialog(identitetsnummer = identitetsnummer)
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response = client.get("/api/personer/${pseudoId.value}/dialogmeldinger") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val dialoger = response.body<List<ApiDialogOppsummering>>()
            assertEquals(1, dialoger.size)
            assertEquals(dialog.conversationRef.value, dialoger.first().conversationRef)
        }

    @Test
    fun `returnerer 404 for ukjent pseudoId`() =
        testApplication {
            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response = client.get("/api/personer/${UUID.randomUUID()}/dialogmeldinger") { bearerAuth(token) }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApplication {
            setupDefaultTestApp()

            val response = client.get("/api/personer/${UUID.randomUUID()}/dialogmeldinger")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
