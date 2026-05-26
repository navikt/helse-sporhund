package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.OutboxMeldingId
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.domain.*
import no.nav.helse.sporhund.infrastructure.api.*
import no.nav.helse.sporhund.infrastructure.api.mapping.tilApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.mapping.tilBehandler
import java.util.*

fun Route.postNyDialogmeldingRoute(
    personPseudoIdProvider: PersonPseudoIdProvider,
    transactionProvider: TransactionProvider,
) {
    post("/personer/{pseudoId}/dialogmelding", {
        operationId = "postNyDialogmelding"
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
        val identitetsnummer =
            personPseudoIdProvider.hentIdentitetsnummer(pseudoId) ?: error("Fant ikke identitetsnummer for pseudoId $pseudoId")
        val apiDialogmelding = call.receive<ApiNyDialogmelding>()
        val dialog =
            transactionProvider.transaction {
                val dialog = apiDialogmelding.tilDialog(identitetsnummer, saksbehandler)
                dialogRepository.lagre(dialog)
                val events = dialog.events()
                events.forEach {
                    outbox.nyMelding(OutboxMelding(OutboxMeldingId(UUID.randomUUID()), it))
                }
                return@transaction dialog
            }

        call.respond(HttpStatusCode.Created, dialog.tilApiDialogDetails())
    }
}

private fun ApiNyDialogmelding.tilDialog(
    identitetsnummer: Identitetsnummer,
    saksbehandler: Saksbehandler,
): Dialog =
    Dialog.ny(
        identitetsnummer = identitetsnummer,
        søkernavn =
            Navn(
                fornavn = sokernavn.fornavn,
                mellomnavn = sokernavn.mellomnavn,
                etternavn = sokernavn.etternavn,
            ),
        melding =
            Dialogmelding.FraNav.ny(
                saksbehandler = saksbehandler.ident,
                behandler = tilBehandler(),
                behandlerRef = BehandlerRef(behandler.id),
                melding = melding,
            ),
        fagområde =
            when (fagomrade) {
                ApiFagomrade.ENKELTSTAENDE_BEHANDLINGSDAGER -> Fagområde.EnkeltståendeBehandlingsdager
                ApiFagomrade.TILBAKEDATERING -> Fagområde.Tilbakedatering
                ApiFagomrade.YRKESSKADE -> Fagområde.Yrkesskade
                ApiFagomrade.BESTRIDELSE -> Fagområde.Bestridelse
            },
        dialogtype =
            when (meldingstype) {
                ApiDialogmeldingType.JOURNALNOTAT -> Dialogtype.Journalnotat
                ApiDialogmeldingType.MEDISINSKE_OPPLYSNINGER -> Dialogtype.MedisinskeOpplysninger
                ApiDialogmeldingType.EKSTRA_UTTALELSER_FRA_LEGE -> Dialogtype.EkstraUttalelserFraLege
                ApiDialogmeldingType.SPESIALISTERKLAERING -> Dialogtype.SpesialistErklæring
                ApiDialogmeldingType.UTVIDET_SPESIALISTERKLAERING -> Dialogtype.UtvidetSpesialistErklæring
            },
    )
