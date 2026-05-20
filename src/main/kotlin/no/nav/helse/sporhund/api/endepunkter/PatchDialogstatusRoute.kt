package no.nav.helse.sporhund.api.endepunkter

import io.github.smiley4.ktoropenapi.patch
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.api.ApiDialogDetails
import no.nav.helse.sporhund.api.ApiOppdaterDialogStatus
import no.nav.helse.sporhund.api.mapping.tilApiDialogDetails
import no.nav.helse.sporhund.api.personPseudoId
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.domain.ConversationRef
import java.util.*

fun Route.patchDialogstatusRoute(
    personPseudoIdProvider: PersonPseudoIdProvider,
    transactionProvider: TransactionProvider,
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
        val pseudoId = call.personPseudoId()
        val identitetsnummer = personPseudoIdProvider.hentIdentitetsnummer(pseudoId)
        if (identitetsnummer == null) {
            call.respond(HttpStatusCode.NotFound)
            return@patch
        }

        val statusOppdatering = call.receive<ApiOppdaterDialogStatus>()
        val conversationRef = ConversationRef(UUID.fromString(call.parameters["conversationRef"]!!))
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
