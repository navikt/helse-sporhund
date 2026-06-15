package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.sporhund.application.VedleggProvider
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagFraBehandlerMelding
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import no.nav.helse.sporhund.infrastructure.api.testhelpers.utstedTokenMedLesTilgang
import java.util.*
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class GetVedleggTest : EndepunktTest() {
    @Test
    fun `returnerer 200 med vedlegg for kjent msgId og gyldig index`() =
        testApplication {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

            val msgId = UUID.randomUUID()
            val vedleggInnhold = "vedlegg-innhold".toByteArray()
            val fraBehandlerMelding = lagFraBehandlerMelding(msgId = msgId, antallVedlegg = 1)
            val dialog = lagDialog(identitetsnummer = identitetsnummer)
            dialog.nyMelding(fraBehandlerMelding)
            transactionProvider.dialogRepository.lagre(dialog)

            vedleggProvider = VedleggProvider { listOf(vedleggInnhold) }

            setupDefaultTestApp()

            val client = jsonClient()
            val token =
                mockOAuth2Server.utstedTokenMedLesTilgang(
                    saksbehandler,
                    tilgangsgrupperTilTilganger,
                    tilgangsgrupperTilBrukerroller,
                )

            val response =
                client.get(
                    "/api/personer/${pseudoId.value}/vedlegg/$msgId/0",
                ) { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertContentEquals(vedleggInnhold, response.bodyAsBytes())
        }

    @Test
    fun `returnerer 200 med riktig vedlegg for index 1`() =
        testApplication {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

            val msgId = UUID.randomUUID()
            val vedlegg0 = "første vedlegg".toByteArray()
            val vedlegg1 = "andre vedlegg".toByteArray()
            val fraBehandlerMelding = lagFraBehandlerMelding(msgId = msgId, antallVedlegg = 2)
            val dialog = lagDialog(identitetsnummer = identitetsnummer)
            dialog.nyMelding(fraBehandlerMelding)
            transactionProvider.dialogRepository.lagre(dialog)

            vedleggProvider = VedleggProvider { listOf(vedlegg0, vedlegg1) }

            setupDefaultTestApp()

            val client = jsonClient()
            val token =
                mockOAuth2Server.utstedTokenMedLesTilgang(
                    saksbehandler,
                    tilgangsgrupperTilTilganger,
                    tilgangsgrupperTilBrukerroller,
                )

            val response =
                client.get(
                    "/api/personer/${pseudoId.value}/vedlegg/$msgId/1",
                ) { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertContentEquals(vedlegg1, response.bodyAsBytes())
        }

    @Test
    fun `returnerer 200 med riktig vedlegg for index 3 av flere vedlegg`() =
        testApplication {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

            val msgId = UUID.randomUUID()
            val vedleggListe =
                listOf(
                    "vedlegg 0".toByteArray(),
                    "vedlegg 1".toByteArray(),
                    "vedlegg 2".toByteArray(),
                    "vedlegg 3".toByteArray(),
                    "vedlegg 4".toByteArray(),
                )
            val fraBehandlerMelding = lagFraBehandlerMelding(msgId = msgId, antallVedlegg = vedleggListe.size)
            val dialog = lagDialog(identitetsnummer = identitetsnummer)
            dialog.nyMelding(fraBehandlerMelding)
            transactionProvider.dialogRepository.lagre(dialog)

            vedleggProvider = VedleggProvider { vedleggListe }

            setupDefaultTestApp()

            val client = jsonClient()
            val token =
                mockOAuth2Server.utstedTokenMedLesTilgang(
                    saksbehandler,
                    tilgangsgrupperTilTilganger,
                    tilgangsgrupperTilBrukerroller,
                )

            val response =
                client.get(
                    "/api/personer/${pseudoId.value}/vedlegg/$msgId/3",
                ) { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertContentEquals("vedlegg 3".toByteArray(), response.bodyAsBytes())
        }

    @Test
    fun `returnerer 404 for index utenfor grensene`() =
        testApplication {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

            val msgId = UUID.randomUUID()
            val fraBehandlerMelding = lagFraBehandlerMelding(msgId = msgId, antallVedlegg = 1)
            val dialog = lagDialog(identitetsnummer = identitetsnummer)
            dialog.nyMelding(fraBehandlerMelding)
            transactionProvider.dialogRepository.lagre(dialog)

            vedleggProvider = VedleggProvider { listOf("innhold".toByteArray()) }

            setupDefaultTestApp()

            val client = jsonClient()
            val token =
                mockOAuth2Server.utstedTokenMedLesTilgang(
                    saksbehandler,
                    tilgangsgrupperTilTilganger,
                    tilgangsgrupperTilBrukerroller,
                )

            val response =
                client.get(
                    "/api/personer/${pseudoId.value}/vedlegg/$msgId/5",
                ) { bearerAuth(token) }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `returnerer 404 for ukjent pseudoId`() =
        testApplication {
            setupDefaultTestApp()

            val client = jsonClient()
            val token =
                mockOAuth2Server.utstedTokenMedLesTilgang(
                    saksbehandler,
                    tilgangsgrupperTilTilganger,
                    tilgangsgrupperTilBrukerroller,
                )

            val response =
                client.get(
                    "/api/personer/${UUID.randomUUID()}/vedlegg/${UUID.randomUUID()}/0",
                ) { bearerAuth(token) }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `returnerer 404 for ukjent msgId`() =
        testApplication {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

            val dialog = lagDialog(identitetsnummer = identitetsnummer)
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token =
                mockOAuth2Server.utstedTokenMedLesTilgang(
                    saksbehandler,
                    tilgangsgrupperTilTilganger,
                    tilgangsgrupperTilBrukerroller,
                )

            val response =
                client.get(
                    "/api/personer/${pseudoId.value}/vedlegg/${UUID.randomUUID()}/0",
                ) { bearerAuth(token) }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApplication {
            setupDefaultTestApp()

            val response =
                client.get(
                    "/api/personer/${UUID.randomUUID()}/vedlegg/${UUID.randomUUID()}/0",
                )

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
