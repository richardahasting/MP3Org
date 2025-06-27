# MP3Org Installation Guide

## Quick Installation

### Option 1: Download Pre-built JAR (Recommended)

1. **Download the latest release** from [GitHub Releases](https://github.com/richardahasting/MP3Org/releases)
2. **Download `mp3org-1.0.0.zip`** (contains JAR + documentation + run scripts)
3. **Extract the ZIP** to your desired location
4. **Run the application**:
   - **Windows**: Double-click `scripts/run-mp3org.bat`
   - **macOS/Linux**: Run `scripts/run-mp3org.sh` from terminal
   - **Any OS**: `java -jar mp3org-1.0.0.jar`

### Option 2: Direct JAR Download

1. **Download `mp3org-1.0.0.jar`** from the releases page
2. **Run with Java**:
   ```bash
   java --enable-native-access=ALL-UNNAMED \
        --add-opens javafx.graphics/com.sun.glass.ui=ALL-UNNAMED \
        -jar mp3org-1.0.0.jar
   ```

## Requirements

### System Requirements
- **Operating System**: Windows 10+, macOS 10.14+, or Linux with X11/Wayland
- **Memory**: 2GB RAM minimum, 4GB recommended for large collections
- **Storage**: 50MB for application + space for your music collection
- **Java**: Version 11 or higher (see below)

### Java Installation

MP3Org requires Java 11 or higher with JavaFX support.

#### Install Java (Choose One):

**Option A: OpenJDK (Recommended)**
- Download from [OpenJDK.org](https://openjdk.org/)
- Most recent versions include JavaFX

**Option B: Oracle JDK**
- Download from [Oracle.com](https://www.oracle.com/java/)
- Includes JavaFX support

**Option C: Package Managers**
```bash
# macOS with Homebrew
brew install openjdk

# Ubuntu/Debian
sudo apt install openjdk-17-jdk openjfx

# Fedora/RHEL
sudo dnf install java-17-openjdk javafx-runtime

# Windows with Chocolatey
choco install openjdk
```

#### Verify Java Installation:
```bash
java --version
# Should show version 11 or higher
```

## Detailed Installation Steps

### Windows Installation

1. **Install Java**:
   - Download OpenJDK from [OpenJDK.org](https://openjdk.org/)
   - Run the installer
   - Verify: Open Command Prompt and run `java --version`

2. **Download MP3Org**:
   - Download `mp3org-1.0.0.zip` from [Releases](https://github.com/richardahasting/MP3Org/releases)
   - Extract to `C:\Program Files\MP3Org\` (or your preferred location)

3. **Run MP3Org**:
   - Double-click `scripts\run-mp3org.bat`
   - Or create a desktop shortcut to the batch file

4. **Optional: Add to Start Menu**:
   - Create a shortcut to `run-mp3org.bat`
   - Move to `C:\Users\[username]\AppData\Roaming\Microsoft\Windows\Start Menu\Programs\`

### macOS Installation

1. **Install Java**:
   ```bash
   # Using Homebrew (recommended)
   brew install openjdk
   
   # Or download from OpenJDK.org
   ```

2. **Download MP3Org**:
   ```bash
   # Download and extract
   cd ~/Downloads
   curl -L -O https://github.com/richardahasting/MP3Org/releases/download/v1.0.0/mp3org-1.0.0.zip
   unzip mp3org-1.0.0.zip
   mv mp3org-1.0.0 /Applications/MP3Org
   ```

3. **Run MP3Org**:
   ```bash
   cd /Applications/MP3Org
   ./scripts/run-mp3org.sh
   ```

4. **Optional: Create Application Bundle**:
   - Use Automator to create a `.app` bundle that runs the shell script

### Linux Installation

1. **Install Java**:
   ```bash
   # Ubuntu/Debian
   sudo apt update
   sudo apt install openjdk-17-jdk openjfx
   
   # Fedora/RHEL
   sudo dnf install java-17-openjdk javafx-runtime
   
   # Arch Linux
   sudo pacman -S jdk-openjdk java-openjfx
   ```

2. **Download MP3Org**:
   ```bash
   cd ~/Downloads
   wget https://github.com/richardahasting/MP3Org/releases/download/v1.0.0/mp3org-1.0.0.zip
   unzip mp3org-1.0.0.zip
   sudo mv mp3org-1.0.0 /opt/mp3org
   ```

3. **Run MP3Org**:
   ```bash
   cd /opt/mp3org
   ./scripts/run-mp3org.sh
   ```

4. **Optional: Create Desktop Entry**:
   ```bash
   # Create desktop entry
   cat > ~/.local/share/applications/mp3org.desktop << EOF
   [Desktop Entry]
   Name=MP3Org
   Comment=Music Collection Manager
   Exec=/opt/mp3org/scripts/run-mp3org.sh
   Icon=/opt/mp3org/icon.png
   Terminal=false
   Type=Application
   Categories=AudioVideo;Music;
   EOF
   ```

## Building from Source

If you prefer to build MP3Org yourself:

### Prerequisites
- **Git**: For cloning the repository
- **Java 11+**: For building and running
- **Gradle**: Included with the project (gradlew)

### Build Steps
```bash
# Clone the repository
git clone https://github.com/richardahasting/MP3Org.git
cd MP3Org

# Build the application
./gradlew build

# Run directly
./gradlew run

# Or build the distribution JAR
./gradlew shadowJar
java -jar build/distributions/mp3org-1.0.0.jar
```

### Build Commands
```bash
# Clean build
./gradlew clean build

# Create fat JAR
./gradlew shadowJar

# Create distribution ZIP
./gradlew distributionZip

# Run tests
./gradlew test

# Run from JAR
./gradlew runShadowJar
```

## Troubleshooting

### Common Issues

**"Java not found" Error**:
- Install Java 11 or higher
- Ensure Java is in your system PATH
- Try running: `java --version`

**"JavaFX not found" Error**:
- Install OpenJDK with JavaFX support
- Or add JavaFX manually: `--module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml`

**Application won't start**:
- Check Java version: `java --version`
- Try running with: `java --enable-native-access=ALL-UNNAMED -jar mp3org-1.0.0.jar`
- Check console output for specific error messages

**Performance Issues**:
- Increase memory: `-Xmx4g` (for 4GB)
- Close other applications
- Use SSD storage for better performance

**File Permission Issues (Linux/macOS)**:
```bash
# Make run script executable
chmod +x scripts/run-mp3org.sh

# Fix JAR permissions
chmod 644 mp3org-1.0.0.jar
```

### Getting Help

- **Built-in Help**: Press F1 in the application
- **User Guide**: See `MP3Org_User_Guide.md`
- **Issues**: Report bugs on [GitHub Issues](https://github.com/richardahasting/MP3Org/issues)
- **Discussions**: Ask questions on [GitHub Discussions](https://github.com/richardahasting/MP3Org/discussions)

## File Locations

### Default Database Location
- **Windows**: `%USERPROFILE%\mp3org\`
- **macOS**: `~/mp3org/`
- **Linux**: `~/mp3org/`

### Configuration Files
- **Database**: `mp3org/` directory
- **Profiles**: `mp3org-profiles.properties`
- **Settings**: `mp3org.properties`

### Log Files
- **Application logs**: Console output
- **Database logs**: `derby.log`

---

**Need help?** Check the [User Guide](MP3Org_User_Guide.md) or open an issue on GitHub!