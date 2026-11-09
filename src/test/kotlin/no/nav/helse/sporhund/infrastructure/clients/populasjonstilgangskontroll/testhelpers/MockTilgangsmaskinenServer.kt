package no.nav.helse.sporhund.infrastructure.clients.populasjonstilgangskontroll.testhelpers

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

/**
 * Enkel mock-server for Tilgangsmaskinen.
 * Brukes i lokal kjøring og E2E-tester og returnerer alltid 204 (tilgang OK) for
 * POST /api/v1/komplett og POST /api/v1/kjerne, som TilgangsmaskinenClient kaller.
 */
class MockTilgangsmaskinenServer {
    private val server = HttpServer.create(InetSocketAddress(0), 0)

    val baseUrl: String get() = "http://localhost:${server.address.port}"

    init {
        listOf("/api/v1/komplett", "/api/v1/kjerne").forEach { sti ->
            server.createContext(sti) { exchange ->
                exchange.requestBody.use { it.readBytes() }
                // 204 = tilgang innvilget
                exchange.sendResponseHeaders(204, -1)
                exchange.close()
            }
        }
        server.executor = null
        server.start()
    }

    fun stop() = server.stop(0)
}
