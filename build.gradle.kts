import com.google.protobuf.gradle.id

plugins {
    java
    application
    id("com.google.protobuf") version "0.9.4"
}

group = "pj"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // gRPC i Protobuf
    implementation("io.grpc:grpc-netty-shaded:1.58.0")
    implementation("io.grpc:grpc-protobuf:1.58.0")
    implementation("io.grpc:grpc-stub:1.58.0")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")

    // Baza danych (H2)
    implementation("com.h2database:h2:2.2.224")

    //jUnit
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.grpc:grpc-testing:1.58.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.0"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.58.0"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
