package no.nav.helse.sporhund.domain.testhelpers

import no.nav.helse.sporhund.domain.NavIdent
import no.nav.helse.sporhund.domain.Saksbehandler
import no.nav.helse.sporhund.domain.SaksbehandlerOid
import java.util.*
import kotlin.random.Random

fun lagNavIdent(): NavIdent = NavIdent(('A'..'Z').random().toString() + "${Random.nextInt(from = 200_000, until = 999_999)}")

fun lagSaksbehandler(
    id: SaksbehandlerOid = SaksbehandlerOid(UUID.randomUUID()),
    navn: String =
        buildString {
            append(lagEtternavn())
            append(", ${lagFornavn()}")
            val mellomnavn = lagMellomnavnOrNull()
            if (mellomnavn != null) append(" $mellomnavn")
        },
    epost: String = navn.split(" ").joinToString(".").lowercase() + "@nav.no",
    navIdent: String =
        navn.also { println(it) }.substringAfterLast(' ').first() + "${
            Random.nextInt(
                from = 200_000,
                until = 999_999,
            )
        }",
): Saksbehandler =
    Saksbehandler(
        id = id,
        navn = navn,
        epost = epost,
        ident = NavIdent(navIdent),
    )

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
        "Bakkeklaring",
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
        "Internasjonal",
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
        "Sofistikert",
    )

fun lagMellomnavn() = mellomnavnListe.random()

fun lagMellomnavnOrNull() =
    if (Math.random() < 0.5) {
        lagMellomnavn()
    } else {
        null
    }
