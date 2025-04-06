plugins {
    id("java")
    id("application")
}

repositories {
    mavenCentral()
}

dependencies {
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("JavaCheck")
}

tasks.jar {
    manifest {
        attributes("Main-Class" to application.mainClass.get())
    }
}