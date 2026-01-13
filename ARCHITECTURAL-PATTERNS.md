# MP3Org Architectural Patterns and Design Decisions

## Overview

This document captures the architectural patterns, design principles, and decision rationale for the MP3Org application. The project uses a modern full-stack architecture with Spring Boot backend and React frontend.

## Current Architecture (v2.0)

### Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| **Frontend** | React 18, TypeScript, Vite | Modern web UI |
| **Backend** | Spring Boot 3.4, Java 21 | REST API & business logic |
| **Database** | SQLite | Embedded database |
| **Real-time** | WebSocket (STOMP) | Scan progress updates |
| **Audio** | JAudioTagger, Chromaprint | Metadata & fingerprinting |

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     React Frontend                          │
│  ┌─────────┐ ┌──────────┐ ┌────────┐ ┌──────────┐ ┌───────┐│
│  │Duplicate│ │ Metadata │ │ Import │ │ Organize │ │Config ││
│  │ Manager │ │  Editor  │ │  View  │ │   View   │ │ View  ││
│  └────┬────┘ └────┬─────┘ └───┬────┘ └────┬─────┘ └───┬───┘│
└───────┼──────────┼─────────────┼───────────┼──────────┼─────┘
        │          │             │           │          │
        └──────────┴─────────────┴───────────┴──────────┘
                              │ HTTP/WebSocket
        ┌─────────────────────┴─────────────────────────┐
        │              Spring Boot REST API              │
        │  ┌─────────────────────────────────────────┐  │
        │  │             Service Layer               │  │
        │  │  MusicFileService, ScanningService,     │  │
        │  │  DuplicateService, ConfigService        │  │
        │  └────────────────────┬────────────────────┘  │
        └───────────────────────┼───────────────────────┘
                                │
                        ┌───────┴───────┐
                        │ SQLite Database│
                        └───────────────┘
```

---

## Design Principles

### 1. SOLID Principles

#### **Single Responsibility Principle (SRP)**
Each class has one reason to change and one primary responsibility.

**Backend Examples:**
- `MusicFileService`: CRUD operations for music files
- `DuplicateService`: Duplicate detection and resolution
- `ScanningService`: Directory scanning and progress tracking
- `FingerprintService`: Audio fingerprint generation

**Frontend Examples:**
- `DuplicateManager.tsx`: Duplicate management UI
- `MetadataEditor.tsx`: Metadata editing UI
- `duplicatesApi.ts`: API client for duplicates endpoints

#### **Open/Closed Principle (OCP)**
Classes are open for extension but closed for modification.

**Implementation:**
- Strategy pattern for duplicate detection algorithms
- Configurable thresholds without code changes
- Plugin architecture for audio format support

#### **Dependency Inversion Principle (DIP)**
High-level modules depend on abstractions, not concrete implementations.

**Application:**
- Service interfaces in Spring Boot
- React hooks abstract API interactions
- Configuration interfaces decouple business logic

### 2. Design Patterns

#### **Repository Pattern**
Data access abstraction for database operations.

```java
@Repository
public interface MusicFileRepository {
    List<MusicFile> findByArtistContaining(String artist);
    Optional<MusicFile> findByFilePath(String path);
}
```

#### **Service Pattern**
Business logic encapsulation in service classes.

```java
@Service
public class DuplicateService {
    public List<DuplicateGroupDTO> getDuplicateGroups() { ... }
    public AutoResolutionResultDTO autoResolveDuplicates() { ... }
}
```

#### **DTO Pattern**
Data transfer objects for API communication.

```java
public record MusicFileDTO(
    Long id,
    String filePath,
    String title,
    String artist,
    // ... other fields
) {}
```

#### **Custom Hooks Pattern (Frontend)**
Reusable React hooks for state management.

```typescript
function useDuplicates() {
    const [groups, setGroups] = useState<DuplicateGroup[]>([]);
    const [loading, setLoading] = useState(false);
    // ... fetch logic
    return { groups, loading, refresh };
}
```

---

## Architectural Layers

### 1. Presentation Layer (Frontend)

**Purpose**: User interface and interaction

**Components:**
- React functional components with TypeScript
- Custom hooks for data fetching
- API client modules for backend communication
- CSS modules for styling

**Principles:**
- Component composition over inheritance
- Hooks for state management
- Memoization for performance
- Accessibility considerations

### 2. API Layer (Controllers)

**Purpose**: HTTP request handling and routing

**Components:**
- `MusicFileController`: Music file CRUD endpoints
- `DuplicateController`: Duplicate detection endpoints
- `ScanningController`: Directory scanning endpoints
- `ConfigController`: Application settings endpoints

**Principles:**
- RESTful endpoint design
- Request validation
- Response DTOs (not entities)
- Exception handling

### 3. Service Layer

**Purpose**: Business logic and orchestration

**Components:**
- `MusicFileService`: File management operations
- `DuplicateService`: Duplicate detection algorithms
- `ScanningService`: Directory traversal and metadata extraction
- `FingerprintService`: Audio fingerprint processing

**Principles:**
- Transaction management
- Business rule enforcement
- Service composition
- Caching strategies

### 4. Data Layer

**Purpose**: Data persistence and retrieval

**Components:**
- `DatabaseManager`: JDBC operations with SQLite
- Entity models (`MusicFile`)
- DTOs for API responses

**Principles:**
- Connection pooling
- Prepared statements
- Index optimization
- Data integrity constraints

---

## API Design

### REST Conventions

| HTTP Method | Purpose | Example |
|-------------|---------|---------|
| GET | Retrieve resource(s) | `GET /api/v1/music/{id}` |
| POST | Create resource or action | `POST /api/v1/scanning/start` |
| PUT | Update resource | `PUT /api/v1/music/{id}` |
| DELETE | Delete resource | `DELETE /api/v1/duplicates/file/{id}` |

### Response Format

```json
{
  "groups": [...],
  "page": 0,
  "size": 25,
  "totalGroups": 150,
  "totalPages": 6,
  "hasMore": true
}
```

### Error Handling

```json
{
  "error": "File not found",
  "status": 404,
  "timestamp": "2026-01-13T12:00:00Z"
}
```

---

## Real-Time Communication

### WebSocket Architecture

```
Browser                     Server
   │                          │
   │─── CONNECT /ws ─────────>│
   │                          │
   │─── SUBSCRIBE ───────────>│
   │    /topic/scanning/{id}  │
   │                          │
   │<─── MESSAGE ─────────────│
   │    {progress: 45%, ...}  │
   │                          │
