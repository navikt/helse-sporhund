package no.nav.helse.sporhund.api

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.OutboxMeldingId
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.clients.personpseudoid.ValkeyPersonPseudoIdProvider
import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.BehandlerRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.HprNummer
import no.nav.helse.sporhund.domain.Organisasjonsnummer
import java.util.UUID

fun Routing.appRoutes(
    personPseudoIdProvider: ValkeyPersonPseudoIdProvider,
    transactionProvider: TransactionProvider,
) {
    route("/api") {
        route("/openapi.json") {
            openApi()
        }
        route("/swagger") {
            swaggerUI("../openapi.json")
        }

        authenticate("oidc") {
            get("/personer/{pseudoId}/dialogmeldinger", {
                operationId = "getDialogmeldinger"
                description = "Hent oversikt over alle dialoger"
                request {
                    pathParameter<String>("pseudoId") {
                        description = "Pseudonymisert person-ID"
                        required = true
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "Liste over dialoger"
                        body<List<ApiDialogOppsummering>>()
                    }
                }
            }) {
//                        val pseudoId = call.parameters["pseudoId"]
//                        veksle pseudoId med fødselsnummer her
                call.respond(MockStore.hentOversikt())
            }

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
//                        val pseudoId = call.parameters["pseudoId"]
//                        veksle pseudoId med fødselsnummer her
                val conversationRef = call.parameters["conversationRef"]!!
                val dialog = MockStore.hentDialog(conversationRef)
                if (dialog != null) {
                    call.respond(dialog)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            post("/personer/{pseudoId}/dialogmelding", {
                operationId = "postDialogmelding"
                description = "Send ny dialogmelding"
                request {
                    pathParameter<String>("pseudoId") {
                        description = "Pseudonymisert person-ID"
                        required = true
                    }
                    body<ApiNyDialogmelding>()
                }
                response {
                    HttpStatusCode.Created to {
                        description = "Dialogmelding opprettet"
                        body<ApiDialogDetails>()
                    }
                }
            }) {
                val pseudoId = call.personPseudoId()
                val saksbehandler = call.saksbehandler()
                val identitetsnummer = personPseudoIdProvider.hentIdentitetsnummer(pseudoId) ?: error("Fant ikke identitetsnummer for pseudoId $pseudoId")
                val apiDialogmelding = call.receive<ApiNyDialogmelding>()
                // TODO: Ta vare på telefonnummer og adresse for behandler
                val behandler =
                    Behandler(
                        HprNummer(apiDialogmelding.behandler.id),
                        navn = apiDialogmelding.behandler.navn.fornavn + " " + apiDialogmelding.behandler.navn.mellomnavn + " " + apiDialogmelding.behandler.navn.etternavn, // TODO: håndtere at mellomnavn kan være null
                        kontor = apiDialogmelding.behandler.legekontor.kontor!!, // TODO: Burde forvente at denne ikke kan være null fra Speil
                        kontorOrganisasjonsnummer = Organisasjonsnummer(apiDialogmelding.behandler.legekontor.orgnummer!!), // TODO: Burde forvente at denne ikke kan være null fra Speil
                    )
                transactionProvider.transaction {
                    val dialog = Dialog.ny(identitetsnummer, Dialogmelding.FraNav.ny(saksbehandler.ident, behandler, behandlerRef = BehandlerRef(apiDialogmelding.behandler.id), apiDialogmelding.melding))
                    dialogRepository.lagre(dialog)
                    val events = dialog.events()
                    events.forEach {
                        outbox.nyMelding(OutboxMelding(OutboxMeldingId(UUID.randomUUID()), it))
                    }
                }
                val ny = call.receive<ApiNyDialogmelding>()
                val opprettet = MockStore.leggTilMelding(ny)
                call.respond(HttpStatusCode.Created, opprettet)
            }

            post("/personer/{pseudoId}/dialogmeldinger/{conversationRef}/svar", {
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
//                        val pseudoId = call.parameters["pseudoId"]
//                        veksle pseudoId med fødselsnummer her
                val conversationRef = call.parameters["conversationRef"]!!
                val svar = call.receive<ApiSvarPaDialog>()
                val oppdatert = MockStore.svarPåDialog(conversationRef, svar)
                if (oppdatert != null) {
                    call.respond(HttpStatusCode.Created, oppdatert)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
