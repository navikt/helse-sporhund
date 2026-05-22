package no.nav.helse.sporhund.api

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import no.nav.helse.sporhund.api.auth.SaksbehandlerPrincipal
import no.nav.helse.sporhund.application.PersonPseudoId
import no.nav.helse.sporhund.domain.ConversationRef
import java.util.*

fun ApplicationCall.personPseudoId() = PersonPseudoId(UUID.fromString(requireNotNull(this.parameters["pseudoId"])))

fun ApplicationCall.saksbehandler() = requireNotNull(principal<SaksbehandlerPrincipal>()).saksbehandler

fun ApplicationCall.accessToken() = requireNotNull(principal<SaksbehandlerPrincipal>()).accessToken

fun ApplicationCall.conversationRef() = ConversationRef(UUID.fromString(requireNotNull(parameters["conversationRef"])))
