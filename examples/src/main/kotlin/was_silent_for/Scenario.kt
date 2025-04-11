package examples.user_inactivity_detection

import runix.simulation.Simulation

fun simulateScenario(app: UserInactivityApp): Simulation {
    return Simulation().apply {
        schedule(app.userPresent, "userPresent") {
            at(0, true)
            at(5000, false) // user left
            at(35000, false) // after 30s of absence → trigger
            at(40000, true) // user returns
        }
    }
}
