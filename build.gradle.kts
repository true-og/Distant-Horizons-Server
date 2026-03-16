import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    java
    eclipse
    id("com.gradleup.shadow") version "8.3.9"
}

group = "no.jckf.dhs"
version = "0.11.1"
description = "Distant Horizons Support"

java {
    sourceCompatibility = JavaVersion.VERSION_17 // Compile with JDK 17 compatibility.
    toolchain { // Select Java toolchain.
        languageVersion.set(JavaLanguageVersion.of(17)) // Use JDK 17.
        vendor.set(JvmVendorSpec.GRAAL_VM) // Use GraalVM CE.
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.purpurmc.org/snapshots")
    maven("https://jitpack.io/")
}

val shade by configurations.creating
configurations.named("implementation") {
    extendsFrom(shade)
}

dependencies {
    compileOnly("org.jetbrains:annotations:25.0.0")
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("com.google.guava:guava:33.3.1-jre")

    shade("org.json:json:20240303")
    shade("org.tukaani:xz:1.10")
    shade("com.github.technicallycoded:FoliaLib:v0.4.3")

    testImplementation("junit:junit:4.11")
}

tasks.named<ProcessResources>("processResources") {
    filesMatching("plugin.yml") {
        expand(
            mapOf(
                "version" to project.version.toString(),
                "mainClass" to "no.jckf.dhsupport.bukkit.DhSupportBukkitPlugin",
                "mcApiVersion" to "1.19"
            )
        )
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("Distant-Horizons-Server.jar")

    configurations = listOf(project.configurations.getByName("shade"))

    relocate("org.bstats", "no.jckf.dhsupport.bstats")
    relocate("org.json", "no.jckf.dhsupport.json")
    relocate("org.tukaani.xz", "no.jckf.dhsupport.xz")
    relocate("com.tcoded.folialib", "no.jckf.dhsupport.folialib")

    exclude("org/jetbrains/**")
    exclude("org/intellij/**")
}

tasks.named("build") {
    dependsOn("shadowJar")
}
