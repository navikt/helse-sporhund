package no.nav.helse.sporhund.api.endepunkter

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.helse.sporhund.api.ApiDialogmeldingOppgave
import no.nav.helse.sporhund.api.ApiDialogmeldingStatus
import no.nav.helse.sporhund.api.testhelpers.jsonClient
import no.nav.helse.sporhund.api.testhelpers.utstedToken
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.domain.testhelpers.lagBehandler
import no.nav.helse.sporhund.domain.testhelpers.lagBehandlerRef
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.domain.testhelpers.lagNavIdent
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetDialogmeldingOppgaverTest : EndepunktTest() {
    @Test
    fun `returnerer 200 med tom liste når det ikke finnes noen åpne dialoger`() =
        testApplication {
            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.body<List<ApiDialogmeldingOppgave>>().isEmpty())
        }

    @Test
    fun `returnerer åpen dialog som oppgave`() =
        testApplication {
            val dialog =
                Dialog.ny(
                    identitetsnummer = lagIdentitetsnummer(),
                    melding = nyFraNavMelding(),
                )
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val oppgaver = response.body<List<ApiDialogmeldingOppgave>>()
            assertEquals(1, oppgaver.size)
            assertEquals(dialog.conversationRef.value, oppgaver.first().conversationRef)
        }

    @Test
    fun `inkluderer ikke lukket dialog`() =
        testApplication {
            val lukketDialog =
                Dialog.fraLagring(
                    conversationRef = ConversationRef(UUID.randomUUID()),
                    identitetsnummer = lagIdentitetsnummer(),
                    meldinger = listOf(nyFraNavMelding()),
                    status = Dialogstatus.DialogLukket,
                )
            transactionProvider.dialogRepository.lagre(lukketDialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.body<List<ApiDialogmeldingOppgave>>().isEmpty())
        }

    @Test
    fun `returnerer kun ikke-lukkede dialoger blant flere`() =
        testApplication {
            val åpenDialog =
                Dialog.ny(
                    identitetsnummer = lagIdentitetsnummer(),
                    melding = nyFraNavMelding(),
                )
            val lukketDialog =
                Dialog.fraLagring(
                    conversationRef = ConversationRef(UUID.randomUUID()),
                    identitetsnummer = lagIdentitetsnummer(),
                    meldinger = listOf(nyFraNavMelding()),
                    status = Dialogstatus.DialogLukket,
                )
            transactionProvider.dialogRepository.lagre(åpenDialog)
            transactionProvider.dialogRepository.lagre(lukketDialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val oppgaver = response.body<List<ApiDialogmeldingOppgave>>()
            assertEquals(1, oppgaver.size)
            assertEquals(åpenDialog.conversationRef.value, oppgaver.first().conversationRef)
        }

    @Test
    fun `status ForespørselSendt mappes til SENDT`() =
        testApplication {
            val dialog =
                Dialog.ny(
                    identitetsnummer = lagIdentitetsnummer(),
                    melding = nyFraNavMelding(),
                )
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)
            val oppgaver = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }.body<List<ApiDialogmeldingOppgave>>()

            assertEquals(ApiDialogmeldingStatus.SENDT, oppgaver.first().status)
        }

    @Test
    fun `status PurringSendt mappes til PURRING_SENDT`() =
        testApplication {
            val dialog =
                Dialog.fraLagring(
                    conversationRef = ConversationRef(UUID.randomUUID()),
                    identitetsnummer = lagIdentitetsnummer(),
                    meldinger = listOf(nyFraNavMelding()),
                    status = Dialogstatus.PurringSendt,
                )
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)
            val oppgaver = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }.body<List<ApiDialogmeldingOppgave>>()

            assertEquals(ApiDialogmeldingStatus.PURRING_SENDT, oppgaver.first().status)
        }

    @Test
    fun `status SvarMottatt mappes til MOTTATT`() =
        testApplication {
            val dialog =
                Dialog.fraLagring(
                    conversationRef = ConversationRef(UUID.randomUUID()),
                    identitetsnummer = lagIdentitetsnummer(),
                    meldinger = listOf(nyFraNavMelding()),
                    status = Dialogstatus.SvarMottatt,
                )
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)
            val oppgaver = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }.body<List<ApiDialogmeldingOppgave>>()

            assertEquals(ApiDialogmeldingStatus.MOTTATT, oppgaver.first().status)
        }

    @Test
    fun `oppgave har satt personPseudoId og fristTidspunkt er etter sisteAktivitetTidspunkt`() =
        testApplication {
            val dialog =
                Dialog.ny(
                    identitetsnummer = lagIdentitetsnummer(),
                    melding = nyFraNavMelding(),
                )
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)
            val oppgaver = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }.body<List<ApiDialogmeldingOppgave>>()

            val oppgave = oppgaver.first()
            assertEquals(dialog.conversationRef.value, oppgave.conversationRef)
            assertTrue(oppgave.fristTidspunkt > oppgave.sisteAktivitetTidspunkt)
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApplication {
            setupDefaultTestApp()

            val response = client.get("/api/dialogmelding-oppgaver")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    private fun nyFraNavMelding(): Dialogmelding.FraNav =
        Dialogmelding.FraNav.fraLagring(
            id = DialogmeldingId(UUID.randomUUID()),
            tidspunkt = Instant.now(),
            melding = "En melding til behandler",
            saksbehandler = lagNavIdent(),
            behandler = lagBehandler(),
            behandlerRef = lagBehandlerRef(),
        )
}
