package chase_squish

import runix.primitives.Signal

object UI {
    object Notifications {
        val BrushTemperatureTooHighToStart = object : Signal("BrushTemperatureTooHighToStart") {}
    }
}