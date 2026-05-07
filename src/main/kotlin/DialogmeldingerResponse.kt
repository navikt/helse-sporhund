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
                ),
        ),
    )
