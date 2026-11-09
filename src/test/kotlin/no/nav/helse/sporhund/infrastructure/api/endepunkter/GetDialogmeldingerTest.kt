package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.infrastructure.api.ApiDialogOppsummering
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDialogmeldingerTest : EndepunktTest() {
    @Test
    fun `returnerer 200 med dialoger for kjent pseudoId`() =
        testApp {
            val dialog = lagDialog()
            val pseudoId = lagreDialog(dialog)

            val client = jsonClient()

            val response = client.get("/api/personer/${pseudoId.value}/dialogmeldinger") { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.OK, response.status)
            val dialoger = response.body<List<ApiDialogOppsummering>>()
            assertEquals(1, dialoger.size)
            assertEquals(dialog.conversationRef.value, dialoger.first().conversationRef)
        }

    @Test
    fun `returnerer 404 for ukjent pseudoId`() =
        testApp {
            val client = jsonClient()

            val response = client.get("/api/personer/${UUID.randomUUID()}/dialogmeldinger") { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApp {
            val response = client.get("/api/personer/${UUID.randomUUID()}/dialogmeldinger")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
