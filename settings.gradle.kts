pluginManagement {
    repositories {
        gradlePluginPortal()     // ✅ Needed for com.guardsquare.proguard
        mavenCentral()
        google()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "runix"

include(":runix-core")
include("examples")
