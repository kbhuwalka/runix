package chase_squish.scenarios

import chase_squish.modules.BrushModule
import runix.core.RunixScheduler
import runix.primitives.ActionResult
import runix.simulation.Simulation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

fun simulateBrushStress(scheduler: RunixScheduler): Simulation {
    val sim = Simulation()

    // Start with a safe temperature
    sim.schedule(BrushModule.currentTemperature, "currentTemperature") {
        at(0, 70f)
    }

// Periodically attempt to start brushes if not running
    scheduler.scope.launch {
        while (true) {
            delay(8000)

            if (!BrushModule.runningState.value) {
                println("🔁 Attempting to start brushes...")

                val result = scheduler.runNowAndWait(BrushModule.Actions.startBrushes)

                when (result) {
                    is ActionResult.Success -> {
                        println("✅ Brushes started successfully: ${result.message ?: "No message"}")
                    }
                    is ActionResult.Failure -> {
                        println("❌ Failed to start brushes: ${result.error.message}")
                    }
                    is ActionResult.Cancelled -> {
                        println("⛔️ Start attempt cancelled: ${result.reason}")
                    }
                    else -> {
                        println("❓ Unexpected result: $result")
                    }
                }
            }
        }
    }

    // Simulate temperature drift based on current brush state
    scheduler.scope.launch {
        while (true) {
            delay(1000)

            val running = BrushModule.runningState.value
            val currentTemp = BrushModule.currentTemperature.value

            val tempDelta = if (running) {
                Random.nextFloat() * 4 + 2  // Heat up: 2–6°C
            } else {
                -(Random.nextFloat() * 3 + 1)  // Cool down: 1–4°C
            }

            val newTemp = (currentTemp + tempDelta).coerceIn(40f, 120f)
            BrushModule.currentTemperature.value = newTemp

            println("🌡 Temp updated to ${newTemp.format(1)}°C (running = $running)")
        }
    }

    return sim
}

private fun Float.format(digits: Int) = "%.${digits}f".format(this)