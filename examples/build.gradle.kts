plugins {
    kotlin("jvm") version "1.9.10"
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    implementation(project(":runix-core"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}