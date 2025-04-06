package runix.primitives

interface ActionError {
    val code: String // machine-parsable
    val message: String // human-readable
}
