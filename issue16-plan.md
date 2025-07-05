# Issue #16 Implementation Plan: TestDataFactory for Programmatic Test Data Generation

## Executive Summary
Implement a TestDataFactory utility to enable dynamic generation of test audio files and metadata combinations. This will complement the existing manual test data collection (23 files) by providing programmatic test data creation capabilities for future test scenarios.

## Current State Analysis

### Existing Test Infrastructure
- **Manual Test Data**: 23 strategically crafted audio files in `src/test/resources/audio/`
- **Coverage**: Duplicates, edge cases, formats (MP3, FLAC, WAV, OGG), Unicode, missing metadata
- **Test Harness**: TESTING-HARNESS profile with automatic cleanup
- **Integration**: 100% test pass rate with current manual data

### Gap Analysis
While manual test data provides excellent coverage, programmatic generation would enable:
- Dynamic test scenarios without manual file creation
- Parameterized testing with multiple variations
- Large-scale performance testing datasets
- Rapid prototyping of new test cases

## Implementation Architecture

### Core Components

#### 1. TestDataFactory (Main API)
```java
package org.hasting.test;

public class TestDataFactory {
    // Factory methods for common test scenarios
    public static List<MusicFile> createDuplicateSet(DuplicateSpec spec);
    public static List<MusicFile> createEdgeCaseSet(EdgeCaseSpec spec);
    public static List<MusicFile> createFormatTestSet(FormatSpec spec);
    public static MusicFile createCustomFile(TestFileSpec spec);
    
    // Batch operations
    public static TestDataSet createTestDataSet(TestDataSetSpec spec);
    public static void cleanupGeneratedFiles();
}
```

#### 2. TestFileGenerator (File Creation Engine)
```java
package org.hasting.test;

public class TestFileGenerator {
    // Template-based generation
    public File generateFromTemplate(File template, TestFileSpec spec);
    
    // Metadata manipulation
    public void embedMetadata(File audioFile, MetadataSpec metadata);
    
    // Format conversion (if ffmpeg available)
    public File convertFormat(File source, AudioFormat targetFormat);
}
```

#### 3. Specification Classes
```java
package org.hasting.test.spec;

// Fluent builder pattern for test file specifications
public class TestFileSpec {
    private String title, artist, album, genre;
    private Integer trackNumber, year;
    private Duration duration;
    private AudioFormat format;
    private BitRate bitRate;
    
    // Builder pattern implementation
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        // Fluent methods
        public Builder title(String title);
        public Builder artist(String artist);
        public Builder randomizeMetadata();
        public TestFileSpec build();
    }
}

// Specialized specifications
public class DuplicateSpec {
    private String baseTitle;
    private String baseArtist;
    private int variations;
    private List<DuplicateVariation> variationTypes;
}

public class EdgeCaseSpec {
    private Set<EdgeCaseType> types;
    private boolean includeUnicode;
    private boolean includeLongStrings;
    private boolean includeMissingMetadata;
}
```

#### 4. Template Management
```java
package org.hasting.test;

public class TestTemplateManager {
    // Template discovery and caching
    private Map<AudioFormat, File> templateCache;
    
    public File getTemplate(AudioFormat format);
    public void registerTemplate(AudioFormat format, File template);
    public void createDefaultTemplates();
}
```

## Implementation Phases

### Phase 1: Core Infrastructure (2-3 hours)
1. **Create package structure**
   - `org.hasting.test` - Main factory classes
   - `org.hasting.test.spec` - Specification/builder classes
   - `org.hasting.test.generator` - File generation logic
   - `org.hasting.test.template` - Template management

2. **Implement basic TestFileSpec**
   - Builder pattern for fluent API
   - Validation logic
   - Default value handling

3. **Create TestFileGenerator**
   - Template loading logic
   - Basic file copying with unique names
   - Metadata embedding using JAudioTagger

4. **Implement TestDataFactory facade**
   - Simple factory methods
   - Cleanup tracking
   - Error handling

### Phase 2: Template-Based Generation (2 hours)
1. **Create template audio files**
   - Short (10-second) samples for each format
   - Silent audio or simple tones
   - Minimal file size for fast operations

