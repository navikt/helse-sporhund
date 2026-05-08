package no.nav.helse.sporhund.application

interface TransactionProvider {
    fun <T> transaction(session: SessionContext.() -> T): T
}
