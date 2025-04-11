package examples.stable_condition

import runix.simulation.Simulation

fun simulateScenario(app: StableConditionApp): Simulation {
    return Simulation().apply {
        schedule(app.isObstacle, "isObstacle") {
            at(0, true)
            at(2500, false) // signal stays true for 2.5s
        }
    }
}
