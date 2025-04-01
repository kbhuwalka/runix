plugins {
    application
    kotlin("jvm")
}

application {
    mainClass.set("example.MainKt") // change if needed
}

dependencies {
    implementation(project(":runix-core"))

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    // SLF4J + Logback Binding (actual logger implementation)
    implementation("ch.qos.logback:logback-classic:1.4.14")
    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}