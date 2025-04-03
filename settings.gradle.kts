pluginManagement {
    repositories {
        gradlePluginPortal()     // ✅ Needed for com.guardsquare.proguard
        mavenCentral()
        google()
    }
}

rootProject.name = "runix"

include(":runix-core")