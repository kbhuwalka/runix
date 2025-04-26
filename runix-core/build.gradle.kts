plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

group = "com.runix"
version = "1.0.0"

repositories {
    mavenCentral()
}

sourceSets["main"].kotlin.srcDirs("src/main/kotlin")

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain(20)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    manifest {
        attributes["Implementation-Title"] = "Runix"
        attributes["Implementation-Version"] = version
    }
}

tasks.register("buildJar", Jar::class) {
    archiveBaseName.set("runix-core")
    from(sourceSets["main"].output)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "20"
        freeCompilerArgs = listOf(
            "-Xjvm-default=all",
            "-Xinline-classes",
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("runix-core-all")
    archiveClassifier.set("")
    archiveVersion.set(version.toString())
}

tasks.register<Exec>("runProguard") {
    dependsOn("buildJar")

    val inputJar = file("build/libs/runix-core-${project.version}.jar")
    val outputJar = file("build/libs/runix-core-obfuscated-${project.version}.jar")
    val proguardJar = file("libs/proguard/lib/proguard.jar") // update if needed

    val runtimeClasspath = configurations.runtimeClasspath.get().files

    doFirst {
        println("🔐 Running ProGuard on $inputJar")
        val argsList = mutableListOf(
            "-jar",
            proguardJar.absolutePath,
            "@proguard.pro",
            "-injars",
            inputJar.absolutePath,
            "-outjars",
            outputJar.absolutePath
        )

        // Dynamically add all runtime dependencies as library jars
        runtimeClasspath.forEach { lib ->
            argsList.add("-libraryjars")
            argsList.add(lib.absolutePath)
        }

        argsList.add("-libraryjars")
        argsList.add("${System.getProperty("java.home")}/jmods")

        commandLine("java", *argsList.toTypedArray())
    }
}

tasks.register("packageAll") {
    group = "build"
    description = "Builds core, fat, and obfuscated Runix JARs"

    dependsOn("buildJar")
    dependsOn("shadowJar")
    dependsOn("runProguard")

    doLast {
        println("✅ All Runix JARs created in build/libs:")
        println("• runix-core.jar")
        println("• runix-core-all-1.0.0.jar")
        println("• runix-core-obfuscated.jar")
    }
}
