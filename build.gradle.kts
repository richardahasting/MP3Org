plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "org.hasting"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("commons-fileupload:commons-fileupload:1.5")

    // Include all JARs from the lib directory
    implementation(files("lib").asFileTree.matching {
        include("**/*.jar")
    })

    // JUnit for testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    
    // TestFX for JavaFX UI testing
    testImplementation("org.testfx:testfx-core:4.0.16-alpha")
    testImplementation("org.testfx:testfx-junit5:4.0.16-alpha")

// https://mvnrepository.com/artifact/org/jaudiotagger
    // implementation("net.jthink:jaudiotagger:3.0.1")
    // Derby database
    implementation("org.apache.derby:derby:10.16.1.1")
    // https://mvnrepository.com/artifact/org.apache.derby/derbyclient
    implementation ("org.apache.derby:derbyclient:10.16.1.1")

    // Connection pooling
    implementation("com.zaxxer:HikariCP:5.0.1")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.4.8")

    // Apache Commons for file operations
    implementation("commons-io:commons-io:2.13.0")

    // https://mvnrepository.com/artifact/org.apache.commons/commons-text
    implementation("org.apache.commons:commons-text:1.13.1")

    // ... other dependencies
}

application {
    mainClass.set("org.hasting.MP3OrgApplication")
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens", "javafx.graphics/com.sun.glass.ui=ALL-UNNAMED"
    )
}

tasks.test {
    useJUnitPlatform()
    jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.hasting.MP3OrgApplication"
    }

    // Include dependencies in the JAR
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

