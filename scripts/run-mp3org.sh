#!/bin/bash
# MP3Org Launcher Script for Unix/Linux/macOS

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
JAR_FILE="$SCRIPT_DIR/../mp3org-1.0.0.jar"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 11 or higher from https://openjdk.org/"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 11 ]; then
    echo "Error: Java 11 or higher is required"
    echo "Current Java version: $JAVA_VERSION"
    echo "Please install Java 11 or higher from https://openjdk.org/"
    exit 1
fi

# Check if JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: MP3Org JAR file not found at: $JAR_FILE"
    exit 1
fi

echo "Starting MP3Org Music Collection Manager..."
echo "JAR file: $JAR_FILE"
echo "Java version: $(java -version 2>&1 | head -n1)"
echo ""

# Run the application with proper JVM arguments
java \
    --enable-native-access=ALL-UNNAMED \
    --add-opens javafx.graphics/com.sun.glass.ui=ALL-UNNAMED \
    -Xmx2g \
    -jar "$JAR_FILE" \
    "$@"