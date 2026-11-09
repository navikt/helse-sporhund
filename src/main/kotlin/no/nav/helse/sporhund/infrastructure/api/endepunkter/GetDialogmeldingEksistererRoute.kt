package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.infrastructure.api.ApiDialogmeldingEksisterer
import no.nav.helse.sporhund.infrastructure.api.auth.MaskinPrincipal

const val DIALOGMELDING_EKSISTERER_ROLLE = "dialogmelding-eksisterer"

private val gyldigMsgId = Regex("^[A-Za-z0-9._:-]{1,128}$")

fun Route.getDialogmeldingEksistererRoute(transactionProvider: TransactionProvider) {
    get("/syfo/v1/dialogmelding/{msgId}/eksisterer", {
        operationId = "getDialogmeldingEksisterer"
        description =
            "Sjekker om Sporhund har tatt inn et svar fra behandler, altså knyttet det til en dialog. " +
            "true endrer seg aldri. false gjelder bare nå: Sporhund har kanskje ikke lest meldingen fra Kafka ennå, " +
            "så spør etter en forsinkelse eller på nytt før du ruter meldingen selv. " +
            "Ved 5xx skal du prøve på nytt og ikke tolke feilen som false. " +
            "Krever et maskintoken (client_credentials) med rollen $DIALOGMELDING_EKSISTERER_ROLLE."
        request {
            pathParameter<String>("msgId") {
                description = "msgId på meldingen fra behandler. 1–128 tegn: bokstaver a–z og A–Z, sifre, punktum, kolon, understrek og bindestrek."
                required = true
            }
        }
        response {
            HttpStatusCode.OK to {
                description = "Om svaret er tatt inn"
                body<ApiDialogmeldingEksisterer>()
            }
            HttpStatusCode.BadRequest to { description = "Ugyldig msgId" }
            HttpStatusCode.Unauthorized to { description = "Mangler gyldig maskintoken fra en forhåndsautorisert app" }
            HttpStatusCode.Forbidden to { description = "Tokenet mangler rollen $DIALOGMELDING_EKSISTERER_ROLLE" }
            HttpStatusCode.InternalServerError to { description = "Sporhund kunne ikke svare. Prøv på nytt." }
        }
    }) {
        val principal = call.principal<MaskinPrincipal>()
        if (principal == null || DIALOGMELDING_EKSISTERER_ROLLE !in principal.roller) {
            return@get call.respond(HttpStatusCode.Forbidden)
        }
        val msgId = call.parameters["msgId"]
        if (msgId == null || !gyldigMsgId.matches(msgId)) {
            return@get call.respond(HttpStatusCode.BadRequest)
        }
        val eksisterer = transactionProvider.transaction { dialogRepository.svarErTattInn(msgId) }
        loggInfo("Svarte på om svar fra behandler er tatt inn", "msgId" to msgId, "eksisterer" to eksisterer, "azp" to principal.klientId)
        call.respond(ApiDialogmeldingEksisterer(eksisterer = eksisterer))
    }
}
