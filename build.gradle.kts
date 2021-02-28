// https://github.com/Kotlin/kotlinx.atomicfu#jvm
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:${Versions.ATOMIC_FU}")
    }
}

plugins {
    idea
    java
    kotlin("jvm") version Versions.KOTLIN apply false
}

allprojects {
    group = "org.example"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("idea")
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
    }

    dependencies {
        // kotlin
        implementation(kotlin("stdlib"))

        // lincheck
        implementation("org.jetbrains.kotlinx:lincheck:${Versions.LINCHECK}")

        // test
        implementation("org.assertj:assertj-core:3.11.1")
        testImplementation(platform("org.junit:junit-bom:5.7.1"))
        testImplementation("org.junit.jupiter:junit-jupiter")
    }

    tasks.withType<Test> {
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
}
