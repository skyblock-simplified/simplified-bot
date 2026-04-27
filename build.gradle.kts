plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.6"
}

group = "dev.sbs"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven(url = "https://central.sonatype.com/repository/maven-snapshots")
    maven(url = "https://jitpack.io")
}

dependencies {
    // Simplified Annotations
    annotationProcessor(libs.simplified.annotations)

    // Lombok Annotations
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Tests
    testImplementation(libs.hamcrest)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)

    // Optaplanner
    implementation(libs.optaplanner.core)
    testImplementation(libs.optaplanner.benchmark)

    // Hazelcast client for the Phase 6c WriteDispatcher SDK
    implementation(libs.hazelcast)

    // Simplified infrastructure (formerly transitive via minecraft-api)
    implementation("com.github.simplified-dev:client:master-SNAPSHOT")
    implementation("com.github.simplified-dev:gson-extras:master-SNAPSHOT")
    implementation("com.github.simplified-dev:manager:master-SNAPSHOT")
    implementation("com.github.simplified-dev:scheduler:master-SNAPSHOT")

    // Split minecraft-api modules
    implementation("com.github.simplified-api:skyblock-data:master-SNAPSHOT")
    implementation("com.github.simplified-api:mojang:master-SNAPSHOT")
    implementation("com.github.skyblock-simplified:sbs-api:master-SNAPSHOT")
    implementation("com.github.simplified-api:hypixel:master-SNAPSHOT")

    // Projects
    implementation("com.github.minecraft-library:asset-renderer:master-SNAPSHOT")
    implementation("com.github.simplified-dev:discord4j-framework:master-SNAPSHOT")
}

tasks {
    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()

        manifest {
            attributes["Main-Class"] = "dev.sbs.simplifiedbot.SimplifiedBot"
        }

        exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }

    build {
        dependsOn(shadowJar)
    }
}
