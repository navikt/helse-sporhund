package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.sporhund.application.InMemoryPersonPseudoIdProvider
import no.nav.helse.sporhund.application.InMemoryPopulasjonstilgangskontrollProvider
import no.nav.helse.sporhund.application.InMemoryTransactionProvider
import no.nav.helse.sporhund.domain.Saksbehandler
import no.nav.helse.sporhund.infrastructure.api.testhelpers.lagTestSaksbehandler
import no.nav.helse.sporhund.infrastructure.api.testhelpers.setupTestApp
import no.nav.security.mock.oauth2.MockOAuth2Server
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class EndepunktTest {
    protected val mockOAuth2Server = MockOAuth2Server()
    protected val saksbehandler: Saksbehandler = lagTestSaksbehandler()
    protected val personPseudoIdProvider = InMemoryPersonPseudoIdProvider()
    protected val transactionProvider = InMemoryTransactionProvider()
    protected val populasjonstilgangskontrollProvider = InMemoryPopulasjonstilgangskontrollProvider()

    fun ApplicationTestBuilder.setupDefaultTestApp() {
        setupTestApp(personPseudoIdProvider, transactionProvider, mockOAuth2Server, populasjonstilgangskontrollProvider)
    }

    @BeforeTest
    fun setUp() {
        mockOAuth2Server.start()
    }

    @AfterTest
    fun tearDown() {
        mockOAuth2Server.shutdown()
    }
}
