package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.infrastructure.api.*
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import no.nav.helse.sporhund.infrastructure.api.testhelpers.utstedToken
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PostNyDialogmeldingTest : EndepunktTest() {
    @Test
    fun `returnerer 201 med ny dialog`() =
        testApplication {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val nyDialogmelding =
                ApiNyDialogmelding(
                    sokernavn = ApiNavn(fornavn = "Slapp", mellomnavn = null, etternavn = "Appelsin"),
                    behandler =
                        ApiBehandler(
                            id = "behandler-123",
                            navn = ApiNavn(fornavn = "Ola", mellomnavn = null, etternavn = "Lege"),
                            type = ApiBehandlerType.FASTLEGE,
                            kategori = ApiBehandlerKategori.LEGE,
                            legekontor =
                                ApiLegekontor(
                                    kontor = "Legesenteret",
                                    orgnummer = "912345678",
                                    adresse = "Storgata 1",
                                    postnummer = "0181",
                                    poststed = "Oslo",
                                ),
                            telefonnummer = "22334455",
                        ),
                    fagomrade = ApiFagomrade.TILBAKEDATERING,
                    melding = "Jeg trenger informasjon om pasienten",
                    meldingstype = ApiDialogmeldingType.JOURNALNOTAT,
                )

            val response =
                client.post("/api/personer/${pseudoId.value}/dialogmelding") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(nyDialogmelding)
                }

            assertEquals(HttpStatusCode.Created, response.status)
            val opprettet = response.body<ApiDialogDetails>()
            assertNotNull(opprettet.conversationRef)
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApplication {
            setupDefaultTestApp()

            val response =
                client.post("/api/personer/${UUID.randomUUID()}/dialogmelding") {
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
