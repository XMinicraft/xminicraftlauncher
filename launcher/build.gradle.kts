import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.time.Instant

plugins {
    id("java")
    id("application")
    id("com.gradleup.shadow") version "9.0.0-beta11"
}

group = "org.xminicraft"
version = "0.1.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    implementation("com.formdev:flatlaf:PR-988-SNAPSHOT")
    implementation("com.formdev:flatlaf-extras:PR-988-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("org.xminicraft.xminicraftlauncher.Main");
}

tasks.processResources {
    dependsOn(":java-check:jar")
    from(project(":java-check").tasks.jar.get().archiveFile) {
        into("META-INF")
        rename { "JavaCheck.jar" }
    }

    val versionProvider = provider { project.version.toString() }
    val timestampProvider = provider { Instant.now().toString() }

    inputs.property("build.time", timestampProvider)
    outputs.file("$destinationDir/build.properties")

    doLast {
        val buildPropsFile = File(destinationDir, "build.properties")
        buildPropsFile.writeText(
            """
            |name=XMinecraft Launcher
            |version=${versionProvider.get()}
            |build.time=${timestampProvider.get()}
            |""".trimMargin()
        )
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("xminicraftlauncher")
    archiveClassifier.set("")

    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Build-Time"] = Instant.now().toString()
    }

    minimize {
        exclude(dependency("com.formdev:.*:.*"))
    }
}

tasks.test {
    useJUnitPlatform()
}