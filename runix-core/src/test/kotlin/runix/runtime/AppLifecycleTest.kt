@file:Suppress("RemoveRedundantBackticks")

package runix.runtime

import kotlinx.coroutines.test.runTest
import runix.primitives.module.AppModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppLifecycleTest {

    @Test
    fun `lifecycle runs install → onStart → onStop in order`() = runTest {
        val log = mutableListOf<String>()

        val module = object : AppModule {
            override val name = "TestModule"

            override fun install(app: App) {
                log += "install"
            }

            override fun onStart() {
                log += "onStart"
            }

            override fun onStop() {
                log += "onStop"
            }
        }

        val app = App {
            install(module)
            onStart { log += "hookStart" }
            onStop { log += "hookStop" }
        }

        app.start()
        app.stop()

        assertEquals(
            listOf("install", "hookStart", "onStart", "onStop", "hookStop"),
            log
        )
    }

    @Test
    fun `only one onStart and onStop allowed`() {
        val builder = AppBuilder()

        builder.onStart {}
        assertFailsWith<IllegalStateException> {
            builder.onStart {}
        }

        builder.onStop {}
        assertFailsWith<IllegalStateException> {
            builder.onStop {}
        }
    }

    @Test
    fun `default install() in AppModule does not affect lifecycle`() = runTest {
        val calls = mutableListOf<String>()

        val module = object : AppModule {
            override val name = "SilentModule"
            override fun onStart() { calls += "start" }
            override fun onStop() { calls += "stop" }
        }

        val app = App { install(module) }

        app.start()
        app.stop()

        assertEquals(listOf("start", "stop"), calls)
    }

    @Test
    fun `module start exception still allows shutdown`() = runTest {
        val log = mutableListOf<String>()

        val broken = object : AppModule {
            override val name = "Broken"

            override fun onStart() {
                error("Boom")
            }

            override fun onStop() {
                log += "stopped"
            }
        }

        val app = App {
            install(broken)
            onStop { log += "shutdown" }
        }

        try {
            app.start()
        } catch (_: Exception) {
            // intentionally swallowing to test safe shutdown
        }

        app.stop()

        assertEquals(listOf("stopped", "shutdown"), log)
    }
}
