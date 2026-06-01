package no.nav.helse.sporhund.domain.testhelpers

import no.nav.helse.sporhund.domain.Adresse
import no.nav.helse.sporhund.domain.Kontor
import no.nav.helse.sporhund.domain.Organisasjonsnummer
import kotlin.random.Random

fun lagKontor() =
    Kontor(
        navn = lagOrganisasjonsnavn(),
        organisasjonsnummer = lagOrganisasjonsnummer(),
        adresse = lagAdresse(),
    )

private val organisasjonsnavnDel1 = listOf("NEPE", "KLOVNE", "BOBLEBAD-", "DUSTE", "SKIHOPP", "SMÅBARN", "SPANIA")
private val organisasjonsnavnDel2 = listOf("AVDELINGEN", "SENTERET", "FORUM", "KLUBBEN", "SNEKKERIET")

fun lagOrganisasjonsnavn() = organisasjonsnavnDel1.random() + organisasjonsnavnDel2.random()

fun lagOrganisasjonsnummer() = Organisasjonsnummer(Random.nextInt(100_000_000, 999_999_999).toString())

fun lagAdresse() =
    Adresse(
        veiadresse = lagVeiadresse(),
        postnummer = Random.nextInt(100, 9999).toString().padStart(4, '0'),
        poststed = listOf("OSLO", "BERGEN", "TRONDHEIM", "STAVANGER", "TROMSØ").random(),
    )

private val gatenavn = listOf("Storgata", "Kirkegata", "Skolegata", "Torggata", "Parkveien", "Navgata", "Fjordgata")

fun lagVeiadresse() = "${gatenavn.random()} ${Random.nextInt(1, 200)}"
