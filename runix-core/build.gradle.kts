plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.0"
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    // SLF4J + Logback Binding (actual logger implementation)
    implementation("ch.qos.logback:logback-classic:1.4.14")

    //Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}