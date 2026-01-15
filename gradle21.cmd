@echo off
setlocal enabledelayedexpansion

REM ============================================================
REM Gradle wrapper that automatically locates and uses Java 21
REM For Windows Command Prompt and PowerShell
REM ============================================================

set "JAVA21_HOME="

REM Check common Java 21 installation locations
REM Eclipse Adoptium / Temurin
for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*") do (
    if exist "%%D\bin\java.exe" set "JAVA21_HOME=%%D"
)

REM Oracle JDK
if not defined JAVA21_HOME (
    for /d %%D in ("%ProgramFiles%\Java\jdk-21*") do (
        if exist "%%D\bin\java.exe" set "JAVA21_HOME=%%D"
    )
)

REM Microsoft Build of OpenJDK
if not defined JAVA21_HOME (
    for /d %%D in ("%ProgramFiles%\Microsoft\jdk-21*") do (
        if exist "%%D\bin\java.exe" set "JAVA21_HOME=%%D"
    )
)

REM Amazon Corretto
if not defined JAVA21_HOME (
    for /d %%D in ("%ProgramFiles%\Amazon Corretto\jdk21*") do (
        if exist "%%D\bin\java.exe" set "JAVA21_HOME=%%D"
    )
)

REM Azul Zulu
if not defined JAVA21_HOME (
    for /d %%D in ("%ProgramFiles%\Zulu\zulu-21*") do (
        if exist "%%D\bin\java.exe" set "JAVA21_HOME=%%D"
    )
)

REM BellSoft Liberica
if not defined JAVA21_HOME (
    for /d %%D in ("%ProgramFiles%\BellSoft\LibericaJDK-21*") do (
        if exist "%%D\bin\java.exe" set "JAVA21_HOME=%%D"
    )
)

REM Check Program Files (x86) as fallback
if not defined JAVA21_HOME (
    for /d %%D in ("%ProgramFiles(x86)%\Eclipse Adoptium\jdk-21*") do (
        if exist "%%D\bin\java.exe" set "JAVA21_HOME=%%D"
    )
)

if not defined JAVA21_HOME (
    for /d %%D in ("%ProgramFiles(x86)%\Java\jdk-21*") do (
        if exist "%%D\bin\java.exe" set "JAVA21_HOME=%%D"
    )
)

REM Check common alternative locations
if not defined JAVA21_HOME (
    for /d %%D in ("C:\Java\jdk-21*") do (
        if exist "%%D\bin\java.exe" set "JAVA21_HOME=%%D"
    )
)

if not defined JAVA21_HOME (
    for /d %%D in ("C:\jdk-21*") do (
        if exist "%%D\bin\java.exe" set "JAVA21_HOME=%%D"
    )
)

REM Check if Java 21 was found
if not defined JAVA21_HOME (
    echo ===========================================================
    echo   ERROR: Java 21 is required but not found
    echo ===========================================================
    echo.
    echo Please install Java 21 using one of these methods:
    echo.
    echo   Adoptium ^(recommended^):
    echo     https://adoptium.net/temurin/releases/?version=21
    echo.
    echo   Microsoft Build of OpenJDK:
    echo     winget install Microsoft.OpenJDK.21
    echo.
    echo   Amazon Corretto:
    echo     winget install Amazon.Corretto.21.JDK
    echo.
    echo   Oracle JDK:
    echo     https://www.oracle.com/java/technologies/downloads/#java21
    echo.
    echo Common installation paths checked:
    echo   - %ProgramFiles%\Eclipse Adoptium\jdk-21*
    echo   - %ProgramFiles%\Java\jdk-21*
    echo   - %ProgramFiles%\Microsoft\jdk-21*
    echo   - %ProgramFiles%\Amazon Corretto\jdk21*
    echo   - %ProgramFiles%\Zulu\zulu-21*
    echo.
    exit /b 1
)

REM Verify it's actually Java 21 (use temp file to handle paths with spaces)
"%JAVA21_HOME%\bin\java" -version 2>&1 | findstr /i "version" > "%TEMP%\mp3org_java_ver.txt"
set /p JAVA_VER_LINE=<"%TEMP%\mp3org_java_ver.txt"
del "%TEMP%\mp3org_java_ver.txt" 2>nul
for /f "tokens=3" %%v in ("%JAVA_VER_LINE%") do set "JAVA_VER=%%v"
set "JAVA_VER=%JAVA_VER:"=%"
for /f "tokens=1 delims=." %%m in ("%JAVA_VER%") do set "JAVA_MAJOR=%%m"

if not "%JAVA_MAJOR%"=="21" (
    echo ERROR: Found Java at %JAVA21_HOME% but it's version %JAVA_MAJOR%, not 21
    exit /b 1
)

echo Using Java 21: %JAVA21_HOME%

REM Set JAVA_HOME and run Gradle
set "JAVA_HOME=%JAVA21_HOME%"
call "%~dp0gradlew.bat" %*
