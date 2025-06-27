plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.hasting"
version = "1.0.0"

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

// Configure Shadow JAR for distribution
tasks.shadowJar {
    archiveBaseName.set("mp3org")
    archiveVersion.set(version.toString())
    archiveClassifier.set("")
    
    manifest {
        attributes(
            "Main-Class" to "org.hasting.MP3OrgApplication",
            "Implementation-Title" to "MP3Org Music Collection Manager",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Richard Hasting"
        )
    }
    
    // Merge service files properly
    mergeServiceFiles()
    
    // Exclude signature files that can cause issues
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    
    // Handle JavaFX native libraries
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    // Set the final JAR name
    destinationDirectory.set(file("$buildDir/distributions"))
}

// Make shadowJar the default build artifact
tasks.build {
    dependsOn(tasks.shadowJar)
}

// Configure regular jar to be executable too (lightweight version)
tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.hasting.MP3OrgApplication"
    }
    archiveClassifier.set("slim")
}

// Task to run the shadow JAR for testing
tasks.register<JavaExec>("runShadowJar") {
    group = "application"
    description = "Run the application from the shadow JAR"
    dependsOn(tasks.shadowJar)
    
    classpath = files(tasks.shadowJar.get().archiveFile)
    jvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens", "javafx.graphics/com.sun.glass.ui=ALL-UNNAMED"
    )
}

// Task to create a distribution ZIP with documentation
tasks.register<Zip>("distributionZip") {
    group = "distribution"
    description = "Create a distribution ZIP with JAR and documentation"
    dependsOn(tasks.shadowJar)
    
    archiveBaseName.set("mp3org")
    archiveVersion.set(version.toString())
    destinationDirectory.set(file("$buildDir/distributions"))
    
    from(tasks.shadowJar.get().archiveFile) {
        rename { "mp3org-${version}.jar" }
    }
    from("README.md")
    from("LICENSE")
    from("MP3Org_User_Guide.md")
    
    // Add a simple run script
    from(file("scripts")) {
        include("**")
        into("scripts")
    }
}

// Task to verify the shadow JAR
tasks.register<JavaExec>("verifyShadowJar") {
    group = "verification"
    description = "Verify that the shadow JAR can start successfully"
    dependsOn(tasks.shadowJar)
    
    classpath = files(tasks.shadowJar.get().archiveFile)
    mainClass.set("org.hasting.MP3OrgApplication")
    jvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens", "javafx.graphics/com.sun.glass.ui=ALL-UNNAMED",
        "-Djava.awt.headless=true"  // For CI environments
    )
    args = listOf("--version")  // Add version flag if you implement it
    
    doFirst {
        println("Verifying shadow JAR: ${tasks.shadowJar.get().archiveFile.get()}")
    }
}

