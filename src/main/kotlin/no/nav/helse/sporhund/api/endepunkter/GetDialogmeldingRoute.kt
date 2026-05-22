package no.nav.helse.sporhund.api.endepunkter

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.helse.sporhund.api.ApiDialogDetails
import no.nav.helse.sporhund.api.accessToken
import no.nav.helse.sporhund.api.conversationRef
import no.nav.helse.sporhund.api.mapping.tilApiDialogDetails
import no.nav.helse.sporhund.api.personPseudoId
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider

fun Route.getDialogmeldingRoute(
    personPseudoIdProvider: PersonPseudoIdProvider,
    transactionProvider: TransactionProvider,
    populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
) {
    get("/personer/{pseudoId}/dialogmeldinger/{conversationRef}", {
        operationId = "getDialogmelding"
        description = "Hent en enkelt dialog med alle meldinger"
        request {
            pathParameter<String>("pseudoId") {
                description = "Pseudonymisert person-ID"
                required = true
            }
            pathParameter<String>("conversationRef") {
                description = "ID til dialogen"
                required = true
            }
        }
        response {
            HttpStatusCode.OK to {
                description = "Full dialog med alle meldinger"
                body<ApiDialogDetails>()
            }
            HttpStatusCode.NotFound to {
                description = "Dialog ikke funnet"
            }
        }
    }) {
        val pseudoId = call.personPseudoId()
        val identitetsnummer = personPseudoIdProvider.hentIdentitetsnummer(pseudoId)
        if (identitetsnummer == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        val result = populasjonstilgangskontrollProvider.kontrollerTilgang(call.accessToken(), identitetsnummer.value)

        when (result) {
            TilgangskontrollResultat.IdentIkkeFunnet -> return@get call.respond(HttpStatusCode.NotFound)
            is TilgangskontrollResultat.ManglerTilgang -> return@get call.respond(HttpStatusCode.Forbidden)
            TilgangskontrollResultat.Ok -> Unit
            is TilgangskontrollResultat.UventetFeil -> return@get call.respond(HttpStatusCode.InternalServerError)
        }

        val conversationRef = call.conversationRef()
        val dialog =
            transactionProvider.transaction {
                dialogRepository.finnDialog(conversationRef)
            }
        if (dialog != null) {
            call.respond(dialog.tilApiDialogDetails())
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}
