plugins {
    id("java")
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "org.hasting"
version = "2.0.0-SNAPSHOT"

// Enforce Java 21 - fail fast with clear error message
val javaVersion = JavaVersion.current()
if (javaVersion != JavaVersion.VERSION_21) {
    throw GradleException("""
        |
        |===========================================================
        |  ERROR: Java 21 is required to build this project.
        |
        |  Current version: $javaVersion
        |  Required version: 21
        |
        |  To fix this:
        |    - SDKMAN:  sdk env
        |    - jenv:    jenv local
        |    - Manual:  export JAVA_HOME=/path/to/java-21
        |===========================================================
        |""".trimMargin())
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Existing dependencies (preserved from original build)
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("commons-fileupload:commons-fileupload:1.5")

    // JAudioTagger for MP3 metadata extraction
    implementation("net.jthink:jaudiotagger:3.0.1")

    // MigLayout for Swing layouts (legacy UI code)
    implementation("com.miglayout:miglayout-swing:11.3")

    // Include any custom JARs from lib directory (log4Rich, etc.)
    implementation(fileTree(mapOf(
        "dir" to "lib",
        "include" to listOf("*.jar"),
        "exclude" to listOf("javafx*.jar", "javafx-swt.jar", "jaudiotagger*.jar", "miglayout*.jar")
    )))

    // JavaFX 21 for compiling existing UI code (will be removed after migration)
    // Using classifier-based approach for cross-platform support
    val javafxPlatform = when {
        System.getProperty("os.name").lowercase().contains("mac") -> {
            if (System.getProperty("os.arch") == "aarch64") "mac-aarch64" else "mac"
        }
        System.getProperty("os.name").lowercase().contains("win") -> "win"
        else -> "linux"
    }
    compileOnly("org.openjfx:javafx-controls:21.0.2:$javafxPlatform")
    compileOnly("org.openjfx:javafx-fxml:21.0.2:$javafxPlatform")
    compileOnly("org.openjfx:javafx-graphics:21.0.2:$javafxPlatform")
    compileOnly("org.openjfx:javafx-base:21.0.2:$javafxPlatform")

    // JavaFX for tests (legacy desktop app tests)
    testRuntimeOnly("org.openjfx:javafx-controls:21.0.2:$javafxPlatform")
    testRuntimeOnly("org.openjfx:javafx-graphics:21.0.2:$javafxPlatform")
    testRuntimeOnly("org.openjfx:javafx-base:21.0.2:$javafxPlatform")

    // SQLite database (embedded) - replaces Derby for Issue #72
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")

    // Connection pooling
    implementation("com.zaxxer:HikariCP:5.0.1")

    // Keep Derby temporarily for data migration
    implementation("org.apache.derby:derby:10.16.1.1")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.4.8")

    // Apache Commons for file operations
    implementation("commons-io:commons-io:2.13.0")
    implementation("org.apache.commons:commons-text:1.13.1")

    // JUnit for testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

springBoot {
    mainClass.set("org.hasting.MP3OrgWebApplication")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.hasting.MP3OrgWebApplication"
    }
}

// Kill previous instance before running
tasks.register<Exec>("killPrevious") {
    description = "Kills any previous instance running on port 9090"
    commandLine("sh", "-c", "lsof -ti:9090 | xargs -r kill -9 2>/dev/null || true")
    isIgnoreExitValue = true
}

tasks.named("bootRun") {
    dependsOn("killPrevious")
}
