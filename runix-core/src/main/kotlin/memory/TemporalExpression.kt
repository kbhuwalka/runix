package runix.memory

typealias TemporalExpression = () -> Boolean

operator fun TemporalExpression.not(): TemporalExpression = { !this() }

infix fun TemporalExpression.and(other: TemporalExpression): TemporalExpression =
    { this() && other() }

infix fun TemporalExpression.or(other: TemporalExpression): TemporalExpression =
    { this() || other() }