package no.nav.helse.sporhund.api

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route

fun Routing.appRoutes() {
    route("/api") {
        route("/openapi.json") {
            openApi()
        }
        route("/swagger") {
            swaggerUI("../openapi.json")
        }

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

        get("/personer/{pseudoId}/dialogmeldinger/{dialogId}", {
            operationId = "getDialogmelding"
            description = "Hent en enkelt dialog med alle meldinger"
            request {
                pathParameter<String>("pseudoId") {
                    description = "Pseudonymisert person-ID"
                    required = true
                }
                pathParameter<String>("dialogId") {
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
            val dialogId = call.parameters["dialogId"]!!
            val dialog = MockStore.hentDialog(dialogId)
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
//                        val pseudoId = call.parameters["pseudoId"]
//                        veksle pseudoId med fødselsnummer her
            val ny = call.receive<ApiNyDialogmelding>()
            val opprettet = MockStore.leggTilMelding(ny)
            call.respond(HttpStatusCode.Created, opprettet)
        }

        post("/personer/{pseudoId}/dialogmeldinger/{dialogId}/svar", {
            operationId = "postSvarPaDialog"
            description = "Svar på en eksisterende dialog"
            request {
                pathParameter<String>("pseudoId") {
                    description = "Pseudonymisert person-ID"
                    required = true
                }
                pathParameter<String>("dialogId") {
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
            val dialogId = call.parameters["dialogId"]!!
            val svar = call.receive<ApiSvarPaDialog>()
            val oppdatert = MockStore.svarPåDialog(dialogId, svar)
            if (oppdatert != null) {
                call.respond(HttpStatusCode.Created, oppdatert)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
