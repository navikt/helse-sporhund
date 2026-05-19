package no.nav.helse.sporhund.api.endepunkter

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.api.ApiDialogDetails
import no.nav.helse.sporhund.api.mapping.tilApiDialogDetails
import no.nav.helse.sporhund.api.personPseudoId
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.clients.personpseudoid.ValkeyPersonPseudoIdProvider
import no.nav.helse.sporhund.domain.ConversationRef
import java.util.*

fun Route.getDialogmeldingRoute(
    personPseudoIdProvider: ValkeyPersonPseudoIdProvider,
    transactionProvider: TransactionProvider,
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
        val conversationRef = call.parameters["conversationRef"]!!
        val dialog =
            transactionProvider.transaction {
                dialogRepository.finnDialog(ConversationRef(UUID.fromString(conversationRef)))
            }
        if (dialog != null) {
            call.respond(dialog.tilApiDialogDetails())
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}
