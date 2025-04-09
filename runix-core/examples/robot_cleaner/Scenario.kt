package robot_cleaner.scenario

import robot_cleaner.state.CleaningState
import robot_cleaner.state.ObstacleState
import robot_cleaner.state.PowerState
import robot_cleaner.state.UserState
import runix.simulation.Simulation

fun simulateRobot(): Simulation {
    return Simulation().apply {
        // Simulate power and battery
        schedule(PowerState.batteryLevel, "batteryLevel") {
            at(0, 100)
            at(5000, 25)
            at(8000, 15)
            at(20000, 10) // triggers return
        }

        schedule(PowerState.isCharging, "isCharging") {
            at(0, false)
            at(25000, true) // docked
        }

        // Simulate bin fill-up
        schedule(CleaningState.binCapacity, "binCapacity") {
            at(0, 0)
            at(6000, 30)
            at(12000, 90) // full → trigger emptyBin
        }

        // Simulate obstacles
        schedule(ObstacleState.obstacleDetected, "obstacleDetected") {
            at(0, false)
            at(10000, true)
            at(13000, false)
        }

        schedule(ObstacleState.isWheelSlipping, "isWheelSlipping") {
            at(0, false)
            at(11000, true)
            at(12500, false)
        }

        // Simulate user presence
        schedule(UserState.userPresent, "userPresent") {
            at(0, true)
            at(20000, false)
            at(40000, true)
        }
    }
}