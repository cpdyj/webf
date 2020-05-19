plugins {
    java
    kotlin("jvm") version "1.4-M1"
    application
}

application {
    mainClassName = "framework.bootstrap.MainKt"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    if (System.getenv("GITHUB_WORKFLOW").orEmpty().trim().isBlank()) {
        maven("https://maven.aliyun.com/repository/public")
        println("add aliyun repos")
    } else {
        println("detected Github Action, not add aliyun repos.")
    }
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    testImplementation("junit", "junit", "4.12")

    implementation("org.reflections:reflections:0.9.12")

    implementation(vertx("core"))
    implementation(vertx("lang-kotlin"))
    implementation(vertx("auth-common"))
    implementation(vertx("web"))
    implementation(vertx("pg-client"))
    implementation(vertx("lang-kotlin-coroutines"))

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")

    implementation("org.apache.logging.log4j:log4j-core:2.13.3")
    implementation("org.apache.logging.log4j:log4j-api:2.13.3")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.0.0")
//    implementation("org.slf4j:slf4j-api:1.7.3")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

fun DependencyHandler.vertx(module: String) =
    "io.vertx:vertx-$module:4.0.0-milestone4"