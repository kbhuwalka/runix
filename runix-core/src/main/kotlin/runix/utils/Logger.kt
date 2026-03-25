package runix.utils

import mu.KLogger
import mu.KotlinLogging

/**
 * Simplified logging utility for the Runix framework.
 * Provides easy access to component-specific loggers with
 * automatic class name detection.
 */
object Logger {
    /**
     * Get a logger for the specified class.
     * The class name is automatically extracted and used as the logger name.
     *
     * Usage:
     * ```
     * private val log = Logger.getLogger<MyClass>()
     * ```
     */
    inline fun <reified T : Any> getLogger(): KLogger {
        return KotlinLogging.logger {}
    }

    /**
     * Get a logger for the specified name.
     *
     * Usage:
     * ```
     * private val log = Logger.getLogger("ComponentName")
     * ```
     */
    fun getLogger(name: String): KLogger {
        return KotlinLogging.logger(name)
    }

    /**
     * Configure global logging settings programmatically.
     * This can be used to adjust log levels at runtime.
     */
    fun setLogLevel(packagePath: String, level: String) {
        // Using the internal Logback API to change levels programmatically
        val loggerContext = org.slf4j.LoggerFactory.getILoggerFactory() as ch.qos.logback.classic.LoggerContext
        val logger = loggerContext.getLogger(packagePath)
        logger.level = ch.qos.logback.classic.Level.valueOf(level)
    }
    
    /**
     * Global logger instance for quick access.
     * 
     * Usage:
     * ```
     * Logger.info { "Message" }
     * ```
     */
    val global: KLogger = getLogger("Global")
    
    // Convenience global methods
    fun trace(message: () -> Any?) = global.trace(message)
    fun debug(message: () -> Any?) = global.debug(message)
    fun info(message: () -> Any?) = global.info(message)
    fun warn(message: () -> Any?) = global.warn(message)
    fun error(message: () -> Any?) = global.error(message)
    fun error(throwable: Throwable, message: () -> Any?) = global.error(throwable, message)
}

/**
 * Extension function to time and log the execution of a block.
 *
 * Usage:
 * ```
 * log.timed("Operation") {
 *     // code to time
 *     result
 * }
 * ```
 */
inline fun <T> KLogger.timed(operation: String, block: () -> T): T {
    val start = System.currentTimeMillis()
    try {
        return block().also {
            val duration = System.currentTimeMillis() - start
            info { "$operation completed in $duration ms" }
        }
    } catch (e: Exception) {
        val duration = System.currentTimeMillis() - start
        error(e) { "$operation failed after $duration ms" }
        throw e
    }
}