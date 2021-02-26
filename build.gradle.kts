plugins {
    kotlin("jvm") version "1.4.31"
}

// https://github.com/Kotlin/kotlinx.atomicfu#jvm
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.15.1")
    }
}

apply {
    plugin("kotlinx-atomicfu")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:lincheck:2.12")
    implementation("org.assertj:assertj-core:3.11.1")
    testImplementation(platform("org.junit:junit-bom:5.7.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    // https://github.com/Kotlin/kotlinx-lincheck#java-9-support
    jvmArgs(
        "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-exports", "java.base/jdk.internal.util=ALL-UNNAMED"
    )
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
