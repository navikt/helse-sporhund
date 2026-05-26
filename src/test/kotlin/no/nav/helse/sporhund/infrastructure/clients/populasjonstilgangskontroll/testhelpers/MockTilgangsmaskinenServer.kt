package no.nav.helse.sporhund.infrastructure.clients.populasjonstilgangskontroll.testhelpers

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

/**
 * Enkel mock-server for Tilgangsmaskinen.
 * Brukes i lokal kjøring og returnerer alltid 204 (tilgang OK) for POST /komplett.
 *
 * TilgangsmaskinenClient kaller: POST $baseUrl/komplett
 */
class MockTilgangsmaskinenServer {
    private val server = HttpServer.create(InetSocketAddress(0), 0)

    val baseUrl: String get() = "http://localhost:${server.address.port}"

    init {
        server.createContext("/komplett") { exchange ->
            // Drain request body
            exchange.requestBody.use { it.readBytes() }
            // 204 = tilgang innvilget
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
        }
        server.executor = null
        server.start()
    }

    fun stop() = server.stop(0)
}
