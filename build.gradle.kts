import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    java
    eclipse
    id("com.gradleup.shadow") version "8.3.8"
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

val paperVersions = listOf(
    "1.16.5",
)

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io/")
}

val shade by configurations.creating
configurations.named("implementation") {
    extendsFrom(shade)
}

dependencies {
    compileOnly("org.jetbrains:annotations:25.0.0")

    implementation("com.google.guava:guava:33.3.1-jre")

    shade("org.bstats:bstats-bukkit:3.1.0")
    shade("org.json:json:20240303")
    shade("org.tukaani:xz:1.10")
    shade("com.github.technicallycoded:FoliaLib:v0.4.3")

    testImplementation("junit:junit:4.11")
}

paperVersions.forEach { mcVersion ->
    val taskSuffix = mcVersion.replace('.', '_')
    val taskName = "buildJar_$taskSuffix"
    val outputDir = file("build/libs/$mcVersion")

    val apiVersion = mcVersion.split('.').take(2).joinToString(".")

    val sourceSetName = "main"

    // Custom processResources task per version
    val processTask = tasks.register<Copy>("processResources_$taskSuffix") {
        val srcSets = project.extensions.getByName("sourceSets") as SourceSetContainer
        from(srcSets[sourceSetName].resources.srcDirs)
        into(layout.buildDirectory.dir("resources-processed/$mcVersion"))

        include("**/*.yml")

        expand(
            mapOf(
                "version" to project.version,
                "mainClass" to "no.jckf.dhsupport.bukkit.DhSupportBukkitPlugin",
                "mcApiVersion" to apiVersion,
            )
        )
    }

    configurations.maybeCreate("compileOnly$mcVersion").apply {
        isCanBeResolved = true
    }

    dependencies {
        add("compileOnly", "com.destroystokyo.paper:paper-api:${mcVersion}-R0.1-SNAPSHOT")
        //compileOnly("io.papermc.paper:paper-api:${mcVersion}-R0.1-SNAPSHOT")
    }

    tasks.register<ShadowJar>(taskName) {
        group = "build"

        archiveBaseName.set("DistantHorizonsSupport")
        archiveVersion.set(version.toString())
        destinationDirectory.set(outputDir)

        // Use processed resources as input
        // Exclude raw YAML to avoid unprocessed plugin.yml in final JAR
        val srcSets = project.extensions.getByName("sourceSets") as SourceSetContainer
        from(srcSets["main"].output) {
            exclude("**/*.yml")
        }

        from(layout.buildDirectory.dir("resources-processed/$mcVersion"))

        configurations = listOf(project.configurations.getByName("shade"))

        relocate("org.bstats", "no.jckf.dhsupport.bstats")
        relocate("org.json", "no.jckf.dhsupport.json")
        relocate("org.tukaani.xz", "no.jckf.dhsupport.xz")
        relocate("com.tcoded.folialib", "no.jckf.dhsupport.folialib")

        exclude("org/jetbrains/**")
        exclude("org/intellij/**")

        dependsOn(processTask)
    }

    tasks.named("build") {
        dependsOn(taskName)
    }

}

