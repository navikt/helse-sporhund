package no.nav.helse.sporhund.api.endepunkter

import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.api.ApiDialogDetails
import no.nav.helse.sporhund.api.ApiDialogmeldingType
import no.nav.helse.sporhund.api.ApiFagomrade
import no.nav.helse.sporhund.api.ApiNyDialogmelding
import no.nav.helse.sporhund.api.mapping.tilApiDialogDetails
import no.nav.helse.sporhund.api.mapping.tilBehandler
import no.nav.helse.sporhund.api.personPseudoId
import no.nav.helse.sporhund.api.saksbehandler
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.OutboxMeldingId
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.domain.BehandlerRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.Dialogtype
import no.nav.helse.sporhund.domain.Fagområde
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
                val dialog =
                    Dialog.ny(
                        identitetsnummer,
                        Dialogmelding.FraNav.ny(
                            saksbehandler = saksbehandler.ident,
                            behandler = apiDialogmelding.tilBehandler(),
                            behandlerRef = BehandlerRef(apiDialogmelding.behandler.id),
                            melding = apiDialogmelding.melding,
                        ),
                        fagområde =
                            when (apiDialogmelding.fagomrade) {
                                ApiFagomrade.ENKELTSTAENDE_BEHANDLINGSDAGER -> Fagområde.EnkeltståendeBehandlingsdager
                                ApiFagomrade.TILBAKEDATERING -> Fagområde.Tilbakedatering
                                ApiFagomrade.YRKESSKADE -> Fagområde.Yrkesskade
                                ApiFagomrade.BESTRIDELSE -> Fagområde.Bestridelse
                            },
                        dialogtype =
                            when (apiDialogmelding.meldingstype) {
                                ApiDialogmeldingType.JOURNALNOTAT -> Dialogtype.Journalnotat
                                ApiDialogmeldingType.MEDISINSKE_OPPLYSNINGER -> Dialogtype.MedisinskeOpplysninger
                                ApiDialogmeldingType.EKSTRA_UTTALELSER_FRA_LEGE -> Dialogtype.EkstraUttalelserFraLege
                                ApiDialogmeldingType.SPESIALISTERKLAERING -> Dialogtype.SpesialistErklæring
                                ApiDialogmeldingType.UTVIDET_SPESIALISTERKLAERING -> Dialogtype.UtvidetSpesialistErklæring
                            },
                    )
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
