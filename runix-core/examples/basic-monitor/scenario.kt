package examples.basic_monitor

import runix.simulation.Simulation

fun simulateScenario(app: DirtDetectorApp): Simulation {
    return Simulation().apply {
        schedule(app.isDirty, "isDirty") {
            at(2000, true)
        }
    }
}