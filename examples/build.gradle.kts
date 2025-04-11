plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

kotlin {
    jvmToolchain(20)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":runix-core"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

ktlint {
    // Optional: lint examples if you want, or skip them entirely
    filter {
        exclude { true }
    }
}