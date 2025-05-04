//@file:Suppress("RemoveRedundantBackticks")
//
//package runix.runtime
//
//import kotlinx.coroutines.test.TestScope
//import kotlinx.coroutines.test.runTest
//import org.junit.jupiter.api.AfterEach
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import runix.primitives.module.AppModule
//import runix.primitives.signal.SignalHandle
//import runix.runtime.internal.RuntimeScope
//import kotlin.test.assertEquals
//import kotlin.test.assertTrue
//
//class AppLifecycleTest {
//
//    // Test coroutine scope for controlling async operations
//    private val testScope = TestScope()
//
//    @BeforeEach
//    fun setup() {
//        // Install test scope before each test
//        RuntimeScope.install(testScope)
//    }
//
//    @AfterEach
//    fun tearDown() {
//        // Clean up after each test
//        RuntimeScope.clear()
//    }
//
//    @Test
//    fun `lifecycle runs install → didStart → willStop in order`() = testScope.runTest {
//        val events = mutableListOf<String>()
//
//        // Create a test module that logs lifecycle events
//        class TestModule : AppModule {
//            override val name = "TestModule"
//
//            init {
//                events.add("module-init")
//
//                // Call defineBehavior method in init
//                defineBehavior {
//                    // No primitives to register
//                }
//
//                // Set lifecycle hooks
//                didStart {
//                    events.add("module-didStart")
//                }
//
//                willStop {
//                    events.add("module-willStop")
//                }
//            }
//        }
//
//        // Create app using constructor approach
//        val app = App {
//            events.add("app-init")
//
//            // Install module
//            install(TestModule())
//
//            // Set lifecycle hooks
//            didStart {
//                events.add("app-didStart")
//            }
//
//            willStop {
//                events.add("app-willStop")
//            }
//        }
//
//        // Start app
//        events.add("before-activate")
//        app.start()
//        events.add("after-activate")
//
//        // Stop app
//        events.add("before-deactivate")
//        app.stop()
//        events.add("after-deactivate")
//
//        // Verify correct order of events
//        assertEquals(
//            listOf(
//                "module-init",
//                "app-init",
//                "before-activate",
//                "module-didStart",
//                "app-didStart",
//                "after-activate",
//                "before-deactivate",
//                "app-willStop",
//                "module-willStop",
//                "after-deactivate"
//            ),
//            events
//        )
//    }
//
//    @Test
//    fun `multiple modules are activated and deactivated in correct order`() = testScope.runTest {
//        val events = mutableListOf<String>()
//
//        // Create test modules
//        class FirstModule : AppModule {
//            override val name = "FirstModule"
//
//            init {
//                didStart { events.add("first-start") }
//                willStop { events.add("first-stop") }
//            }
//        }
//
//        class SecondModule : AppModule {
//            override val name = "SecondModule"
//
//            init {
//                didStart { events.add("second-start") }
//                willStop { events.add("second-stop") }
//            }
//        }
//
//        // Create app with modules
//        val app = App {
//            install(FirstModule())
//            install(SecondModule())
//        }
//
//        // Run lifecycle
//        app.start()
//        app.stop()
//
//        // Verify modules start in installation order
//        assertEquals(
//            listOf("first-start", "second-start", "second-stop", "first-stop"),
//            events,
//            "Modules should start in installation order but stop in reverse order"
//        )
//    }
//
//    @Test
//    fun `module behavior registration works correctly`() = testScope.runTest {
//        val registeredItems = mutableListOf<String>()
//
//        // Create a module with behavior registration
//        class TestModule : AppModule {
//            override val name = "TestModule"
//
//            // Mock signal
//            private val testSignal = SignalHandle<Unit>("testSignal")
//
//            init {
//                // Define behavior
//                defineBehavior {
//                    // Mock primitives registration
//                    object {
//                        override fun toString() = "monitor1"
//                    }.also { registeredItems.add(it.toString()) }
//
//                    object {
//                        override fun toString() = "reaction1"
//                    }.also { registeredItems.add(it.toString()) }
//
//                    object {
//                        override fun toString() = "action1"
//                    }.also { registeredItems.add(it.toString()) }
//                }
//            }
//        }
//
//        // Create and activate the app
//        val app = App {
//            install(TestModule())
//        }
//
//        app.start()
//
//        // Verify all behaviors were registered
//        assertEquals(
//            listOf("monitor1", "reaction1", "action1"),
//            registeredItems,
//            "All primitives should be registered during defineBehavior"
//        )
//    }
//
//    @Test
//    fun `module activation exception still allows proper shutdown`() = testScope.runTest {
//        val events = mutableListOf<String>()
//
//        // Create a broken module that throws during activation
//        class BrokenModule : AppModule {
//            override val name = "BrokenModule"
//
//            init {
//                didStart {
//                    events.add("broken-module-start-attempt")
//                    error("Simulated failure during activation")
//                }
//
//                willStop {
//                    events.add("broken-module-stop")
//                }
//            }
//        }
//
//        // Create a normal module
//        class WorkingModule : AppModule {
//            override val name = "WorkingModule"
//
//            init {
//                didStart { events.add("working-module-start") }
//                willStop { events.add("working-module-stop") }
//            }
//        }
//
//        // Create app with both modules
//        val app = App {
//            install(WorkingModule())
//            install(BrokenModule())
//
//            didStart { events.add("app-start") }
//            willStop { events.add("app-stop") }
//        }
//
//        // Start app, catching the expected exception
//        try {
//            app.start()
//        } catch (e: Exception) {
//            events.add("caught-${e.message?.substringBefore(':')}")
//        }
//
//        // Now shutdown - this should still work correctly
//        app.stop()
//
//        // Verify events
//        assertTrue(events.contains("broken-module-start-attempt"),
//                   "Broken module should attempt to start")
//        assertTrue(events.contains("caught-Simulated failure"),
//                   "Exception should be caught")
//        assertTrue(events.contains("working-module-stop"),
//                   "Working module should still stop")
//        assertTrue(events.contains("broken-module-stop"),
//                   "Broken module should still stop")
//        assertTrue(events.contains("app-stop"),
//                   "App should stop cleanly")
//    }
//
//    @Test
//    fun `app runtime manages app lifecycle correctly`() = testScope.runTest {
//        val events = mutableListOf<String>()
//
//        // Create a test app that logs lifecycle events
//        val app = App {
//            didStart { events.add("app-start") }
//            willStop { events.add("app-stop") }
//        }
//
//        // Start through AppRuntime
//        AppRuntime.start(app)
//
//        assertTrue(events.contains("app-start"),
//                   "App should be started by AppRuntime")
//
//        // Stop through AppRuntime
//        AppRuntime.stop(app)
//
//        assertTrue(events.contains("app-stop"),
//                   "App should be stopped by AppRuntime")
//    }
//
//    @Test
//    fun `multiple lifecycle hooks can be registered`() = testScope.runTest {
//        val events = mutableListOf<String>()
//
//        // Create app with multiple lifecycle hooks
//        val app = App {
//            // We can call the same methods multiple times in the builder
//            didStart { events.add("start-hook-1") }
//            didStart { events.add("start-hook-2") }
//
//            willStop { events.add("stop-hook-1") }
//            willStop { events.add("stop-hook-2") }
//        }
//
//        // Run lifecycle
//        app.start()
//        app.stop()
//
//        // Verify all hooks were called
//        assertTrue(events.contains("start-hook-1"))
//        assertTrue(events.contains("start-hook-2"))
//        assertTrue(events.contains("stop-hook-1"))
//        assertTrue(events.contains("stop-hook-2"))
//
//        // Check start hooks run before stop hooks
//        assertTrue(events.indexOf("start-hook-1") < events.indexOf("stop-hook-1"))
//        assertTrue(events.indexOf("start-hook-2") < events.indexOf("stop-hook-1"))
//    }
//
//    @Test
//    fun `app can be activated and deactivated multiple times`() = testScope.runTest {
//        val events = mutableListOf<String>()
//
//        // Create a simple app
//        val app = App {
//            didStart { events.add("start") }
//            willStop { events.add("stop") }
//        }
//
//        // Run through multiple lifecycle cycles
//        app.start()
//        app.stop()
//
//        app.start()
//        app.stop()
//
//        // Verify correct number of events
//        assertEquals(2, events.count { it == "start" },
//                    "Start hook should run twice")
//        assertEquals(2, events.count { it == "stop" },
//                    "Stop hook should run twice")
//
//        // Verify correct alternating pattern
//        assertEquals("start", events[0])
//        assertEquals("stop", events[1])
//        assertEquals("start", events[2])
//        assertEquals("stop", events[3])
//    }
//}