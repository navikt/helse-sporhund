package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.infrastructure.api.ApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.ApiSvarPaDialog
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import no.nav.helse.sporhund.infrastructure.api.testhelpers.utstedTokenMedSkrivTilgang
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ForespørselMedVedleggTest : EndepunktTest() {
    @Test
    fun `ny forespørsel sendes på Kafka og journalføres med samme PDF`() =
        testApp {
            val response = postNyForespørsel(melding = "Jeg trenger informasjon om pasienten")
            assertEquals(HttpStatusCode.Created, response.status)
            val dialog = response.body<ApiDialogDetails>()

            kjørKommandoerTilKøenErTom()

            val record = producer.history().single()
            assertEquals(dialog.conversationRef.toString(), record.key())
            val kafkamelding = objectMapper.readTree(record.value())
            assertEquals(1, kafkamelding["dialogmeldingKode"].asInt())
            assertEquals("Jeg trenger informasjon om pasienten", kafkamelding["dialogmeldingTekst"].asString())

            val journalpost = sendtJournalpost()
            assertEquals("Dialogmelding - tilbakedatering", journalpost["tittel"].asString())
            assertEquals(kafkamelding["dialogmeldingUuid"].asString(), journalpost["eksternReferanseId"].asString())

            val vedlegg = Base64.decode(kafkamelding["dialogmeldingVedlegg"].asString())
            assertContentEquals(pdf, vedlegg)
            assertContentEquals(vedlegg, journalpost.fysiskDokument())

            val avsender = pdfInput.single().fra
            assertEquals(saksbehandler.navn, avsender.navn)
            assertEquals(saksbehandler.ident.value, avsender.NAVIdent)
        }

    @Test
    fun `svar i en dialog som finnes sendes på Kafka og journalføres med samme PDF`() =
        testApp {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)
            val dialog = lagDialog(identitetsnummer = identitetsnummer)
            transactionProvider.dialogRepository.lagre(dialog)

            val response =
                jsonClient().post("/api/personer/${pseudoId.value}/dialogmeldinger/${dialog.conversationRef.value}") {
                    bearerAuth(token())
                    contentType(ContentType.Application.Json)
                    setBody(ApiSvarPaDialog(melding = "Dette er et svar"))
                }
            assertEquals(HttpStatusCode.Created, response.status)

            kjørKommandoerTilKøenErTom()

            val record = producer.history().single()
            assertEquals(dialog.conversationRef.value.toString(), record.key())
            val kafkamelding = objectMapper.readTree(record.value())
            assertEquals("Dette er et svar", kafkamelding["dialogmeldingTekst"].asString())
            val vedlegg = Base64.decode(kafkamelding["dialogmeldingVedlegg"].asString())
            assertContentEquals(pdf, vedlegg)
            assertContentEquals(vedlegg, sendtJournalpost().fysiskDokument())
        }

    private fun token() =
        mockOAuth2Server.utstedTokenMedSkrivTilgang(
            saksbehandler,
            tilgangsgrupperTilTilganger,
            tilgangsgrupperTilBrukerroller,
        )
}
