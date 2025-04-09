package runix.dsl

import runix.primitives.*
import runix.tracing.ExecutionTrace
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ActionBuilder internal constructor(val name: String) {
    var timeout: Duration = 5.seconds
    var conflictPolicy: ConflictPolicy = ConflictPolicy.Allow
    var cancelOn: List<Signal> = emptyList()
    var actor: String? = null
    var tags: List<String> = emptyList()

    lateinit var executeBlock: suspend ActionContext.() -> ActionResult

    var onComplete: suspend (ActionResult, ExecutionTrace) -> Unit = { _, _ -> }

    fun onExecute(block: suspend ActionContext.() -> ActionResult) {
        this.executeBlock = block
    }

    fun build(): Action {
        val builder = this
        return object : Action(name) {
            override val timeout = builder.timeout
            override val conflictPolicy = builder.conflictPolicy
            override val cancelOn = builder.cancelOn
            override val actor = builder.actor ?: name
            override val tags = builder.tags

            override suspend fun onExecute(context: ActionContext): ActionResult {
                return builder.executeBlock(context)
            }

            override suspend fun onComplete(result: ActionResult, trace: ExecutionTrace) {
                builder.onComplete(result, trace)
            }
        }
    }
}

fun action(name: String, block: ActionBuilder.() -> Unit): Action {
    return ActionBuilder(name).apply(block).build()
}