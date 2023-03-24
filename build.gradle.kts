import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val verKotlin = "1.4.31"
    val verSpring = "3.0.5"
    val verSpringManagement = "1.0.11.RELEASE"

    id("org.jetbrains.kotlin.jvm") version "1.6.21"
    kotlin("plugin.spring") version verKotlin
    id("org.springframework.boot") version verSpring
    id("io.spring.dependency-management") version verSpringManagement
}

object Versions {
    const val JUNIT = "5.9.2"
    const val MOCKK = "1.13.4"
    const val SPRING_MOCKK = "3.0.1"
    const val REST_ASSURED = "5.3.0"
    const val ASSERTJ = "3.24.2"
    const val ARROW = "1.1.3"
    const val FAKER = "1.0.2"
    const val KOTEST_ASSERTIONS = "5.5.4"
    const val MICROMETER = "1.10.5"
    const val OKHTTP = "4.10.0"
    const val JACKSON = "2.14.1"
    const val WIREMOCK = "3.0.0-beta-4"
    const val KMONGO = "4.8.0"
    const val TESTCONTAINERS = "1.17.6"
    const val SPRING_KAFKA = "3.0.5"
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.arrow-kt:arrow-core:${Versions.ARROW}")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-undertow")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka:${Versions.SPRING_KAFKA}")
    implementation("io.micrometer:micrometer-registry-datadog:${Versions.MICROMETER}")
    implementation("com.squareup.okhttp3:okhttp:${Versions.OKHTTP}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${Versions.JACKSON}")
    implementation("com.fasterxml.jackson.core:jackson-core:${Versions.JACKSON}")
    implementation("com.fasterxml.jackson.core:jackson-annotations:${Versions.JACKSON}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.JACKSON}")
    implementation("org.litote.kmongo:kmongo:${Versions.KMONGO}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test"){
        exclude(module = "mockito-core")
    }
    testImplementation(group = "io.mockk", name = "mockk", version = Versions.MOCKK)
    testImplementation(group = "com.ninja-squad", name = "springmockk", version = Versions.SPRING_MOCKK)
    testImplementation("com.github.tomakehurst:wiremock:${Versions.WIREMOCK}")
    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.JUNIT}")
    testImplementation(group = "org.assertj", name = "assertj-core", version = Versions.ASSERTJ)
    testImplementation(group = "io.kotest", name = "kotest-assertions-core-jvm", version = Versions.KOTEST_ASSERTIONS)
    testImplementation(group= "com.github.javafaker", name= "javafaker", version= Versions.FAKER) {
        exclude(group = "org.yaml")
    }
    testImplementation(group =  "org.testcontainers", name = "testcontainers", version = Versions.TESTCONTAINERS)
    testImplementation(group =  "org.testcontainers", name = "mongodb", version = Versions.TESTCONTAINERS)
    testImplementation(group =  "org.testcontainers", name = "kafka", version = Versions.TESTCONTAINERS)
    testImplementation(group = "io.rest-assured", name = "rest-assured", version = Versions.REST_ASSURED)
}

tasks.apply {
    test {
        maxParallelForks = 1
        enableAssertions = true
        useJUnitPlatform {}
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict", "-Xinline-classes")
        }
    }

    task<Test>("unitTest") {
        description = "Runs unit tests."
        useJUnitPlatform {
            excludeTags("integration")
            excludeTags("acceptance")
        }
        shouldRunAfter(test)
    }

    task<Test>("integrationTest") {
        description = "Runs integration tests."
        useJUnitPlatform {
            includeTags("integration")
        }
        shouldRunAfter(test)
    }

    task<Test>("acceptanceTest") {
        description = "Runs acceptance tests."
        useJUnitPlatform {
            includeTags("acceptance")
        }
        shouldRunAfter(test)
    }
}

configurations {
    all {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
}
