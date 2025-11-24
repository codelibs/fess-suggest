# CLAUDE.md - Fess Suggest Project Guide

> AI assistant guidance for the Fess Suggest project - a Java library providing intelligent search suggestions built on OpenSearch.

## Project Overview

**Fess Suggest** delivers auto-completion, search suggestions, and popular word analytics with multi-language support.

**Technology Stack:**
- Java 21+, Maven
- OpenSearch (provided dependency)
- Apache Lucene (query parsing, text analysis)
- ICU4J (Unicode text processing)
- JUnit 4 (testing)

**Repository:** https://github.com/codelibs/fess-suggest

---

## Architecture

### Package Structure

```
org.codelibs.fess.suggest/
├── Suggester.java              # Main entry point (Facade)
├── SuggesterBuilder.java       # Builder for Suggester
├── index/
│   ├── SuggestIndexer.java    # Indexing operations
│   ├── contents/              # Content parsers
│   └── writer/                # Index writers
├── request/
│   ├── suggest/               # Suggestion queries
│   └── popularwords/          # Popular word queries
├── settings/                   # Configuration management
├── entity/                     # Domain models (SuggestItem, etc.)
├── normalizer/                 # Text normalizers
├── converter/                  # Reading converters (katakana, romaji)
├── concurrent/                 # Async patterns (Deferred/Promise)
└── util/                      # Utilities
```

### Key Design Patterns

- **Builder**: SuggesterBuilder, SuggestRequestBuilder
- **Facade**: Suggester (main entry point)
- **Composite**: NormalizerChain, ReadingConverterChain
- **Strategy**: Normalizer, ReadingConverter, ContentsParser
- **Deferred/Promise**: Async operations

### Index Alias Strategy

Zero-downtime index updates using dual aliases:

```
Index Naming: {baseIndex}.{timestamp}
Example:      my-suggest.20250123120000

Aliases:
├── Search Alias: {baseIndex}          (read operations)
└── Update Alias: {baseIndex}.update   (write operations)
```

### Text Processing Pipeline

```
Input Text
    ↓
[Normalization] - NormalizerChain
    ↓
[Reading Conversion] - ReadingConverterChain
    ↓
[Analysis] - SuggestAnalyzer
```

Note: Suggester has TWO ReadingConverter instances:
- `readingConverter` - For query/metadata fields
- `contentsReadingConverter` - For content/document fields

---

## Key Components

### Suggester (Main Entry Point)
Location: `src/main/java/org/codelibs/fess/suggest/Suggester.java`

Main operations: `suggest()`, `popularWords()`, `indexer()`, `createIndexIfNothing()`, `createNextIndex()`, `switchIndex()`, `removeDisableIndices()`

### SuggestIndexer (Indexing Engine)
Location: `src/main/java/org/codelibs/fess/suggest/index/SuggestIndexer.java`

Handles all indexing operations, content parsing, and word management (bad words, elevate words).

### SuggestSettings (Configuration)
Location: `src/main/java/org/codelibs/fess/suggest/settings/SuggestSettings.java`

Manages configuration stored in OpenSearch: analyzers, bad words, elevate words, timeouts.

### SuggestItem (Domain Entity)
Location: `src/main/java/org/codelibs/fess/suggest/entity/SuggestItem.java`

Core attributes: `text`, `timestamp`, `queryFreq`, `docFreq`, `userBoost`, `readings`, `tags`, `roles`, `languages`, `kinds`

Kind types: `DOCUMENT`, `QUERY`, `USER`

---

## Development Workflow

### Build Commands

```bash
mvn compile                              # Compile
mvn test                                 # Run all tests
mvn test -Dtest=SuggesterTest           # Run specific test
mvn package                              # Package JAR
mvn formatter:format license:format     # Format code and apply licenses
mvn clean jacoco:prepare-agent test jacoco:report  # Generate coverage report
```

### Adding New Features

1. Read related source files and tests
2. Write implementation following existing patterns
3. Add comprehensive tests
4. Run `mvn formatter:format license:format test`
5. Update JavaDoc for changed/new classes

**Principles:**
- Avoid over-engineering - keep solutions simple
- Only expose necessary public APIs
- Validate parameters with `Objects.requireNonNull()`
- Provide detailed error messages with context
- Add logging with contextual information

### Resource Files

- `src/main/resources/suggest_indices/suggest.json` - Index settings
- `src/main/resources/suggest_indices/suggest/mappings-default.json` - Field mappings
- `src/main/resources/suggest_settings/` - Default configurations

---

## Testing

### Test Framework

