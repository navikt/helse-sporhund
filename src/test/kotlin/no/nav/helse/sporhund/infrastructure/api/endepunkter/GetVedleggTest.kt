package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.sporhund.application.VedleggProvider
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagFraBehandlerMelding
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import java.util.*
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class GetVedleggTest : EndepunktTest() {
    @Test
    fun `returnerer 200 med vedlegg for kjent msgId og gyldig index`() =
        testApp {
            val msgId = UUID.randomUUID()
            val vedleggInnhold = "vedlegg-innhold".toByteArray()
            val fraBehandlerMelding = lagFraBehandlerMelding(msgId = msgId, antallVedlegg = 1)
            val dialog = lagDialog()
            dialog.nyMelding(fraBehandlerMelding)
            val pseudoId = lagreDialog(dialog)

            vedleggProvider = VedleggProvider { listOf(vedleggInnhold) }

            val client = jsonClient()

            val response =
                client.get(
                    "/api/personer/${pseudoId.value}/vedlegg/$msgId/0",
                ) { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertContentEquals(vedleggInnhold, response.bodyAsBytes())
        }

    @Test
    fun `returnerer 200 med riktig vedlegg for index 1`() =
        testApp {
            val msgId = UUID.randomUUID()
            val vedlegg0 = "første vedlegg".toByteArray()
            val vedlegg1 = "andre vedlegg".toByteArray()
            val fraBehandlerMelding = lagFraBehandlerMelding(msgId = msgId, antallVedlegg = 2)
            val dialog = lagDialog()
            dialog.nyMelding(fraBehandlerMelding)
            val pseudoId = lagreDialog(dialog)

            vedleggProvider = VedleggProvider { listOf(vedlegg0, vedlegg1) }

            val client = jsonClient()

            val response =
                client.get(
                    "/api/personer/${pseudoId.value}/vedlegg/$msgId/1",
                ) { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertContentEquals(vedlegg1, response.bodyAsBytes())
        }

    @Test
    fun `returnerer 200 med riktig vedlegg for index 3 av flere vedlegg`() =
        testApp {
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
            val dialog = lagDialog()
            dialog.nyMelding(fraBehandlerMelding)
            val pseudoId = lagreDialog(dialog)

            vedleggProvider = VedleggProvider { vedleggListe }

            val client = jsonClient()

            val response =
                client.get(
                    "/api/personer/${pseudoId.value}/vedlegg/$msgId/3",
                ) { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertContentEquals("vedlegg 3".toByteArray(), response.bodyAsBytes())
        }

    @Test
    fun `returnerer 404 for index utenfor grensene`() =
        testApp {
            val msgId = UUID.randomUUID()
            val fraBehandlerMelding = lagFraBehandlerMelding(msgId = msgId, antallVedlegg = 1)
            val dialog = lagDialog()
            dialog.nyMelding(fraBehandlerMelding)
            val pseudoId = lagreDialog(dialog)

            vedleggProvider = VedleggProvider { listOf("innhold".toByteArray()) }

            val client = jsonClient()

            val response =
                client.get(
                    "/api/personer/${pseudoId.value}/vedlegg/$msgId/5",
                ) { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `returnerer 404 for ukjent pseudoId`() =
        testApp {
            val client = jsonClient()

            val response =
                client.get(
                    "/api/personer/${UUID.randomUUID()}/vedlegg/${UUID.randomUUID()}/0",
                ) { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `returnerer 404 for ukjent msgId`() =
        testApp {
            val dialog = lagDialog()
            val pseudoId = lagreDialog(dialog)

            val client = jsonClient()

            val response =
                client.get(
                    "/api/personer/${pseudoId.value}/vedlegg/${UUID.randomUUID()}/0",
                ) { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApp {
            val response =
                client.get(
                    "/api/personer/${UUID.randomUUID()}/vedlegg/${UUID.randomUUID()}/0",
                )

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
