plugins {
    kotlin("jvm")
    `maven-publish`
    id("maven_publish.shlex_publish")
}

group = "com.github.klee0kai.shlex"

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
