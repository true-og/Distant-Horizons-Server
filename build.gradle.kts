import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    java
    eclipse
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.6"
}

group = "no.jckf"
version = "0.9.0"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots") { name = "spigot" }
    maven("https://repo.papermc.io/repository/maven-public") { name = "papermc" }
    maven("https://jitpack.io") { name = "jitpack" }
}

val manifoldDep = "systems.manifold:manifold-preprocessor:2024.1.34"
val guavaDep = "com.google.guava:guava:33.3.1-jre"

data class McTarget(
    val id: String,
    val apiVersion: String,
    val apiVersionPatch: String,
    val pluginApi: String,
    val compileOnlyCoordinate: String,
    val readForgeByte: Boolean
)

val targets = listOf(
    McTarget("1.16.5", "1.16", ".5", "Bukkit-Spigot", "org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT", true),
    McTarget("1.17.1", "1.17", ".1", "Bukkit-Paper", "io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT", true),
    McTarget("1.18.2", "1.18", ".2", "Bukkit-Paper", "io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT", true),
    McTarget("1.19.2", "1.19", ".2", "Bukkit-Paper", "io.papermc.paper:paper-api:1.19.2-R0.1-SNAPSHOT", true),
    McTarget("1.19.4", "1.19", ".4", "Bukkit-Paper", "io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT", true)
)

dependencies {
    implementation("org.tukaani:xz:1.10")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("com.github.technicallycoded:FoliaLib:v0.4.3")
    compileOnly(guavaDep)
    compileOnly("org.jetbrains:annotations:25.0.0")
    compileOnly(manifoldDep)
    annotationProcessor(manifoldDep)
    testCompileOnly(manifoldDep)
    testAnnotationProcessor(manifoldDep)
    testImplementation("junit:junit:4.11")
    testImplementation(guavaDep)
    targets.forEach { compileOnly(it.compileOnlyCoordinate) }
}

tasks.named<ProcessResources>("processResources") {
    exclude("plugin.yml")
    filteringCharset = "UTF-8"
    expand(mapOf("main.class" to "no.jckf.dhsupport.bukkit.DhSupportBukkitPlugin"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    if ("-Xplugin:Manifold" !in options.compilerArgs) options.compilerArgs.add("-Xplugin:Manifold")
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-AREAD_FORGE_BYTE=false")
}

targets.forEach { target ->
    val ss = sourceSets.create("mc${target.id.replace(".", "")}") {
        java.srcDirs(sourceSets["main"].java.srcDirs)
        resources.srcDirs(sourceSets["main"].resources.srcDirs)
        compileClasspath += sourceSets["main"].compileClasspath
        runtimeClasspath += sourceSets["main"].runtimeClasspath
    }
    dependencies {
        add(ss.compileOnlyConfigurationName, target.compileOnlyCoordinate)
        add(ss.compileOnlyConfigurationName, manifoldDep)
        add(ss.annotationProcessorConfigurationName, manifoldDep)
    }
    val cap = ss.name.replaceFirstChar { it.uppercase() }
    val compileTask = tasks.register<JavaCompile>("compile${cap}Sources") {
        source = ss.java
        classpath = ss.compileClasspath
        destinationDirectory.set(layout.buildDirectory.dir("classes/${ss.name}"))
        options.compilerArgs.add("-AREAD_FORGE_BYTE=${target.readForgeByte}")
    }
    val filteredResDir = layout.buildDirectory.dir("filtered-resources/${target.id}")
    val versionStr = version.toString()
    val resTask = tasks.register<Copy>("filter${cap}Resources") {
        dependsOn(tasks.named("processResources"))
        from(sourceSets["main"].resources)
        into(filteredResDir)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            val repl = mapOf(
                "\${project.version}" to versionStr,
                "\${main.class}" to "no.jckf.dhsupport.bukkit.DhSupportBukkitPlugin",
                "\${mc.api-version}" to target.apiVersion,
                "\${mc.api-version-patch}" to target.apiVersionPatch
            )
            filter { line: String ->
                var s = line
                repl.forEach { (k, v) -> s = s.replace(k, v) }
                s
            }
        }
    }
    val plainJar = tasks.register<Jar>("jar$cap") {
        dependsOn(compileTask, resTask)
        archiveClassifier.set("plain-${target.id}")
        from(compileTask.flatMap { it.destinationDirectory })
        from(filteredResDir)
    }
    val runtimeCfg = configurations.named(ss.runtimeClasspathConfigurationName)
    val shadow = tasks.register<ShadowJar>("shadow$cap") {
        dependsOn(plainJar)
        if (target.id == "1.19.4") {
            archiveBaseName.set("Distant-Horizons-Server")
            archiveClassifier.set("")
        } else {
            archiveClassifier.set("all-${target.id}")
        }
        from(plainJar)
        configurations = listOf(runtimeCfg.get())
        exclude("META-INF/**")
        relocate("org.bstats", "no.jckf.dhsupport.bstats")
        relocate("com.tcoded.folialib", "no.jckf.dhsupport.folialib")
    }
    tasks.register<Copy>("copy${cap}Jar") {
        dependsOn(shadow)
        from(shadow)
        into(layout.projectDirectory.dir("jars"))
        rename { _ ->
            "DistantHorizonsSupport-${target.pluginApi}-${versionStr}-${target.apiVersion}${target.apiVersionPatch}.jar"
        }
    }
    tasks.named("build") { dependsOn(shadow) }
}

tasks.test { useJUnit() }
tasks.named<Delete>("clean") { delete(layout.buildDirectory, layout.projectDirectory.dir("jars")) }

