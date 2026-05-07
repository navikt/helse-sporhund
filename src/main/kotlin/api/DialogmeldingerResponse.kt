package api

data class ApiVedlegg(
    val navn: String,
    val url: String,
)

data class ApiDialogmelding(
    val tittel: String,
    val innehold: String,
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

data class ApiBehandlerDialoger(
    val behandlernavn: String,
    val dialoger: List<ApiDialog>,
)

fun mockDialogmeldinger(): List<ApiBehandlerDialoger> =
    listOf(
        ApiBehandlerDialoger(
            behandlernavn = "Dialog med Linus Lege",
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
                                    innehold =
                                        "Takk for tilsendt dokumentasjon. Vi trenger noen tilleggsopplysninger om " +
                                            "pasientens funksjonsnivå og eventuelle tilretteleggingsmuligheter på " +
                                            "arbeidsplassen. Kan dere gi en nærmere vurdering av dette?",
                                    tid = "2026-04-24T14:36:00",
                                    fraNav = true,
                                    vedlegg = emptyList(),
                                ),
                                ApiDialogmelding(
                                    tittel = "Svar på forespørsel",
                                    innehold =
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
                                    innehold =
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
                                    innehold =
                                        "Vi ønsker en oppdatering på pasientens tilstand og forventet varighet på sykmeldingen.",
                                    tid = "2026-04-20T08:30:00",
                                    fraNav = true,
                                    vedlegg = emptyList(),
                                ),
                            ),
                    ),
                ),
        ),
        ApiBehandlerDialoger(
            behandlernavn = "Dialog med Solveig Lege",
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
                                    innehold =
                                        "Vi ber om dokumentasjon knyttet til pasientens diagnose og behandlingsplan.",
                                    tid = "2026-04-24T14:36:00",
                                    fraNav = true,
                                    vedlegg = emptyList(),
                                ),
                                ApiDialogmelding(
                                    tittel = "Svar med vedlegg",
                                    innehold = "Vedlagt sender jeg etterspurt dokumentasjon.",
                                    tid = "2026-04-23T10:49:00",
                                    fraNav = false,
                                    vedlegg = listOf(ApiVedlegg(navn = "Dokumentasjon.pdf", url = "#")),
                                ),
                            ),
                    ),
                ),
        ),
        ApiBehandlerDialoger(
            behandlernavn = "Dialog med Christian Lege",
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
                                    innehold =
                                        "Vi ønsker mer informasjon om diagnosen og prognosen for tilbakekomst til arbeid.",
                                    tid = "2026-04-10T09:00:00",
                                    fraNav = true,
                                    vedlegg = emptyList(),
                                ),
                                ApiDialogmelding(
                                    tittel = "Svar",
                                    innehold =
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
                                    innehold =
                                        "Kan dere gi en vurdering av pasientens nåværende arbeidsevne og muligheter for gradert sykmelding?",
                                    tid = "2026-04-05T11:00:00",
                                    fraNav = true,
                                    vedlegg = emptyList(),
                                ),
                                ApiDialogmelding(
                                    tittel = "Svar på vurdering",
                                    innehold =
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
                                    innehold = "Vi ber om bekreftelse på at behandlingsplanen er iverksatt.",
                                    tid = "2026-03-28T14:00:00",
                                    fraNav = true,
                                    vedlegg = emptyList(),
                                ),
                            ),
                    ),
                ),
        ),
    )
