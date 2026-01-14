# Building MP3Org on macOS

## Prerequisites

**Java 21** is required. Install via one of these methods:

```bash
# Homebrew (recommended)
brew install openjdk@21

# SDKMAN
sdk install java 21.0.5-tem
```

Verify installation:
```bash
/usr/libexec/java_home -v 21
```

## Build

```bash
git clone https://github.com/richardahasting/MP3Org.git
cd MP3Org
./gradle21 build -x test
```

## Run

```bash
./start
```

This starts both backend and frontend, then opens http://localhost:5173 in your browser.

Press `Ctrl+C` to stop.

## Why `gradle21` instead of `gradlew`?

The `gradle21` script automatically finds Java 21 on your system, regardless of what `JAVA_HOME` or `PATH` is set to. This avoids build failures when newer Java versions (like Java 25) are the system default.

If Java 21 is already your default, you can use `./gradlew` directly.
