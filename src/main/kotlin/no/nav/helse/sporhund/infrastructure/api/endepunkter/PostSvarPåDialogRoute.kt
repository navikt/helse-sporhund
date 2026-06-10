package no.nav.helse.sporhund.infrastructure.api.endepunkter

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.infrastructure.api.*
import no.nav.helse.sporhund.infrastructure.api.mapping.tilApiDialogDetails

fun Route.postSvarPåDialogRoute(
    personPseudoIdProvider: PersonPseudoIdProvider,
    populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
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
        medPerson(personPseudoIdProvider, populasjonstilgangskontrollProvider) {
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
                    val melding = dialog.nyesteMeldingFraNav()
                    events.forEach {
                        outbox.nyMelding(OutboxMelding.nyDialogmeldingFraNav(it))
                    }
                    outbox.nyMelding(OutboxMelding.opprettUtgåendeJournalpost(melding, dialog, saksbehandler))
                    dialog
                }
            if (oppdatertDialog != null) {
                call.respond(HttpStatusCode.Created, oppdatertDialog.tilApiDialogDetails())
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
