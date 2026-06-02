package no.nav.helse.sporhund.infrastructure.clients.sprinter.testhelpers

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

/**
 * Enkel mock-server for Sprinter (PDF-generering).
 * Brukes i lokal kjøring og returnerer alltid en minimal dummy-PDF for POST /api/v1/genpdf/sporhund/melding_til_behandler.
 */
class MockSprinterServer {
    private val server = HttpServer.create(InetSocketAddress(0), 0)

    val baseUrl: String get() = "http://localhost:${server.address.port}"

    init {
        server.createContext("/api/v1/genpdf/sporhund/melding_til_behandler") { exchange ->
            exchange.requestBody.use { it.readBytes() }
            // Minimal fake PDF bytes
            val fakePdf = "%PDF-1.4 mock".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/pdf")
            exchange.sendResponseHeaders(200, fakePdf.size.toLong())
            exchange.responseBody.use { it.write(fakePdf) }
        }
        server.executor = null
        server.start()
    }

    fun stop() = server.stop(0)
}
