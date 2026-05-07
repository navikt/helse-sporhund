package api

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

data class ApiDialog(
    val id: String,
    val tittel: String,
    val tid: String,
    val dialogmeldinger: List<ApiDialogmelding>,
)

data class ApiBehandlerDialog(
    val behandlerId: String,
    val behandlernavn: String,
    val dialoger: List<ApiDialog>,
)

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

object MockStore {
    private val data: MutableList<ApiBehandlerDialog> = initialMockData().toMutableList()

    fun hentAlle(): List<ApiBehandlerDialog> = data.toList()

    fun leggTilMelding(ny: ApiNyDialogmelding): ApiBehandlerDialog {
        val behandlerIndex = data.indexOfFirst { it.behandlerId == ny.behandlerId }
        val behandler = if (behandlerIndex >= 0) data[behandlerIndex] else throw NoSuchElementException("Behandler ikke funnet")

        val nyDialog =
            ApiDialog(
                id =
                    java.util.UUID
                        .randomUUID()
                        .toString(),
                tittel = ny.type.tittel,
                tid =
                    java.time.LocalDateTime
                        .now()
                        .toString(),
                dialogmeldinger =
                    listOf(
                        ApiDialogmelding(
                            tittel = ny.type.tittel,
                            melding = ny.melding,
                            tid =
                                java.time.LocalDateTime
                                    .now()
                                    .toString(),
                            fraNav = true,
                            vedlegg = emptyList(),
                        ),
                    ),
            )
        val oppdatertBehandler = behandler.copy(dialoger = behandler.dialoger + nyDialog)
        data[behandlerIndex] = oppdatertBehandler
        return oppdatertBehandler
    }
}

fun mockDialogmeldinger(): List<ApiBehandlerDialog> = MockStore.hentAlle()

private fun initialMockData(): List<ApiBehandlerDialog> =
    listOf(
        ApiBehandlerDialog(
            behandlerId = "behandlerId-1",
            behandlernavn = "Linus Lege",
            dialoger =
                listOf(
                    ApiDialog(
                        id = "dialogId-1",
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
                    ApiDialog(
                        id = "dialogId-2",
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
                ),
        ),
        ApiBehandlerDialog(
            behandlerId = "behandlerId-2",
            behandlernavn = "Solveig Lege",
            dialoger =
                listOf(
                    ApiDialog(
                        id = "dialogId-3",
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
                ),
        ),
        ApiBehandlerDialog(
            behandlerId = "behandlerId-3",
            behandlernavn = "Christian Lege",
            dialoger =
                listOf(
                    ApiDialog(
                        id = "dialogId-4",
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
                    ApiDialog(
                        id = "dialogId-5",
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
                    ApiDialog(
                        id = "dialogId-6",
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
                ),
        ),
    )
