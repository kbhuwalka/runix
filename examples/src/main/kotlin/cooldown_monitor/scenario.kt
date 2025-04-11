package examples.cooldown_monitor

import runix.simulation.Simulation

fun simulateScenario(app: CooldownMonitorApp): Simulation {
    return Simulation().apply {
        schedule(app.binFull, "binFull") {
            at(100, true)
            at(200, false)
            at(600, true)
            at(700, false)
            at(4000, true)
        }
    }
}
