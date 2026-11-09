package no.nav.helse.sporhund.infrastructure.clients.sprinter.testhelpers

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Enkel mock-server for Sprinter (PDF-generering).
 * Svarer med en egen dummy-PDF per mal, så testene kan se hvilken mal PDF-en kom fra.
 */
class MockSprinterServer {
    private val server = HttpServer.create(InetSocketAddress(0), 0)
    private val forespørsler = CopyOnWriteArrayList<Pair<String, String>>()

    val baseUrl: String get() = "http://localhost:${server.address.port}"

    /** Request-bodyene (JSON) som er sendt til [mal]. */
    fun forespørslerTil(mal: String): List<String> = forespørsler.filter { it.first == mal }.map { it.second }

    init {
        PDF_PER_MAL.forEach { (mal, pdf) ->
            server.createContext("/api/v1/genpdf/sporhund/$mal") { exchange ->
                forespørsler.add(mal to exchange.requestBody.use { it.readBytes() }.decodeToString())
                exchange.responseHeaders.add("Content-Type", "application/pdf")
                exchange.sendResponseHeaders(200, pdf.size.toLong())
                exchange.responseBody.use { it.write(pdf) }
            }
        }
        server.executor = null
        server.start()
    }

    fun stop() = server.stop(0)

    companion object {
        const val MELDING_TIL_BEHANDLER = "melding_til_behandler"
        const val PURRING_TIL_BEHANDLER = "purring_til_behandler"

        val PDF_PER_MAL =
            mapOf(
                MELDING_TIL_BEHANDLER to "%PDF-1.4 mock".toByteArray(),
                PURRING_TIL_BEHANDLER to "%PDF-1.4 purring mock".toByteArray(),
            )
    }
}
