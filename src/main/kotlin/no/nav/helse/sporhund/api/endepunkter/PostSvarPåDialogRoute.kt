package no.nav.helse.sporhund.api.endepunkter

import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.api.ApiDialogDetails
import no.nav.helse.sporhund.api.ApiSvarPaDialog
import no.nav.helse.sporhund.api.conversationRef
import no.nav.helse.sporhund.api.mapping.tilApiDialogDetails
import no.nav.helse.sporhund.api.personPseudoId
import no.nav.helse.sporhund.api.saksbehandler
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.OutboxMeldingId
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.domain.Dialogmelding
import java.util.*

fun Route.postSvarPåDialogRoute(
    personPseudoIdProvider: PersonPseudoIdProvider,
    transactionProvider: TransactionProvider,
) {
    post("/personer/{pseudoId}/dialogmeldinger/{conversationRef}", {
        operationId = "postSvarPaDialog"
        description = "Svar på en eksisterende dialog"
        request {
            pathParameter<String>("pseudoId") {
                description = "Pseudonymisert person-ID"
                required = true
            }
            pathParameter<String>("conversationRef") {
                description = "ID til dialogen"
                required = true
            }
            body<ApiSvarPaDialog>()
        }
        response {
            HttpStatusCode.Created to {
                description = "Svar lagt til i dialogen"
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
            return@post
        }
        val saksbehandler = call.saksbehandler()
        val conversationRef = call.conversationRef()
        val svar = call.receive<ApiSvarPaDialog>()
        val oppdatertDialog =
            transactionProvider.transaction {
                val dialog =
                    dialogRepository.finnDialog(conversationRef)
                        ?: return@transaction null
                val nyesteFraNav = dialog.nyesteMeldingFraNav()
                dialog.nyMelding(
                    Dialogmelding.FraNav.ny(
                        saksbehandler.ident,
                        nyesteFraNav.behandler,
                        nyesteFraNav.behandlerRef,
                        svar.melding,
                    ),
                )
                dialogRepository.lagre(dialog)
                val events = dialog.events()
                events.forEach {
                    outbox.nyMelding(OutboxMelding(OutboxMeldingId(UUID.randomUUID()), it))
                }
                dialog
            }
        if (oppdatertDialog != null) {
            call.respond(HttpStatusCode.Created, oppdatertDialog.tilApiDialogDetails())
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}
