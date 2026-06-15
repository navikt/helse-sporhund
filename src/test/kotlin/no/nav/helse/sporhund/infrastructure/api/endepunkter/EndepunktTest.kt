package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.helse.sporhund.application.InMemoryPersonPseudoIdProvider
import no.nav.helse.sporhund.application.InMemoryPopulasjonstilgangskontrollProvider
import no.nav.helse.sporhund.application.InMemoryTransactionProvider
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.VedleggProvider
import no.nav.helse.sporhund.application.meldinger
import no.nav.helse.sporhund.domain.Saksbehandler
import no.nav.helse.sporhund.infrastructure.api.testhelpers.lagTestSaksbehandler
import no.nav.helse.sporhund.infrastructure.api.testhelpers.setupTestApp
import no.nav.helse.sporhund.tilgangskontroll.tilgangsgrupperTilTilganger
import no.nav.security.mock.oauth2.MockOAuth2Server
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

abstract class EndepunktTest {
    protected val mockOAuth2Server = MockOAuth2Server()
    protected val saksbehandler: Saksbehandler = lagTestSaksbehandler()
    protected val personPseudoIdProvider = InMemoryPersonPseudoIdProvider()
    protected val transactionProvider = InMemoryTransactionProvider()
    protected val populasjonstilgangskontrollProvider = InMemoryPopulasjonstilgangskontrollProvider()
    protected val tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger()
    protected var vedleggProvider: VedleggProvider = VedleggProvider { emptyList() }

    fun ApplicationTestBuilder.setupDefaultTestApp() {
        setupTestApp(personPseudoIdProvider, transactionProvider, mockOAuth2Server, populasjonstilgangskontrollProvider, tilgangsgrupperTilTilganger, vedleggProvider)
    }

    protected inline fun <reified T : OutboxMelding> assertOutboxContains() {
        val meldinger =
            transactionProvider.transaction {
                outbox.meldinger<T>()
            }
        assertEquals(1, meldinger.size)
    }

    protected fun assertEmptyOutbox() {
        val meldinger =
            transactionProvider.transaction {
                outbox.meldinger<OutboxMelding>()
            }
        assertEquals(0, meldinger.size)
    }

    protected fun testApp(block: suspend ApplicationTestBuilder.() -> Unit = { }) {
        testApplication {
            setupDefaultTestApp()
            block()
        }
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
