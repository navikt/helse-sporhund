package no.nav.helse.sporhund.domain.testhelpers

import no.nav.helse.sporhund.domain.NavIdent
import no.nav.helse.sporhund.domain.Saksbehandler
import kotlin.random.Random

fun lagNavIdent(): NavIdent = NavIdent(('A'..'Z').random().toString() + "${Random.nextInt(from = 200_000, until = 999_999)}")

fun lagSaksbehandler(
    navn: String =
        buildString {
            append(lagEtternavn())
            append(", ${lagFornavn()}")
            val mellomnavn = lagMellomnavnOrNull()
            if (mellomnavn != null) append(" $mellomnavn")
        },
    epost: String = navn.split(" ").joinToString(".").lowercase() + "@nav.no",
    navIdent: String =
        navn.substringAfterLast(' ').first() + "${
            Random.nextInt(
                from = 200_000,
                until = 999_999,
            )
        }",
): Saksbehandler =
    Saksbehandler(
        navn = navn,
        epost = epost,
        ident = NavIdent(navIdent),
    )
