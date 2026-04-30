package domain

import java.util.UUID

// TODO: Denne skal nok helt sikkert hete noe annet etterhvert
@JvmInline
value class BehandlerId(
    val value: UUID,
)