2. **Implement template operations**
   - Copy template to test location
   - Modify metadata using JAudioTagger
   - Handle format-specific limitations

3. **Add variation generation**
   - Bitrate modifications (if possible)
   - Duration trimming (if ffmpeg available)
   - Metadata variations

### Phase 3: Advanced Features (2 hours)
1. **Duplicate generation logic**
   - Title variations (typos, case changes)
   - Artist variations (featuring, &, and)
   - Bitrate differences
   - Duration variations

2. **Edge case generation**
   - Unicode character sets
   - Maximum length strings
   - Special characters
   - Missing/null metadata

3. **Batch operations**
   - Performance test datasets
   - Complete test suites
   - Cleanup management

### Phase 4: Integration & Testing (1 hour)
1. **Integration with existing tests**
   - Update test base classes
   - Migration utilities for existing tests
   - Documentation

2. **Test the test factory**
   - Unit tests for factory methods
   - Validation of generated files
   - Performance benchmarks

## Technical Decisions

### Audio Generation Approach
**Decision: Template-Based Generation**
- Use pre-created short audio files as templates
- Modify copies with different metadata
- Rationale: Simpler, faster, no external dependencies

**Alternative considered**: FFmpeg integration
- Pros: More flexibility, format conversion
- Cons: External dependency, complexity
- Status: Can be added in Phase 5 if needed

### Metadata Library
**Decision: JAudioTagger (already in project)**
- Proven integration with MP3Org
- Supports all required formats
- No new dependencies

### File Storage
**Decision: Temporary directory with tracking**
- Use system temp directory
- Track all generated files for cleanup
- Automatic cleanup on JVM exit
- Manual cleanup method available

## Usage Examples

### Example 1: Generate Duplicate Test Set
```java
@Test
public void testDuplicateDetection() {
    // Generate variations of the same song
    List<MusicFile> duplicates = TestDataFactory.createDuplicateSet(
        DuplicateSpec.builder()
            .baseTitle("Test Song")
            .baseArtist("Test Artist")
            .addVariation(TITLE_TYPO)        // "Test Snog"
            .addVariation(ARTIST_FEATURING)   // "Test Artist ft. Someone"
            .addVariation(BITRATE_DIFFERENT)  // 192kbps vs 320kbps
            .addVariation(CASE_DIFFERENT)     // "test song"
            .count(4)
            .build()
    );
    
    // Test duplicate detection
    List<List<MusicFile>> groups = DatabaseManager.findDuplicateGroups();
    assertEquals(1, groups.size());
    assertEquals(4, groups.get(0).size());
}
```

### Example 2: Edge Case Testing
```java
@Test
public void testUnicodeHandling() {
    MusicFile unicodeFile = TestDataFactory.createCustomFile(
        TestFileSpec.builder()
            .title("日本語タイトル")
            .artist("Björk")
            .album("Ñoño")
            .genre("电子音乐")
            .format(AudioFormat.MP3)
            .build()
    );
    
    // Save and retrieve
    DatabaseManager.saveMusicFile(unicodeFile);
    MusicFile retrieved = DatabaseManager.findByPath(unicodeFile.getFilePath());
    
    assertEquals("日本語タイトル", retrieved.getTitle());
}
```

### Example 3: Performance Testing
```java
@Test
public void testLargeDatasetPerformance() {
    // Generate 1000 files for performance testing
    TestDataSet largeSet = TestDataFactory.createTestDataSet(
        TestDataSetSpec.builder()
            .fileCount(1000)
            .randomizeMetadata(true)
            .formatDistribution(
                MP3(70),    // 70% MP3
                FLAC(20),   // 20% FLAC
                WAV(5),     // 5% WAV
                OGG(5)      // 5% OGG
            )
            .includeDuplicates(true, 10) // 10% duplicates
            .build()
    );
    
    long startTime = System.currentTimeMillis();
    for (MusicFile file : largeSet.getFiles()) {
        DatabaseManager.saveMusicFile(file);
    }
    long duration = System.currentTimeMillis() - startTime;
    
    assertTrue("Insert performance", duration < 30000); // 30 seconds
}
```

