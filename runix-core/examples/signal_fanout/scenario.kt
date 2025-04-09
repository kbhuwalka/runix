package examples.signal_fanout

import runix.simulation.Simulation

fun simulateScenario(app: SignalFanoutApp): Simulation {
    return Simulation().apply {
        schedule(app.batteryLevel, "batteryLevel") {
            at(0, 100)     // normal
            at(1000, 30)   // approaching low
            at(2000, 15)   // below threshold → triggers lowBatteryDetected
            at(6000, 25)   // recovers
            at(7000, 15)   // drops again → should re-trigger
        }

        schedule(app.isCharging, "isCharging") {
            at(0, false)
            at(1500, true)  // charging when lowBattery triggers at 2000
            at(2500, false) // not charging when battery recovers
        }

        schedule(app.isDocked, "isDocked") {
            at(0, false)
            at(7500, true)  // becomes docked before second trigger
        }
    }
}