package no.nav.helse.sporhund.infrastructure.api.endepunkter

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import io.github.smiley4.ktoropenapi.patch
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.infrastructure.api.ApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.ApiOppdaterDialogStatus
import no.nav.helse.sporhund.infrastructure.api.conversationRef
import no.nav.helse.sporhund.infrastructure.api.mapping.tilApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.medPerson

fun Route.patchDialogstatusRoute(
    personPseudoIdProvider: PersonPseudoIdProvider,
    populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
    transactionProvider: TransactionProvider
) {
    patch("/personer/{pseudoId}/dialogmeldinger/{conversationRef}", {
        operationId = "patchDialogstatus"
        description = "Oppdater status for en eksisterende dialog"
        request {
            pathParameter<String>("pseudoId") {
                description = "Pseudonymisert person-ID"
                required = true
            }
            pathParameter<String>("conversationRef") {
                description = "ID til dialogen"
                required = true
            }
            body<ApiOppdaterDialogStatus>()
        }
        response {
            HttpStatusCode.OK to {
                description = "Dialogstatus oppdatert"
                body<ApiDialogDetails>()
            }
            HttpStatusCode.NotFound to {
                description = "Dialog eller person ikke funnet"
            }
        }
    }) {
        medPerson(personPseudoIdProvider, populasjonstilgangskontrollProvider) { identitetsnummer ->
            val statusOppdatering = call.receive<ApiOppdaterDialogStatus>()
            val conversationRef = call.conversationRef()
            val oppdatertDialog =
                transactionProvider.transaction {
                    val dialog = dialogRepository.finnDialog(conversationRef) ?: return@transaction null
                    if (dialog.identitetsnummer != identitetsnummer) return@transaction null
                    if (statusOppdatering.ferdigstilt) dialog.ferdigstill() else dialog.gjenåpne()
                    dialogRepository.lagre(dialog)
                    dialog
                }

            if (oppdatertDialog != null) {
                call.respond(HttpStatusCode.OK, oppdatertDialog.tilApiDialogDetails())
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
