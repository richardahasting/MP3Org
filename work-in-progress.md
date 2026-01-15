# MP3Org Work In Progress

*This file tracks active work, current context, and session-to-session continuity*

## Current Status: January 2026

### **Application Version**: 2.0 (Web Application)
- **Architecture**: Spring Boot 3.4 + React 18 + TypeScript
- **Database**: SQLite (migrated from Apache Derby)
- **Java**: 21 LTS (enforced via gradle21 scripts)

---

## Active Work

### **Issue #99 - Make building on Windows easy** (Ready for PR)
- ✅ Fixed gradle21.cmd path handling for spaces
- ✅ Verified builds work in PowerShell, Command Prompt, and Git Bash
- ✅ Updated INSTALLATION.md with clear Windows instructions
- Branch: `feature/issue-99-windows-build`

---

## Recently Completed

### **PR #93 - Cross-Platform Build Portability** (Merged 2026-01-13)
- Enhanced `gradle21` script with Linux and Windows support
- Created `gradle21.cmd` for native Windows CMD/PowerShell
- Added Maven dependencies for jaudiotagger and miglayout
- Made JavaFX dependencies platform-independent
- Removed tracked `.dylib` files from git

### **PR #91 - Help Pages for All Tabs** (Merged 2026-01-13)
- Issue #90 - Added contextual help documentation
- OS-specific Chromaprint installation instructions
- Help overlay accessible from each tab

### **PR #87 - Delete File Button** (Merged 2026-01-13)
- Issue #83 - Changed "Keep This File" to "Delete this file"
- Safer, more explicit duplicate resolution
- Group auto-resolves when only 1 file remains

### **Issue #92 - Directory-Based Duplicate Resolution** (Completed)
- View mode toggle: By Similarity / By Directory
- Directory conflict list showing folder pairs
- Preview and execute directory-level resolution
- Bulk delete files from non-preferred directories

### **Bulk Edit Enhancements** (Completed)
- Combobox inputs with HTML5 datalist
- Suggestion chips showing distinct values with frequency counts
- Pre-selection of most common value
- Year field added to bulk edit

---

## Web Application Migration Complete

| Phase | Description | Status |
|-------|-------------|--------|
| 1. Foundation | Spring Boot + basic React | ✅ Complete |
| 2. Import/Scanning | WebSocket progress | ✅ Complete |
| 3. Duplicate Detection | Core feature | ✅ Complete |
| 4. Metadata Editor | Search + bulk edit | ✅ Complete |
| 5. Organization | Template system | ✅ Complete |
| 6. Configuration | All settings | ✅ Complete |
| 7. UI Polish | frontend-design skill | ✅ Complete |

---

## Open Issues

### **Issue #70 - API Test Suite** (Not Started)
- Create comprehensive integration tests for REST API
- Priority: Medium

### **Issue #94 - Production Build** (Potential)
- Create versioned release artifacts
- GitHub release with downloadable JAR
- Priority: Low

---

## Known Technical Items

### Deprecation Warnings (Non-critical)
- `StringUtils.java` uses deprecated API
- Unchecked operations in some files
- Run with `-Xlint:deprecation` for details

### Java Version Requirement
- Application requires Java 21
- `gradle21` / `gradle21.cmd` auto-locate Java 21
- Direct `./gradlew` works if Java 21 is default

---

## Branch Status

### **Current Branch**: feature/log4Rich-integration
- Contains logging framework updates
- Documentation updates in progress

### **Main Branch**: Up to date
- All PRs merged
- Ready for development

---

## Quick Reference

### Starting Development
```bash
# Backend (Terminal 1)
./gradle21 bootRun

# Frontend (Terminal 2)
cd frontend && npm install && npm run dev
```

### Running Tests
```bash
./gradle21 test                    # Backend tests
cd frontend && npm test            # Frontend E2E tests
```

### Building for Production
```bash
./gradle21 build                   # Creates JAR in build/libs/
cd frontend && npm run build       # Creates dist/ folder
```

---

*Last Updated: 2026-01-13*
*Status: Development active, all core features complete*
