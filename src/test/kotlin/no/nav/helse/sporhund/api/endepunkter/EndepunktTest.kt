package no.nav.helse.sporhund.api.endepunkter

import no.nav.helse.sporhund.api.testhelpers.lagTestSaksbehandler
import no.nav.helse.sporhund.domain.Saksbehandler
import no.nav.security.mock.oauth2.MockOAuth2Server
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class EndepunktTest {
    protected val mockOAuth2Server = MockOAuth2Server()
    protected val saksbehandler: Saksbehandler = lagTestSaksbehandler()

    @BeforeTest
    fun setUp() {
        mockOAuth2Server.start()
    }

    @AfterTest
    fun tearDown() {
        mockOAuth2Server.shutdown()
    }
}
