package no.nav.helse.sporhund.api

import java.time.LocalDateTime
import java.util.UUID

// === Detail types (full dialog with messages) ===

data class ApiVedlegg(
    val navn: String,
    val url: String,
)

data class ApiDialogmelding(
    val tittel: String,
    val melding: String,
    val tid: String,
    val fraNav: Boolean,
    val vedlegg: List<ApiVedlegg>,
)

data class ApiDialogDetails(
    val id: String,
    val behandlerId: String,
    val behandlernavn: String,
    val tittel: String,
    val tid: String,
    val dialogmeldinger: List<ApiDialogmelding>,
)

// === Summary types (inbox list) ===

data class ApiDialogOppsummering(
    val id: String,
    val tittel: String,
    val tid: String,
    val antallMeldinger: Int,
    val antallVedlegg: Int,
)

data class ApiBehandlerMedDialoger(
    val behandlerId: String,
    val behandlernavn: String,
    val dialoger: List<ApiDialogOppsummering>,
)

// === Request type ===

enum class DialogmeldingType(
    val tittel: String,
) {
    L8("Tilleggsopplysninger (L8)"),
    L40("Legeerklæring (L40)"),
}

data class ApiNyDialogmelding(
    val behandlerId: String,
    val behandlernavn: String,
    val type: DialogmeldingType,
    val melding: String,
)

data class ApiSvarPaDialog(
    val melding: String,
)

// === Internal model + store ===

private data class InternalDialog(
    val id: String,
    val behandlerId: String,
    val behandlernavn: String,
    val tittel: String,
    val tid: String,
    val dialogmeldinger: List<ApiDialogmelding>,
)

object MockStore {
    private val data: MutableList<InternalDialog> = initialMockData().toMutableList()

    fun hentOversikt(): List<ApiBehandlerMedDialoger> =
        data.groupBy { it.behandlerId }.map { (_, dialoger) ->
            ApiBehandlerMedDialoger(
                behandlerId = dialoger.first().behandlerId,
                behandlernavn = dialoger.first().behandlernavn,
                dialoger = dialoger.map { it.tilOversikt() },
            )
        }

    fun hentDialog(dialogId: String): ApiDialogDetails? = data.find { it.id == dialogId }?.tilDialogDetails()

    fun leggTilMelding(ny: ApiNyDialogmelding): ApiDialogDetails {
        val nyInternalDialog =
            InternalDialog(
                id =
                    UUID
                        .randomUUID()
                        .toString(),
                behandlerId = ny.behandlerId,
                behandlernavn = ny.behandlernavn,
                tittel = ny.type.tittel,
                tid =
                    LocalDateTime
                        .now()
                        .toString(),
                dialogmeldinger =
                    listOf(
                        ApiDialogmelding(
                            tittel = ny.type.tittel,
                            melding = ny.melding,
                            tid =
                                LocalDateTime
                                    .now()
                                    .toString(),
                            fraNav = true,
                            vedlegg = emptyList(),
                        ),
                    ),
            )
        data.add(nyInternalDialog)
        return nyInternalDialog.tilDialogDetails()
    }

    fun svarPåDialog(
        dialogId: String,
        svar: ApiSvarPaDialog,
    ): ApiDialogDetails? {
        val index = data.indexOfFirst { it.id == dialogId }
        if (index < 0) return null
        val dialog = data[index]
        val nyMelding =
            ApiDialogmelding(
                tittel = dialog.tittel,
                melding = svar.melding,
                tid =
                    LocalDateTime
                        .now()
                        .toString(),
                fraNav = true,
                vedlegg = emptyList(),
            )
        data[index] = dialog.copy(dialogmeldinger = dialog.dialogmeldinger + nyMelding)
        return data[index].tilDialogDetails()
    }

    private fun InternalDialog.tilOversikt() =
        ApiDialogOppsummering(
            id = id,
            tittel = tittel,
            tid = tid,
            antallMeldinger = dialogmeldinger.size,
            antallVedlegg = dialogmeldinger.sumOf { it.vedlegg.size },
        )

    private fun InternalDialog.tilDialogDetails() =
        ApiDialogDetails(
            id = id,
            behandlerId = behandlerId,
            behandlernavn = behandlernavn,
            tittel = tittel,
            tid = tid,
            dialogmeldinger = dialogmeldinger,
        )
}

