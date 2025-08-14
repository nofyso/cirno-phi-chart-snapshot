val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "1.9.0"
    id("io.ktor.plugin") version "3.2.3"
}

group = "han.cirno"
version = "0.1.0"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation("com.opencsv:opencsv:5.9")
    implementation("org.json:json:20250517")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-openapi")
    implementation("io.ktor:ktor-server-swagger")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
