package runix.tracing

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import runix.tracing.events.*
import runix.tracing.reporters.TraceReporter
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TraceCollectorTests {

    @BeforeEach
    fun resetCollector() {
        TraceCollector.reset()
    }

    @Test
    fun `emitting events adds them to buffer`() {
        val event = ActionStarted(
            componentPath = "ModuleA/Action1",
            actionName = "StartCleaning"
        )

        TraceCollector.emit(event)

        val buffered = TraceCollector.getBufferedEvents()
        assertEquals(1, buffered.size)
        assertEquals(event, buffered.first())
    }

    @Test
    fun `multiple events are recorded in order`() {
        val events = listOf(
            ActionStarted(componentPath = "ModuleX/Action1", actionName = "First"),
            ActionSucceeded(
                traceId = UUID.randomUUID(),
                componentPath = "ModuleX/Action1",
                result = "ok",
                durationMillis = 123
            ),
            ActionFailed(
                traceId = UUID.randomUUID(),
                componentPath = "ModuleX/Action2",
                reason = "HardwareError",
                durationMillis = 320
            )
        )

        events.forEach { TraceCollector.emit(it) }

        val buffered = TraceCollector.getBufferedEvents()
        assertEquals(3, buffered.size)
        assertEquals(events[0], buffered[0])
        assertEquals(events[1], buffered[1])
        assertEquals(events[2], buffered[2])
    }

    @Test
    fun `reporter receives emitted events`() {
        var received: TraceEvent? = null
        val reporter = TraceReporter { event -> received = event }

        TraceCollector.registerReporter(reporter)

        val event = ActionCancelled(
            traceId = UUID.randomUUID(),
            componentPath = "ModuleY/Action1",
            reason = "Aborted"
        )

        TraceCollector.emit(event)

        assertEquals(event, received)
    }

    @Test
    fun `buffer is trimmed to max size`() {
        // Emit more events than the max buffer size (5000)
        repeat(6000) { i ->
            TraceCollector.emit(
                ActionStarted(
                    componentPath = "M$i",
                    actionName = "A$i"
                )
            )
        }

        val buffered = TraceCollector.getBufferedEvents()
        assertTrue(buffered.size <= 5000)
        assertTrue(buffered.first().componentPath.contains("M1000"))
    }

    @Test
    fun `reset clears buffer and reporters`() {
        val dummyEvent = ActionStarted(componentPath = "Z/ActionZ", actionName = "Zed")
        val dummyReporter = TraceReporter { }

        TraceCollector.emit(dummyEvent)
        TraceCollector.registerReporter(dummyReporter)

        assertTrue(TraceCollector.getBufferedEvents().isNotEmpty())

        TraceCollector.reset()

        assertEquals(0, TraceCollector.getBufferedEvents().size)
        TraceCollector.emit(dummyEvent) // no reporter should be triggered, no error
    }
}