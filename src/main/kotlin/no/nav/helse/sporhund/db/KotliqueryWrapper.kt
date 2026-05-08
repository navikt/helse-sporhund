package no.nav.helse.sporhund.db

import kotliquery.Query
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import org.intellij.lang.annotations.Language

fun asSQL(
    @Language("SQL") sql: String,
    vararg params: Pair<String, Any?>,
) = queryOf(sql, params.toMap())

/**
 * Lager en string som kan gjøres om til et array ved å caste den til varchar[] i spørringer.
 *
 * For eksempel:
 * ```
 * i parameterene:
 *   "identer" to personidenter.somDbArray()
 * i spørringen:
 *   :identer::varchar[]
 * ```
 */
fun <T> Iterable<T>?.somDbArray(transform: (T) -> CharSequence = { it.toString() }) = this?.joinToString(prefix = "{", postfix = "}", transform = transform) ?: "{}"

fun insert(
    @Language("SQL") sql: String,
    params: Map<String, Any?>,
) = queryOf(sql, params)

// Plis bare bruk denne til ting det ikke går an å gjøre med navngitte parametere - eks. ".. AND orgnummer = ANY(?)"
fun asSQLWithQuestionMarks(
    @Language("SQL") sql: String,
    vararg params: Any?,
) = queryOf(sql, *params)

fun <T> Query.single(
    session: Session,
    mapping: (Row) -> T?,
) = session.run(map(mapping).asSingle)

fun <T> Query.list(
    session: Session,
    mapping: (Row) -> T?,
) = session.run(map(mapping).asList)

fun Query.update(session: Session) = session.run(asUpdate)

fun Query.updateAndReturnGeneratedKey(session: Session) = session.run(this.asUpdateAndReturnGeneratedKey)
