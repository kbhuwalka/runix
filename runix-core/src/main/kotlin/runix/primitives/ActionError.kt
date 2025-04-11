package runix.primitives

data class ActionError(
    val code: String, // machine-parsable
    val message: String
) // human-readable
