package no.nav.helse.sporhund.application

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat

class InMemoryPopulasjonstilgangskontrollProvider : PopulasjonstilgangskontrollProvider {
    override fun kontrollerTilgang(
        accessToken: String,
        fødselsnummer: String
    ): TilgangskontrollResultat = TilgangskontrollResultat.Ok
}
