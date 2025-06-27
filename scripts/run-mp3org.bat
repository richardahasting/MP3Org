@echo off
REM MP3Org Launcher Script for Windows

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0
set JAR_FILE=%SCRIPT_DIR%..\mp3org-1.0.0.jar

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 11 or higher from https://openjdk.org/
    pause
    exit /b 1
)

REM Check if JAR file exists
if not exist "%JAR_FILE%" (
    echo Error: MP3Org JAR file not found at: %JAR_FILE%
    pause
    exit /b 1
)

echo Starting MP3Org Music Collection Manager...
echo JAR file: %JAR_FILE%
echo.

REM Run the application with proper JVM arguments
java ^
    --enable-native-access=ALL-UNNAMED ^
    --add-opens javafx.graphics/com.sun.glass.ui=ALL-UNNAMED ^
    -Xmx2g ^
    -jar "%JAR_FILE%" ^
    %*

if %errorlevel% neq 0 (
    echo.
    echo Application exited with error code: %errorlevel%
    pause
)