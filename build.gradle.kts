plugins {
    java
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
}


subprojects {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
}

allprojects {
    group = "click.dailyfeed"
    version = "0.0.1-SNAPSHOT"
    description = "dailyfeed content"

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    configurations {
        compileOnly {
            extendsFrom(configurations.annotationProcessor.get())
        }
    }

    repositories {
        mavenCentral()
    }

    extra["springCloudVersion"] = "2025.0.0"

    val querydslVersion = "5.0.0:jakarta"
    val mapstructVersion = "1.5.4.Final"

    dependencyManagement {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        }
    }

    dependencies {
        implementation(project(":dailyfeed-code"))
        implementation(project(":dailyfeed-feign"))
        implementation(project(":dailyfeed-pagination-support"))
        implementation(project(":dailyfeed-redis-support"))
        implementation(project(":dailyfeed-kafka-support"))
        implementation(project(":dailyfeed-pvc-support"))
        implementation(project(":dailyfeed-deadletter-support"))

        // spring
        implementation("org.springframework.boot:spring-boot-starter-data-jpa")
        implementation("org.springframework.boot:spring-boot-starter-data-redis")
        implementation("org.springframework.boot:spring-boot-starter-validation")
        implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
        implementation("org.springframework.boot:spring-boot-starter-web")

        // kafka
        implementation("org.springframework.kafka:spring-kafka")
        testImplementation("org.springframework.kafka:spring-kafka-test")

        // jakarta
        annotationProcessor("jakarta.annotation:jakarta.annotation-api")
        annotationProcessor("jakarta.persistence:jakarta.persistence-api")

        // mapstruct
        implementation("org.mapstruct:mapstruct:${mapstructVersion}")
        annotationProcessor("org.mapstruct:mapstruct-processor:${mapstructVersion}")

        // querydsl
        implementation ("com.querydsl:querydsl-jpa:${querydslVersion}")
        annotationProcessor("com.querydsl:querydsl-apt:${querydslVersion}")

        // lombok
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")

        // springdoc
        implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.2")

        // database
        runtimeOnly("com.h2database:h2")
        runtimeOnly("com.mysql:mysql-connector-j")

        // test
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
