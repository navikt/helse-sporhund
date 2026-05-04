package domain.testhelpers

import domain.NavIdent
import kotlin.random.Random

fun lagNavIdent(): NavIdent = NavIdent(('A'..'Z').random().toString() + "${Random.nextInt(from = 200_000, until = 999_999)}")
