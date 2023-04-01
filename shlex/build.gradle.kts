plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "1.8.10"
    `maven-publish`
    id("maven_publish.shlex_publish")
}

group = "com.github.klee0kai.shlex"


val sourceJar = project.tasks.register("sourceJar", Jar::class.java) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

kotlin {
    jvmToolchain(8)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}
