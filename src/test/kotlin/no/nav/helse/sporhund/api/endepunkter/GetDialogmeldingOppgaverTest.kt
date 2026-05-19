package no.nav.helse.sporhund.api.endepunkter

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.helse.sporhund.api.ApiDialogmeldingOppgave
import no.nav.helse.sporhund.api.testhelpers.FakePersonPseudoIdProvider
import no.nav.helse.sporhund.api.testhelpers.FakeTransactionProvider
import no.nav.helse.sporhund.api.testhelpers.jsonClient
import no.nav.helse.sporhund.api.testhelpers.setupTestApp
import no.nav.helse.sporhund.api.testhelpers.utstedToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GetDialogmeldingOppgaverTest : EndepunktTest() {
    @Test
    fun `returnerer 200 med liste`() =
        testApplication {
            setupTestApp(FakePersonPseudoIdProvider(), FakeTransactionProvider(), mockOAuth2Server)

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val oppgaver = response.body<List<ApiDialogmeldingOppgave>>()
            assertNotNull(oppgaver)
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApplication {
            setupTestApp(FakePersonPseudoIdProvider(), FakeTransactionProvider(), mockOAuth2Server)

            val response = client.get("/api/dialogmelding-oppgaver")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
