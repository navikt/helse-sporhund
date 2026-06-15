package no.nav.helse.sporhund.infrastructure.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import no.nav.helse.sporhund.application.PersonPseudoId
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.sporhund.domain.tilgangskontroll.Tilgang
import no.nav.helse.sporhund.infrastructure.api.auth.SaksbehandlerPrincipal
import java.util.*

fun ApplicationCall.personPseudoId() = PersonPseudoId(UUID.fromString(requireNotNull(this.parameters["pseudoId"])))

fun ApplicationCall.saksbehandler() = requireNotNull(principal<SaksbehandlerPrincipal>()).saksbehandler

fun ApplicationCall.accessToken() = requireNotNull(principal<SaksbehandlerPrincipal>()).accessToken

fun ApplicationCall.tilganger(): Set<Tilgang> = requireNotNull(principal<SaksbehandlerPrincipal>()).tilganger

fun ApplicationCall.brukerroller(): Set<Brukerrolle> = requireNotNull(principal<SaksbehandlerPrincipal>()).brukerroller

fun ApplicationCall.conversationRef() = ConversationRef(UUID.fromString(requireNotNull(parameters["conversationRef"])))