JUnit 4 with opensearch-runner (embedded instance)

### Standard Test Setup

```java
@BeforeClass
public static void beforeClass() {
    runner = new OpenSearchRunner();
    runner.build(newConfigs().clusterName("TestCluster").numOfNode(1));
    client = runner.client();
}

@Before
public void before() {
    runner.admin().indices().prepareDelete("_all").execute().actionGet();
}

@AfterClass
public static void afterClass() {
    runner.close();
    runner.clean();
}
```

### Testing Best Practices

- Minimize `Thread.sleep()` - use only when absolutely necessary
- For timestamp tests: use minimal delays (50-100ms)
- Clean only test-specific indices (not `_all` unless needed)
- Use `runner.refresh()` after index operations
- Test with realistic multilingual content

### Key Test Classes

- `SuggesterRefactoringTest` - Index lifecycle, alias management
- `SuggesterTest` - Core functionality
- `SuggestIndexerTest` - Indexing operations
- `DefaultContentsParserTest` - Text processing pipeline

---

## Code Conventions

### Java Style

- **Formatter**: Eclipse formatter at `src/config/eclipse/formatter/java.xml`
- **Indentation**: 4 spaces (no tabs)
- **Line Length**: 140 characters max
- **License Headers**: Required (use `mvn license:format`)

### Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Classes/Interfaces | PascalCase | `SuggestIndexer`, `ReadingConverter` |
| Methods | camelCase | `createIndexIfNothing()` |
| Constants | UPPER_SNAKE_CASE | `DEFAULT_MAX_READING_NUM` |
| Packages | lowercase | `org.codelibs.fess.suggest` |

### JavaDoc Requirements

All public classes and methods require JavaDoc with `@param`, `@return`, and `@throws` tags.

### Best Practices

**Null Safety:**
```java
public Suggester(final Client client, final SuggestSettings settings) {
    this.client = Objects.requireNonNull(client, "client must not be null");
    this.settings = Objects.requireNonNull(settings, "settings must not be null");
}
```

**Logging with Context:**
```java
if (logger.isInfoEnabled()) {
    logger.info("Creating suggest index: index={}, searchAlias={}, updateAlias={}",
                indexName, getSearchAlias(index), getUpdateAlias(index));
}
```

**Exception Handling:**
```java
try {
    converter.init();
} catch (Exception e) {
    throw new SuggesterException("Failed to initialize converter: "
                                + converter.getClass().getName(), e);
}
```

**Resource Management:**
```java
try (InputStream is = getClass().getClassLoader()
        .getResourceAsStream("suggest_indices/suggest.json")) {
    if (is == null) {
        throw new IOException("Resource not found");
    }
    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
}
```

---

## Quick Reference

### Essential Files

| File | Purpose |
|------|---------|
| `Suggester.java` | Main entry point, index management |
| `SuggestIndexer.java` | Indexing operations |
| `SuggestRequest.java` | Query building, scoring |
| `SuggestSettings.java` | Configuration |
| `SuggestItem.java` | Domain model |
| `DefaultContentsParser.java` | Text processing pipeline |

### Key Constants

```java
// Field names (FieldNames.java)
TEXT, READING_PREFIX, QUERY_FREQ, DOC_FREQ, USER_BOOST, KINDS, TIMESTAMP

// Default values
MAX_READING_NUM = 10
PREFIX_MATCH_WEIGHT = 2.0f
EXPECTED_INDEX_COUNT = 1

// Default timeouts
searchTimeout: 15s
indexTimeout: 1m
bulkTimeout: 1m
```

### Common Workflows

**Index Lifecycle:**
```java
suggester.createIndexIfNothing();      // Initial setup
suggester.createNextIndex();            // Create new index
// ... indexing operations ...
suggester.switchIndex();                // Switch to new index
suggester.removeDisableIndices();       // Cleanup old indices
```

**Async Operations:**
```java
suggester.suggest()
    .setQuery("search")
    .execute()
    .done(response -> { /* handle success */ })
    .error(throwable -> { /* handle error */ });
```

---

## Important Notes

### Thread Safety
- **Suggester**: Thread-safe for queries
- **SuggestIndexer**: Thread-safe for indexing
- **SuggestSettings**: NOT thread-safe for modifications

### Performance
- Use batch operations for indexing (100-500 items)
- Don't call `refresh()` too frequently
- Limit `maxReadingNum` to prevent memory issues
- OpenSearch auto-refreshes every 1 second by default

### Compatibility
- Designed for OpenSearch 2.x+
- All text processing uses UTF-8
- Test with target OpenSearch version before deploying
