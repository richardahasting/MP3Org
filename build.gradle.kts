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

// ============================================
// Frontend Build Tasks
// ============================================

// Build the React frontend using npm
tasks.register<Exec>("buildFrontend") {
    description = "Builds the React frontend for production"
    group = "build"
    workingDir = file("frontend")
    commandLine("npm", "run", "build")

    // Only run if frontend source files have changed
    inputs.dir("frontend/src")
    inputs.file("frontend/package.json")
    inputs.file("frontend/vite.config.ts")
    outputs.dir("frontend/dist")
}

// Copy built frontend to Spring Boot static resources
tasks.register<Copy>("copyFrontend") {
    description = "Copies built frontend to Spring Boot static resources"
    group = "build"
    dependsOn("buildFrontend")
    from("frontend/dist")
    into("src/main/resources/static")
}

// Clean the copied frontend files
tasks.register<Delete>("cleanFrontend") {
    description = "Removes copied frontend files from static resources"
    group = "build"
    delete("src/main/resources/static")
}

// Make the build depend on copying the frontend
tasks.named("processResources") {
    dependsOn("copyFrontend")
}

// Clean should also clean the frontend
tasks.named("clean") {
    dependsOn("cleanFrontend")
}

// ============================================
// Native Installer Tasks (jpackage)
// ============================================

val jreModules = listOf(
    "java.base",
    "java.desktop",
    "java.instrument",
    "java.logging",
    "java.management",
    "java.naming",
    "java.net.http",
    "java.prefs",
    "java.scripting",
    "java.security.jgss",
    "java.sql",
    "jdk.crypto.ec",
    "jdk.unsupported"
).joinToString(",")

// jpackage requires version format: 1.2.3 (no -SNAPSHOT or other suffixes)
val appVersion = "2.0.0"
val jpackageDir = layout.buildDirectory.dir("jpackage").get().asFile

// Create minimal custom JRE using jlink
tasks.register<Exec>("jlink") {
    description = "Creates a minimal custom JRE using jlink"
    group = "packaging"

    val jreOutput = layout.buildDirectory.dir("custom-jre").get().asFile

    doFirst {
        delete(jreOutput)
    }

    commandLine(
        "jlink",
        "--add-modules", jreModules,
        "--strip-debug",
        "--no-man-pages",
        "--no-header-files",
        "--compress=zip-6",
        "--output", jreOutput.absolutePath
    )

    outputs.dir(jreOutput)
}

// macOS DMG installer
tasks.register<Exec>("packageMac") {
    description = "Creates macOS .dmg installer"
    group = "packaging"
    dependsOn("bootJar", "jlink")

    val customJre = layout.buildDirectory.dir("custom-jre").get().asFile
    val jarFile = "MP3Org-${version}.jar"

    doFirst {
        mkdir(jpackageDir)
    }

    commandLine(
        "jpackage",
        "--input", layout.buildDirectory.dir("libs").get().asFile.absolutePath,
        "--main-jar", jarFile,
        "--name", "MP3Org",
        "--app-version", appVersion,
        "--vendor", "MP3Org",
        "--description", "Music Collection Manager with Duplicate Detection",
        "--type", "dmg",
        "--runtime-image", customJre.absolutePath,
        "--dest", jpackageDir.absolutePath,
        "--icon", "packaging/icon.icns",
        "--mac-package-name", "MP3Org",
        "--java-options", "-Xmx512m"
    )
}

// Windows MSI installer
tasks.register<Exec>("packageWindows") {
    description = "Creates Windows .msi installer"
    group = "packaging"
    dependsOn("bootJar", "jlink")

    val customJre = layout.buildDirectory.dir("custom-jre").get().asFile
    val jarFile = "MP3Org-${version}.jar"

    doFirst {
        mkdir(jpackageDir)
    }

    commandLine(
        "jpackage",
        "--input", layout.buildDirectory.dir("libs").get().asFile.absolutePath,
        "--main-jar", jarFile,
        "--name", "MP3Org",
        "--app-version", appVersion,
        "--vendor", "MP3Org",
        "--description", "Music Collection Manager with Duplicate Detection",
        "--type", "msi",
        "--runtime-image", customJre.absolutePath,
        "--dest", jpackageDir.absolutePath,
        "--icon", "packaging/icon.ico",
        "--win-menu",
        "--win-shortcut",
        "--win-dir-chooser",
        "--java-options", "-Xmx512m"
    )
}

// Linux DEB package
tasks.register<Exec>("packageLinuxDeb") {
    description = "Creates Linux .deb package"
    group = "packaging"
    dependsOn("bootJar", "jlink")

    val customJre = layout.buildDirectory.dir("custom-jre").get().asFile
    val jarFile = "MP3Org-${version}.jar"

    doFirst {
        mkdir(jpackageDir)
    }

    commandLine(
        "jpackage",
        "--input", layout.buildDirectory.dir("libs").get().asFile.absolutePath,
        "--main-jar", jarFile,
        "--name", "mp3org",
        "--app-version", appVersion,
        "--vendor", "MP3Org",
        "--description", "Music Collection Manager with Duplicate Detection",
        "--type", "deb",
        "--runtime-image", customJre.absolutePath,
        "--dest", jpackageDir.absolutePath,
        "--icon", "packaging/icon.png",
        "--linux-shortcut",
        "--linux-menu-group", "AudioVideo",
        "--java-options", "-Xmx512m"
    )
}

// Linux RPM package
tasks.register<Exec>("packageLinuxRpm") {
    description = "Creates Linux .rpm package"
    group = "packaging"
    dependsOn("bootJar", "jlink")

    val customJre = layout.buildDirectory.dir("custom-jre").get().asFile
    val jarFile = "MP3Org-${version}.jar"

    doFirst {
        mkdir(jpackageDir)
    }

    commandLine(
        "jpackage",
        "--input", layout.buildDirectory.dir("libs").get().asFile.absolutePath,
        "--main-jar", jarFile,
        "--name", "mp3org",
        "--app-version", appVersion,
        "--vendor", "MP3Org",
        "--description", "Music Collection Manager with Duplicate Detection",
        "--type", "rpm",
        "--runtime-image", customJre.absolutePath,
        "--dest", jpackageDir.absolutePath,
        "--icon", "packaging/icon.png",
        "--linux-shortcut",
        "--linux-menu-group", "AudioVideo",
        "--java-options", "-Xmx512m"
    )
}

// Clean jpackage output
tasks.register<Delete>("cleanPackaging") {
    description = "Removes jpackage output and custom JRE"
    group = "packaging"
    delete(jpackageDir)
    delete(layout.buildDirectory.dir("custom-jre"))
}
