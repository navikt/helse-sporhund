package no.nav.helse.sporhund.domain.testhelpers

import no.nav.helse.sporhund.domain.Navn

private val etternavnListe =
    listOf(
        "Diode",
        "Flom",
        "Damesykkel",
        "Undulat",
        "Bakgrunn",
        "Genser",
        "Fornøyelse",
        "Campingvogn",
        "Bakkeklaring"
    )

fun lagEtternavn() = etternavnListe.random()

private val fornavnListe =
    listOf(
        "Måteholden",
        "Dypsindig",
        "Ultrafiolett",
        "Urettferdig",
        "Berikende",
        "Upresis",
        "Stridlynt",
        "Rund",
        "Internasjonal"
    )

fun lagFornavn() = fornavnListe.random()

private val mellomnavnListe =
    listOf(
        "Lysende",
        "Spennende",
        "Tidløs",
        "Hjertelig",
        "Storslått",
        "Sjarmerende",
        "Uforutsigbar",
        "Behagelig",
        "Robust",
        "Sofistikert"
    )

fun lagMellomnavn() = mellomnavnListe.random()

fun lagMellomnavnOrNull() =
    if (Math.random() < 0.5) {
        lagMellomnavn()
    } else {
        null
    }

fun lagNavn() =
    Navn(
        fornavn = lagFornavn(),
        mellomnavn = lagMellomnavnOrNull(),
        etternavn = lagEtternavn()
    )
