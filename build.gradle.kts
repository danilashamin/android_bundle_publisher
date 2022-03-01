import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    application
}

group = "me.danilashamin.bundle_upload"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.auth:google-auth-library-oauth2-http:0.22.0")
    implementation("com.google.apis:google-api-services-androidpublisher:v3-rev20211125-1.32.1")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}