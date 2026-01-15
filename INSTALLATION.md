# MP3Org Installation Guide

## Quick Start

### Prerequisites
- **Java 21** (LTS) - Required
- **Node.js 18+** - Required for frontend
- **Chromaprint** - Optional, for audio fingerprinting

### Clone and Run

```bash
# Clone the repository
git clone https://github.com/richardahasting/MP3Org.git
cd MP3Org

# Start backend (Terminal 1)
./gradle21 bootRun              # macOS/Linux/Git Bash
.\gradle21.cmd bootRun          # Windows PowerShell or Command Prompt

# Start frontend (Terminal 2)
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 in your browser.

---

## Java 21 Installation

MP3Org requires Java 21 LTS. The `gradle21` scripts automatically locate Java 21 on your system.

### macOS

**Option A: Homebrew (Recommended)**
```bash
brew install openjdk@21
```

**Option B: SDKMAN**
```bash
curl -s "https://get.sdkman.io" | bash
sdk install java 21-tem
```

**Option C: Direct Download**
- [Adoptium Temurin](https://adoptium.net/)
- [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)

### Linux

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

**Fedora/RHEL:**
```bash
sudo dnf install java-21-openjdk-devel
```

**Arch Linux:**
```bash
sudo pacman -S jdk21-openjdk
```

**SDKMAN (Any distro):**
```bash
curl -s "https://get.sdkman.io" | bash
sdk install java 21-tem
```

### Windows

**Option A: winget (Recommended)**
```cmd
winget install EclipseAdoptium.Temurin.21.JDK
```

**Option B: Chocolatey**
```cmd
choco install temurin21
```

**Option C: Direct Download**
- [Adoptium Temurin](https://adoptium.net/)
- [Microsoft OpenJDK](https://docs.microsoft.com/en-us/java/openjdk/download)
- [Amazon Corretto](https://aws.amazon.com/corretto/)

### Verify Installation

```bash
java --version
# Should show: openjdk 21.x.x or similar
```

---

## Node.js Installation

The React frontend requires Node.js 18 or higher.

### macOS

```bash
# Homebrew
brew install node

# Or use nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
nvm install 18
```

### Linux

```bash
# Ubuntu/Debian (using NodeSource)
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# Or use nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
nvm install 18
```

### Windows

```cmd
# winget
winget install OpenJS.NodeJS.LTS

# Or download from https://nodejs.org/
```

### Verify Installation

```bash
node --version    # Should show v18.x.x or higher
npm --version     # Should show 9.x.x or higher
```

---

## Chromaprint Installation (Optional)

Audio fingerprinting requires Chromaprint for acoustic duplicate detection.

### macOS

```bash
brew install chromaprint
```

### Linux

**Ubuntu/Debian:**
```bash
sudo apt install libchromaprint-tools
```

**Fedora/RHEL:**
```bash
sudo dnf install chromaprint-tools
```

**Arch Linux:**
```bash
sudo pacman -S chromaprint
```

### Windows

**Option A: winget (Recommended, Verified)**
```cmd
winget install AcoustID.Chromaprint
```
Note: Restart your terminal after installation for the PATH to update.

**Option B: Chocolatey**
```cmd
choco install chromaprint
```

**Option C: Manual Download**
1. Download from https://acoustid.org/chromaprint
2. Extract the ZIP file
3. Add the folder containing `fpcalc.exe` to your PATH

### Verify Installation

```bash
fpcalc -version
# Should show: fpcalc version 1.x.x
```

---

## How gradle21 Works

The `gradle21` scripts automatically find Java 21 on your system:

| Platform | Locations Checked |
|----------|-------------------|
| **macOS** | `/usr/libexec/java_home -v 21` |
| **Linux** | SDKMAN, `/usr/lib/jvm/java-21-*`, `/opt/`, Homebrew |
| **Windows** | Program Files (Adoptium, Oracle, Microsoft, Amazon, Zulu, BellSoft) |

If Java 21 is already your default, you can use `./gradlew` directly.

---

## Building from Source

### Development Mode

```bash
# Backend with hot reload
./gradle21 bootRun

# Frontend with hot reload
cd frontend
npm run dev
```

### Production Build

```bash
# Build backend JAR
./gradle21 build

# Build frontend
cd frontend
npm run build
```

The backend JAR is created at `build/libs/MP3Org-2.0.0-SNAPSHOT.jar`.

### Running the Production Build

```bash
# Start backend
java -jar build/libs/MP3Org-2.0.0-SNAPSHOT.jar

# Serve frontend (use any static server)
cd frontend
npm run preview
```

---

## Directory Structure

After installation, MP3Org creates:

```
~/.mp3org/                  # Application data (Linux/macOS)
%USERPROFILE%\.mp3org\      # Application data (Windows)
├── mp3org.db               # SQLite database
└── config.properties       # Settings (if customized)
```

---

## Troubleshooting

### "Java 21 not found"

The `gradle21` script shows installation instructions if Java 21 isn't found:

```
ERROR: Java 21 not found on this system.
Please install Java 21 (LTS) from one of these sources:
  - Adoptium Temurin: https://adoptium.net/
  ...
```

Follow the provided instructions for your platform.

### "JAVA_HOME points to wrong version"

The `gradle21` scripts override JAVA_HOME, so this shouldn't be an issue. If using `./gradlew` directly, ensure JAVA_HOME points to Java 21:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk     # Linux
```

### "Port already in use"

**Backend (9090):**
```bash
# Find and kill process on port 9090
lsof -i :9090           # macOS/Linux
netstat -ano | findstr :9090  # Windows
```

**Frontend (5173):**
```bash
# Kill process or use different port
npm run dev -- --port 3000
```

### "npm install fails"

```bash
# Clear npm cache and retry
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### "WebSocket connection failed"

- Ensure backend is running on port 9090
- Check browser console for CORS errors
- Try refreshing the page

---

## Version Managers

### SDKMAN (Java)

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash

# Install and use Java 21
sdk install java 21-tem
sdk use java 21-tem

# Auto-switch when entering project
sdk env
```

The project includes `.sdkmanrc` for automatic version switching.

### nvm (Node.js)

```bash
# Install nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# Install and use Node 18
nvm install 18
nvm use 18
```

---

## Platform-Specific Notes

### macOS

- Gatekeeper may block unsigned JARs - right-click and select "Open"
- Use Homebrew for easiest installation of all dependencies

### Linux

- Some distros require manual PATH configuration for Java
- Wayland users may need XWayland for some Java features

### Windows

**Quick Install (winget):**
```cmd
winget install EclipseAdoptium.Temurin.21.JDK
winget install OpenJS.NodeJS.LTS
```

**Running Build Commands:**

| Environment | Command |
|-------------|---------|
| **PowerShell** | `.\gradle21.cmd build` |
| **Command Prompt** | `.\gradle21.cmd build` |
| **Git Bash** | `./gradle21 build` |

**Notes:**
- The `.\` prefix is required in PowerShell and recommended in Command Prompt
- In Git Bash, use the Unix-style `./gradle21` script
- Admin privileges are only needed if installing to protected directories
- The script automatically finds Java 21 from common install locations

---

## Getting Help

- **User Guide**: See [MP3Org_User_Guide.md](MP3Org_User_Guide.md)
- **README**: See [README.md](README.md) for technical details
- **Issues**: [GitHub Issues](https://github.com/richardahasting/MP3Org/issues)

---

*Last updated: January 2026*
*Version: MP3Org 2.0*
