package no.nav.helse.sporhund.api.endepunkter

import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.api.ApiDialogDetails
import no.nav.helse.sporhund.api.ApiNyDialogmelding
import no.nav.helse.sporhund.api.MockStore
import no.nav.helse.sporhund.api.mapping.tilBehandler
import no.nav.helse.sporhund.api.personPseudoId
import no.nav.helse.sporhund.api.saksbehandler
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.OutboxMeldingId
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.clients.personpseudoid.ValkeyPersonPseudoIdProvider
import no.nav.helse.sporhund.domain.BehandlerRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding
import java.util.*

fun Route.postDialogmeldingRoute(
    personPseudoIdProvider: ValkeyPersonPseudoIdProvider,
    transactionProvider: TransactionProvider,
) {
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
        // TODO: Kommenterte ut elvisen pga mocken
        val identitetsnummer =
            personPseudoIdProvider.hentIdentitetsnummer(pseudoId) // ?: error("Fant ikke identitetsnummer for pseudoId $pseudoId")
        val apiDialogmelding = call.receive<ApiNyDialogmelding>()
        // TODO: Kan fjerne if-en når elvisen over kommenteres inn igjen
        if (identitetsnummer != null) {
            transactionProvider.transaction {
                val dialog =
                    Dialog.ny(
                        identitetsnummer,
                        Dialogmelding.FraNav.ny(
                            saksbehandler.ident,
                            // TODO: Ta vare på adresse for behandler
                            apiDialogmelding.tilBehandler(),
                            behandlerRef = BehandlerRef(apiDialogmelding.behandler.id),
                            apiDialogmelding.melding,
                        ),
                    )
                dialogRepository.lagre(dialog)
                val events = dialog.events()
                events.forEach {
                    outbox.nyMelding(OutboxMelding(OutboxMeldingId(UUID.randomUUID()), it))
                }
            }
        }

        val opprettet = MockStore.leggTilMelding(apiDialogmelding, pseudoId.value.toString())
        call.respond(HttpStatusCode.Created, opprettet)
    }
}