private fun initialMockData(): List<InternalDialog> =
    listOf(
        InternalDialog(
            id = "dialogId-1",
            behandlerId = "behandlerId-1",
            behandlernavn = "Linus Lege",
            tittel = "Forespørsel om dokumentasjon",
            tid = "2026-04-24T14:36:00",
            dialogmeldinger =
                listOf(
                    ApiDialogmelding(
                        tittel = "Forespørsel om dokumentasjon",
                        melding =
                            "Takk for tilsendt dokumentasjon. Vi trenger noen tilleggsopplysninger om " +
                                "pasientens funksjonsnivå og eventuelle tilretteleggingsmuligheter på " +
                                "arbeidsplassen. Kan dere gi en nærmere vurdering av dette?",
                        tid = "2026-04-24T14:36:00",
                        fraNav = true,
                        vedlegg = emptyList(),
                    ),
                    ApiDialogmelding(
                        tittel = "Svar på forespørsel",
                        melding =
                            "Hei, vedlagt finner dere den forespurte dokumentasjonen. Jeg har lagt ved " +
                                "relevant journaldokumentasjon og vurdering av pasientens tilstand. " +
                                "Ta gjerne kontakt dersom dere trenger ytterligere opplysninger.",
                        tid = "2026-04-22T07:21:00",
                        fraNav = false,
                        vedlegg =
                            listOf(
                                ApiVedlegg(navn = "Sykmelding.pdf", url = "#"),
                                ApiVedlegg(navn = "Legeerklæring.pdf", url = "#"),
                                ApiVedlegg(navn = "Journal_2024.pdf", url = "#"),
                            ),
                    ),
                    ApiDialogmelding(
                        tittel = "Ytterligere dokumentasjon",
                        melding =
                            "Hei, vi behandler saken til Mia Cathrine Svendsen og trenger ytterligere " +
                                "dokumentasjon for å kunne fatte et vedtak. Kan dere sende over relevant " +
                                "dokumentasjon som belyser pasientens tilstand og arbeidsevne?",
                        tid = "2026-04-20T09:15:00",
                        fraNav = true,
                        vedlegg = emptyList(),
                    ),
                ),
        ),
        InternalDialog(
            id = "dialogId-2",
            behandlerId = "behandlerId-1",
            behandlernavn = "Linus Lege",
            tittel = "Oppfølging etter sykmelding",
            tid = "2026-04-20T08:30:00",
            dialogmeldinger =
                listOf(
                    ApiDialogmelding(
                        tittel = "Oppfølging etter sykmelding",
                        melding =
                            "Vi ønsker en oppdatering på pasientens tilstand og forventet varighet på sykmeldingen.",
                        tid = "2026-04-20T08:30:00",
                        fraNav = true,
                        vedlegg = emptyList(),
                    ),
                ),
        ),
        InternalDialog(
            id = "dialogId-3",
            behandlerId = "behandlerId-2",
            behandlernavn = "Solveig Lege",
            tittel = "Forespørsel om dokumentasjon",
            tid = "2026-04-24T14:36:00",
            dialogmeldinger =
                listOf(
                    ApiDialogmelding(
                        tittel = "Forespørsel om dokumentasjon",
                        melding =
                            "Vi ber om dokumentasjon knyttet til pasientens diagnose og behandlingsplan.",
                        tid = "2026-04-24T14:36:00",
                        fraNav = true,
                        vedlegg = emptyList(),
                    ),
                    ApiDialogmelding(
                        tittel = "Svar med vedlegg",
                        melding = "Vedlagt sender jeg etterspurt dokumentasjon.",
                        tid = "2026-04-23T10:49:00",
                        fraNav = false,
                        vedlegg = listOf(ApiVedlegg(navn = "Dokumentasjon.pdf", url = "#")),
                    ),
                ),
        ),
        InternalDialog(
            id = "dialogId-4",
            behandlerId = "behandlerId-3",
            behandlernavn = "Christian Lege",
            tittel = "Sykmeldingsopplysninger",
            tid = "2026-04-10T09:00:00",
            dialogmeldinger =
                listOf(
                    ApiDialogmelding(
                        tittel = "Sykmeldingsopplysninger",
                        melding =
                            "Vi ønsker mer informasjon om diagnosen og prognosen for tilbakekomst til arbeid.",
                        tid = "2026-04-10T09:00:00",
                        fraNav = true,
                        vedlegg = emptyList(),
                    ),
                    ApiDialogmelding(
                        tittel = "Svar",
                        melding =
                            "Pasienten er sykmeldt grunnet rygglidelse. Prognosen er god, forventet tilbakekomst om 6–8 uker.",
                        tid = "2026-04-08T13:15:00",
                        fraNav = false,
                        vedlegg = listOf(ApiVedlegg(navn = "Sykmelding.pdf", url = "#")),
                    ),
                ),
        ),
        InternalDialog(
            id = "dialogId-5",
            behandlerId = "behandlerId-3",
            behandlernavn = "Christian Lege",
            tittel = "Vurdering av arbeidsevne",
            tid = "2026-04-05T11:00:00",
            dialogmeldinger =
                listOf(
                    ApiDialogmelding(
                        tittel = "Vurdering av arbeidsevne",
                        melding =
                            "Kan dere gi en vurdering av pasientens nåværende arbeidsevne og muligheter for gradert sykmelding?",
                        tid = "2026-04-05T11:00:00",
                        fraNav = true,
                        vedlegg = emptyList(),
                    ),
                    ApiDialogmelding(
                        tittel = "Svar på vurdering",
                        melding =
                            "Pasienten kan på det nåværende tidspunkt ikke benytte seg av gradert sykmelding, " +
                                "men vi vil revurdere dette om 2 uker.",
                        tid = "2026-04-04T10:00:00",
                        fraNav = false,
                        vedlegg = listOf(ApiVedlegg(navn = "Vurdering.pdf", url = "#")),
                    ),
                ),
        ),
        InternalDialog(
            id = "dialogId-6",
            behandlerId = "behandlerId-3",
            behandlernavn = "Christian Lege",
            tittel = "Bekreftelse på behandlingsplan",
            tid = "2026-03-28T14:00:00",
            dialogmeldinger =
                listOf(
                    ApiDialogmelding(
                        tittel = "Bekreftelse på behandlingsplan",
                        melding = "Vi ber om bekreftelse på at behandlingsplanen er iverksatt.",
                        tid = "2026-03-28T14:00:00",
                        fraNav = true,
                        vedlegg = emptyList(),
                    ),
                ),
        ),
    )
