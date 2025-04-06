package runix.primitives

enum class ConflictPolicy {
    Allow,
    SkipIfRunning,
    CancelPrevious
}
