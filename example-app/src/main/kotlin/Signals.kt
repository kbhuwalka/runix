import runix.primitives.Signal


object StartDelivery: Signal("StartDelivery")

object DropFailed: Signal("DropFailed")

object DropSucceeded: Signal("DropSucceeded")

object MotorFailed: Signal("MotorFailed")

object GripperFailed: Signal("GripperFailed")

object NavigationFailed: Signal("NavigationFailed")

object FallbackNavigationFailed: Signal("FallbackNavigationFailed")
