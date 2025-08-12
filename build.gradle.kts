plugins {
	java
	id("org.springframework.boot") version "3.2.4"
	id("io.spring.dependency-management") version "1.1.4"
	id("io.freefair.lombok") version "8.6"
}

group = "vn.jobhunter"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("com.turkraft.springfilter:jpa:3.1.7")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("com.mysql:mysql-connector-j")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.apache.poi:poi:5.2.3")
	implementation("org.apache.poi:poi-ooxml:5.2.3")
	implementation("com.opencsv:opencsv:5.8")
	implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")
	implementation("com.google.api-client:google-api-client:2.7.0")
	implementation("org.apache.tika:tika-core:2.9.1")
    implementation("org.apache.tika:tika-parsers-standard-package:2.9.1")

	//cache vs spring vs redis
	implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

	//conver jackson
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Lombok (freefair lo liệu, nhưng cứ khai báo rõ ràng cho chắc)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
	//mapstruc
	implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

	//  BẮT BUỘC khi dùng Lombok + MapStruct để tránh xung đột thứ tự processor
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	//kafka
    implementation("org.springframework.kafka:spring-kafka")
    testImplementation("org.springframework.kafka:spring-kafka-test")

	//monitoring performence
	implementation("io.micrometer:micrometer-registry-prometheus")


	//ratelimit api vs butket
	implementation("com.bucket4j:bucket4j_jdk17-core:8.14.0")
    implementation("com.bucket4j:bucket4j_jdk17-redis-common:8.14.0")
    implementation("com.bucket4j:bucket4j_jdk17-lettuce:8.14.0")



}


tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}


tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-parameters",
        "-Amapstruct.verbose=true",
        "-Amapstruct.suppressGeneratorTimestamp=true"
    ))
}

