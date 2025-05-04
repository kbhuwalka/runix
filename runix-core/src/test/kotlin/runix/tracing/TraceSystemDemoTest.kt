package runix.tracing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import runix.tracing.events.*
import runix.tracing.reporters.ConsoleTraceReporter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TraceSystemDemoTest {

    @BeforeEach
    fun reset() {
        TraceCollector.reset()
    }

    @Test
    fun `basic trace flow from Action lifecycle`() {
        // Register a console reporter
        TraceCollector.registerReporter(ConsoleTraceReporter)

        val context = TraceContext.root()

        val start = ActionStarted(
            traceId = context.traceId,
            componentPath = "ModuleX/ActionA",
            actionName = "MoveArm"
        )

        val end = ActionSucceeded(
            traceId = context.traceId,
            componentPath = "ModuleX/ActionA",
            result = "OK",
            durationMillis = 780
        )

        TraceCollector.emit(start)
        TraceCollector.emit(end)

        val buffered = TraceCollector.getBufferedEvents()
        assertEquals(2, buffered.size)

        assertTrue(buffered[0] is ActionStarted)
        assertTrue(buffered[1] is ActionSucceeded)

        assertEquals("MoveArm", (buffered[0] as ActionStarted).actionName)
        assertEquals("OK", (buffered[1] as ActionSucceeded).result)
    }

    @Test
    fun `linked trace flow using derived context`() {
        val parent = TraceContext.root()
        val child = parent.derive()

        val monitor = MonitorTriggered(
            traceId = parent.traceId,
            componentPath = "Safety/CollisionMonitor",
            monitorName = "NoObstacle",
            satisfiedConditions = listOf("LidarClear")
        )

        val signal = SignalEmitted(
            traceId = parent.traceId,
            componentPath = "Safety/CollisionMonitor",
            signalName = "ClearToProceed"
        )

        val reaction = ReactionTriggered(
            traceId = child.traceId,
            componentPath = "Nav/StartMove",
            reactionName = "StartMoving"
        )

        TraceCollector.emit(monitor)
        TraceCollector.emit(signal)
        TraceCollector.emit(reaction)

        val events = TraceCollector.getBufferedEvents()
        assertEquals(3, events.size)
        assertTrue(events[2].traceId != events[0].traceId)
        assertTrue((reaction.traceId == child.traceId) && (child.parentId == parent.traceId))
    }
}
