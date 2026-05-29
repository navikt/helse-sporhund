package no.nav.helse.sporhund.domain

data class Behandler(
    val hprNummer: HprNummer,
    val navn: Navn,
    val kontor: Kontor,
    val telefonnummer: Telefonnummer?
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
)

data class Kontor(
    val navn: String?,
    val organisasjonsnummer: Organisasjonsnummer?,
    val adresse: Adresse?
)

data class Adresse(
    val veiadresse: String?,
    val postnummer: String?,
    val poststed: String?
)
