package examples.basic1

import examples.examples.basic1.BasicModule
import runix.runtime.App


class RobotApp: App() {
    init {

        install(BasicModule)

        didStart {
            println("Hello World!")
        }

        willStop {
            println("Goodbye World!")
        }
    }
}