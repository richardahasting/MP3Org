# MP3Org Test Plan

## Overview

This test plan covers the MP3Org web application (v2.0) with Spring Boot backend and React frontend.

---

## Test Environment Setup

### Prerequisites

- Java 21 installed
- Node.js 18+ installed
- Chromaprint installed (for fingerprinting tests)
- Test music files available

### Starting the Application

```bash
# Terminal 1: Backend
./gradle21 bootRun

# Terminal 2: Frontend
cd frontend && npm run dev
```

### Test Data Setup

Create a test directory with sample music files:
- Various formats (MP3, FLAC, M4A)
- Known duplicates with different metadata
- Files with complete and incomplete metadata

---

## Backend Tests

### Running Backend Tests

```bash
./gradle21 test
```

### Test Categories

#### Unit Tests

| Test Class | Purpose |
|------------|---------|
| `StringUtilsTest` | Fuzzy string matching algorithms |
| `MusicFileTest` | Music file entity operations |
| `FuzzyMatcherTest` | Similarity calculation |

#### Service Tests

| Test Class | Purpose |
|------------|---------|
| `MusicFileServiceTest` | CRUD operations |
| `DuplicateServiceTest` | Duplicate detection logic |
| `ScanningServiceTest` | Directory scanning |

#### Integration Tests

| Test Class | Purpose |
|------------|---------|
| `MusicFileControllerTest` | REST API endpoints |
| `DuplicateControllerTest` | Duplicate API endpoints |
| `ScanningControllerTest` | Scanning API endpoints |

---

## Frontend Tests

### Running E2E Tests

```bash
cd frontend
npm run build
npm run preview &
npm test
```

### E2E Test Scenarios

#### Import Tab Tests

- [ ] Browse directories on server
- [ ] Start a scan
- [ ] View real-time progress via WebSocket
- [ ] Cancel running scan
- [ ] View scan results

#### Duplicate Manager Tests

- [ ] Load duplicate groups
- [ ] Select a duplicate group
- [ ] View file details and comparison
- [ ] Delete individual file
- [ ] Preview auto-resolution
- [ ] Execute auto-resolution
- [ ] Switch to directory view
- [ ] Select directory conflict
- [ ] Preview directory resolution
- [ ] Execute directory resolution

#### Metadata Editor Tests

- [ ] Search for files
- [ ] View search results
- [ ] Select file for editing
- [ ] Edit individual fields
- [ ] Save changes
- [ ] Select multiple files
- [ ] Open bulk edit dialog
- [ ] Apply bulk edits

#### Config Tab Tests

- [ ] View current settings
- [ ] Modify duplicate detection thresholds
- [ ] Enable/disable audio fingerprinting
- [ ] Save configuration
- [ ] View help documentation

---

## API Tests

### Music Files API

```bash
# List files
curl http://localhost:9090/api/v1/music?page=0&size=10

# Get single file
curl http://localhost:9090/api/v1/music/1

# Search
curl "http://localhost:9090/api/v1/music/search?q=beatles"

# Update
curl -X PUT http://localhost:9090/api/v1/music/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"New Title","artist":"Artist"}'
```

### Duplicates API

```bash
# Get duplicate groups
curl http://localhost:9090/api/v1/duplicates?page=0&size=25

# Get directory conflicts
curl http://localhost:9090/api/v1/duplicates/by-directory

# Preview auto-resolution
curl http://localhost:9090/api/v1/duplicates/auto-resolve/preview

# Delete file
curl -X DELETE http://localhost:9090/api/v1/duplicates/file/123
```

### Scanning API

```bash
# Browse directories
curl "http://localhost:9090/api/v1/scanning/browse?path=/Users"

# Start scan
curl -X POST http://localhost:9090/api/v1/scanning/start \
  -H "Content-Type: application/json" \
  -d '{"directory":"/path/to/music"}'

# Get scan status
curl http://localhost:9090/api/v1/scanning/status/{sessionId}

# Cancel scan
curl -X POST http://localhost:9090/api/v1/scanning/cancel/{sessionId}
```

---

## Manual Test Scenarios

### Scenario 1: Complete Import Workflow

1. Start application (backend + frontend)
2. Navigate to Import tab
3. Browse to test music directory
4. Start scan
5. Verify progress updates in real-time
6. Wait for completion
7. Verify file count matches expected

**Expected**: All files imported with correct metadata

### Scenario 2: Duplicate Detection and Resolution

1. Import test directory with known duplicates
2. Navigate to Duplicate Manager
3. Verify duplicate groups are detected
4. Select a duplicate group
5. Compare file quality (bitrate, size)
6. Delete lower quality file
7. Verify group updates

**Expected**: Lower quality duplicate removed, group resolved

### Scenario 3: Directory-Based Resolution

1. Import two directories with overlapping files
2. Navigate to Duplicate Manager
3. Switch to "By Directory" view
4. Select a directory conflict
5. Preview resolution
6. Execute resolution (keep preferred directory)
7. Verify files deleted from non-preferred directory

**Expected**: All duplicates from one directory removed

### Scenario 4: Bulk Metadata Edit

1. Search for files by artist
2. Select multiple files
3. Open bulk edit
4. Verify combobox shows distinct values with counts
5. Select new artist name
6. Apply changes
7. Verify all files updated

**Expected**: All selected files have new artist name

### Scenario 5: Audio Fingerprinting

1. Enable fingerprinting in Config
2. Import files with different metadata but same audio
3. Run duplicate detection
4. Verify duplicates detected by fingerprint

**Expected**: Acoustically identical files grouped as duplicates

---

## Performance Tests

### Large Collection Test

- Import 10,000+ files
- Verify scan completes without errors
- Check memory usage stays reasonable
- Verify duplicate detection performance

### Stress Test

- Rapid tab switching
- Multiple concurrent API requests
- Large bulk edit operations

---

## Regression Tests

### Critical Paths

| Feature | Test |
|---------|------|
| Application startup | Backend starts on 9090, frontend on 5173 |
| Database initialization | SQLite database created on first run |
| WebSocket connection | Progress updates received during scan |
| File deletion | Files removed from disk and database |
| Metadata persistence | Changes saved and survive restart |

### Known Edge Cases

- Empty metadata fields
- Very long file paths
- Unicode characters in metadata
- Files without audio tags
- Corrupted audio files

---

## Test Results Template

### Test Execution: [Date]

| Category | Pass | Fail | Skip |
|----------|------|------|------|
| Backend Unit | | | |
| Backend Integration | | | |
| Frontend E2E | | | |
| Manual Scenarios | | | |

### Issues Found

| Issue | Severity | Description |
|-------|----------|-------------|
| | | |

### Notes

```
[Testing observations and notes]
```

---

*Test Plan Version: 2.0*
*Last Updated: January 2026*
