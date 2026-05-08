package no.nav.helse.sporhund.domain.testhelpers

import no.nav.helse.sporhund.domain.BehandlerRef
import java.util.UUID

fun lagBehandlerRef() = BehandlerRef(UUID.randomUUID().toString())
