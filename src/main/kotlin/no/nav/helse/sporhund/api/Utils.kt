package no.nav.helse.sporhund.api

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import no.nav.helse.sporhund.api.auth.SaksbehandlerPrincipal
import no.nav.helse.sporhund.application.PersonPseudoId
import java.util.UUID

fun ApplicationCall.personPseudoId() = PersonPseudoId(UUID.fromString(requireNotNull(this.parameters["person-pseudoId"])))

fun ApplicationCall.saksbehandler() = requireNotNull(principal<SaksbehandlerPrincipal>()).saksbehandler