```

### STOMP Protocol Usage

- `/app/scanning/start`: Initiate scan
- `/topic/scanning/{sessionId}`: Progress updates
- Automatic reconnection on disconnect

---

## Performance Considerations

### Backend Optimization

- **Connection Pooling**: HikariCP for SQLite connections
- **Caching**: In-memory caching for duplicate groups
- **Batch Operations**: Bulk metadata extraction
- **Lazy Loading**: Stream large result sets

### Frontend Optimization

- **Code Splitting**: Vite automatic chunk splitting
- **Memoization**: `useMemo`, `useCallback` for expensive operations
- **Virtual Scrolling**: For large file lists
- **Debouncing**: Search input optimization

### Database Optimization

- **Indexes**: On filePath, artist, album, title
- **Query Optimization**: Avoid N+1 queries
- **Connection Reuse**: Single connection per request

---

## Security Considerations

### Input Validation

- Path traversal prevention
- SQL injection prevention (prepared statements)
- XSS prevention (React automatic escaping)

### File System Safety

- Restricted to allowed directories
- Path normalization
- Permission validation

### API Security

- CORS configuration
- Request size limits
- Rate limiting (future)

---

## Testing Architecture

### Backend Testing

```
src/test/java/
├── controller/          # Controller integration tests
├── service/             # Service unit tests
├── util/                # Utility class tests
└── integration/         # End-to-end tests
```

### Frontend Testing

```
frontend/tests/
├── e2e/                 # Puppeteer E2E tests
└── components/          # Component unit tests (future)
```

### Test Categories

| Category | Tool | Purpose |
|----------|------|---------|
| Unit | JUnit 5 | Service/utility testing |
| Integration | Spring Boot Test | Controller testing |
| E2E | Puppeteer | Full workflow testing |

---

## Migration History

### v1.0 → v2.0 Changes

| Aspect | v1.0 (Desktop) | v2.0 (Web) |
|--------|----------------|------------|
| UI | JavaFX/Swing | React + TypeScript |
| Backend | Monolithic | Spring Boot REST API |
| Database | Apache Derby | SQLite |
| Java | 11 | 21 |
| Real-time | Event listeners | WebSocket |

### Key Migration Decisions

1. **SQLite over PostgreSQL**: Simpler deployment, no external database server
2. **React over Vue/Angular**: Better ecosystem, team familiarity
3. **Spring Boot over Quarkus**: Mature ecosystem, better documentation
4. **WebSocket over SSE**: Bidirectional communication for future features

---

## Future Evolution

### Planned Improvements

- **Plugin System**: Custom duplicate detection algorithms
- **Multi-user Support**: Authentication and authorization
- **Cloud Sync**: Optional cloud backup
- **Mobile App**: React Native or PWA

### Extension Points

- `DuplicateDetector` interface for custom algorithms
- `MetadataProvider` interface for external metadata sources
- `StorageProvider` interface for cloud storage

---

## Conclusion

The MP3Org architecture balances simplicity with scalability. The clear separation between frontend and backend enables independent development and deployment. The use of standard patterns (Repository, Service, DTO) makes the codebase accessible to developers familiar with Spring Boot.

**Key Principles:**
1. **Separation of Concerns**: Clear boundaries between layers
2. **Single Responsibility**: Each component has one job
3. **API-First Design**: Frontend and backend are decoupled
4. **Performance by Default**: Optimization built into architecture
5. **Security in Depth**: Multiple layers of protection

---

*Document Updated: January 2026*
*Architecture Version: 2.0*