## File Structure

```
src/
├── main/java/
│   └── org/hasting/test/
│       ├── TestDataFactory.java          (Main API - 150 lines)
│       ├── TestFileGenerator.java        (Generation logic - 200 lines)
│       ├── TestTemplateManager.java      (Template handling - 100 lines)
│       ├── spec/
│       │   ├── TestFileSpec.java         (File specification - 120 lines)
│       │   ├── DuplicateSpec.java        (Duplicate scenarios - 80 lines)
│       │   ├── EdgeCaseSpec.java         (Edge case scenarios - 60 lines)
│       │   └── TestDataSetSpec.java      (Batch specifications - 100 lines)
│       └── generator/
│           ├── MetadataEmbedder.java     (JAudioTagger wrapper - 100 lines)
│           ├── FileVariationGenerator.java (Variation logic - 150 lines)
│           └── TemplateProcessor.java     (Template operations - 80 lines)
├── test/
│   └── resources/
│       └── templates/                     (Template audio files)
│           ├── template-10s.mp3          (10-second MP3)
│           ├── template-10s.flac         (10-second FLAC)
│           ├── template-10s.wav          (10-second WAV)
│           └── template-10s.ogg          (10-second OGG)
```

## Success Criteria

### Functional Requirements
- [ ] Generate test files that pass all existing MusicFile validations
- [ ] Support all audio formats used in MP3Org (MP3, FLAC, WAV, OGG)
- [ ] Embed metadata correctly for all supported fields
- [ ] Create recognizable duplicate variations
- [ ] Handle edge cases (Unicode, special characters, missing data)
- [ ] Clean up all generated files on demand

### Performance Requirements
- [ ] Generate single test file in < 100ms
- [ ] Generate 100 test files in < 5 seconds
- [ ] Minimal disk space usage (< 1MB per generated file)
- [ ] No memory leaks during batch generation

### Quality Requirements
- [ ] 100% test coverage for factory classes
- [ ] Clear, fluent API with good discoverability
- [ ] Comprehensive JavaDoc with examples
- [ ] Integration with existing test infrastructure
- [ ] No new external dependencies

## Risk Mitigation

### Risk 1: Template File Size
**Mitigation**: Create minimal 10-second silent audio files
- MP3: ~150KB
- FLAC: ~50KB  
- WAV: ~1.7MB
- OGG: ~100KB

### Risk 2: Metadata Embedding Failures
**Mitigation**: Graceful degradation
- Validate metadata before embedding
- Skip unsupported fields for specific formats
- Log warnings for partial failures

### Risk 3: File System Issues
**Mitigation**: Robust file handling
- Check disk space before generation
- Use unique names to avoid conflicts
- Implement retry logic for temporary failures
- Always cleanup on JVM exit

## Timeline Estimate

**Total: 7-8 hours**

1. **Phase 1 - Core Infrastructure**: 2-3 hours
2. **Phase 2 - Template Generation**: 2 hours  
3. **Phase 3 - Advanced Features**: 2 hours
4. **Phase 4 - Integration & Testing**: 1 hour

## Future Enhancements (Phase 5+)

1. **FFmpeg Integration**
   - Dynamic format conversion
   - Bitrate modifications
   - Duration adjustments
   - Audio quality variations

2. **Realistic Audio Generation**
   - Sine wave generation
   - Multi-track simulation
   - Realistic file sizes

3. **Database Seeding**
   - Pre-populate test databases
   - Performance test datasets
   - Demo data generation

4. **CI/CD Integration**
   - Automatic test data generation
   - Performance benchmarking
   - Test data versioning

## Conclusion

The TestDataFactory will provide a powerful tool for test data generation while maintaining the simplicity and reliability of the existing test infrastructure. The template-based approach ensures quick implementation without external dependencies, while the modular design allows for future enhancements as needed.

The implementation follows MP3Org's development philosophy of self-documenting code with clear patterns that teach themselves, making it easy for future developers to understand and extend the test data generation capabilities.