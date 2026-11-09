package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.sporhund.infrastructure.api.*
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PostNyDialogmeldingTest : EndepunktTest() {
    @Test
    fun `returnerer 201 med ny dialog`() =
        testApp {
            val response = postNyForespørsel(melding = "Jeg trenger informasjon om pasienten")

            assertEquals(HttpStatusCode.Created, response.status)
            val opprettet = response.body<ApiDialogDetails>()
            assertNotNull(opprettet.conversationRef)
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApp {
            val response =
                client.post("/api/personer/${UUID.randomUUID()}/dialogmelding") {
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertIngentingSendt()
        }
}
