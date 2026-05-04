package domain.testhelpers

import domain.BehandlerRef
import java.util.UUID

fun lagBehandlerRef() = BehandlerRef(UUID.randomUUID().toString())
