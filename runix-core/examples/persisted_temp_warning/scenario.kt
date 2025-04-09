package examples.persisted_temp_warning

import runix.simulation.Simulation

fun simulateScenario(app: PersistedTempApp): Simulation {
    return Simulation().apply {
        schedule(app.temperature, "temperature") {
            at(0, 75f)
            at(1000, 90f)   // starts hot
            at(3000, 80f)   // breaks persistence
            at(4000, 88f)   // hot again
            at(10000, 70f)  // ends persistence after 6s
        }
    }
}