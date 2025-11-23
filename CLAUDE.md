# CLAUDE.md - Fess Suggest Project Guide

> This document provides comprehensive guidance for AI assistants (Claude) working on the Fess Suggest project. It includes architecture overview, development workflows, testing strategies, and common tasks.

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture Overview](#architecture-overview)
3. [Key Components](#key-components)
4. [Development Workflow](#development-workflow)
5. [Testing Guide](#testing-guide)
6. [Common Tasks](#common-tasks)
7. [Code Conventions](#code-conventions)
8. [Troubleshooting](#troubleshooting)

---

## Project Overview

**Fess Suggest** is a Java library providing intelligent search suggestion functionality built on OpenSearch/Elasticsearch. The project delivers:

- Auto-completion and search suggestions
- Popular word analytics and trending search tracking
- Multi-language support (Japanese via Kuromoji analyzer)
- Flexible text processing pipeline (converters, normalizers, analyzers)
- Asynchronous operations with callback support
- Zero-downtime index switching via alias management

**Technology Stack:**
- Java 21+
- OpenSearch/Elasticsearch (provided dependency)
- Apache Lucene (query parsing, text analysis)
- ICU4J (Unicode text processing)
- JUnit 4 (testing)
- Maven (build management)

**Repository:** https://github.com/codelibs/fess-suggest

**License:** Apache License 2.0

---

## Architecture Overview

### Package Structure

```
org.codelibs.fess.suggest/
├── Suggester.java                 # Main entry point (Facade pattern)
├── SuggesterBuilder.java          # Builder for Suggester
├── analysis/                      # Text analysis interfaces
├── concurrent/                    # Async patterns (Deferred/Promise)
├── constants/                     # Field names and constants
├── converter/                     # Reading converters (katakana, romaji)
├── entity/                        # Domain models (SuggestItem, ElevateWord)
├── exception/                     # Custom exceptions
├── index/                         # Indexing operations
│   ├── SuggestIndexer.java       # Main indexing API
│   ├── contents/                 # Content parsers (documents, query logs)
│   └── writer/                   # Index writers (bulk, file)
├── normalizer/                    # Text normalizers (ICU, alphabet)
├── request/                       # Request/Response handling
│   ├── suggest/                  # Suggestion queries
│   └── popularwords/             # Popular word queries
├── settings/                      # Configuration management
└── util/                         # Utilities
```

### Design Patterns

| Pattern | Usage | Key Classes |
|---------|-------|-------------|
| **Builder** | Flexible object construction | SuggesterBuilder, SuggestSettingsBuilder, SuggestRequestBuilder |
| **Facade** | Simplified interface | Suggester (main entry point) |
| **Composite** | Chain of processors | NormalizerChain, ReadingConverterChain |
| **Strategy** | Pluggable algorithms | Normalizer, ReadingConverter, ContentsParser |
| **Deferred/Promise** | Async operations | Deferred<T>, Promise |
| **Repository** | Settings persistence | SuggestSettings |
| **Template Method** | Request processing | Request<T> abstract class |

### Index Alias Strategy

Fess Suggest uses a dual-alias architecture for zero-downtime index updates:

```
Index Naming: {baseIndex}.{timestamp}
Example:      my-suggest.20250123120000

Aliases:
├── Search Alias: {baseIndex}          (for read operations)
└── Update Alias: {baseIndex}.update   (for write operations)
```

**Benefits:**
- Zero-downtime index switching
- Read/write separation
- Atomic alias operations
- Safe index rotation

---

## Key Components

### 1. Suggester (Main Orchestrator)

**Location:** `src/main/java/org/codelibs/fess/suggest/Suggester.java`

**Responsibilities:**
- Central API for all suggestion operations
- Index lifecycle management (create, switch, cleanup)
- Provides builders for suggestion queries and indexing operations

**Key Methods:**
```java
// Query operations
suggester.suggest()                    // Create suggestion query builder
suggester.popularWords()               // Create popular words query builder

// Index management
suggester.createIndexIfNothing()       // Initialize index
suggester.createNextIndex()            // Create new index for updates
suggester.switchIndex()                // Switch search alias to new index
suggester.removeDisableIndices()       // Cleanup orphaned indices

// Indexing operations
suggester.indexer()                    // Create indexer instance
suggester.refresh()                    // Refresh indices
```

### 2. SuggestIndexer (Indexing Engine)

**Location:** `src/main/java/org/codelibs/fess/suggest/index/SuggestIndexer.java`

**Responsibilities:**
- All indexing operations (create, update, delete)
- Content parsing from multiple sources (documents, query logs)
- Text processing pipeline application
- Bad word and elevate word management

**Key Methods:**
```java
// Direct indexing
indexer.index(SuggestItem)
indexer.index(SuggestItem[])

// Content-based indexing
indexer.indexFromDocument(Map[])       // From document maps
indexer.indexFromQueryLog(QueryLog[])  // From query logs (async support)
indexer.indexFromSearchWord()          // From raw search strings

// Deletion
indexer.delete(String id)
indexer.deleteByQuery(QueryBuilder)
indexer.deleteDocumentWords()
indexer.deleteQueryWords()

// Word management
indexer.addBadWord() / deleteBadWord()
indexer.addElevateWord() / deleteElevateWord()
```

### 3. Text Processing Pipeline

**Three-Level Processing:**

```
Input Text
    ↓
[1. Normalization] - NormalizerChain
├── ICUNormalizer (Unicode normalization)
├── AnalyzerNormalizer (OpenSearch analyzer)
├── FullWidthToHalfWidthAlphabetNormalizer
└── HankakuKanaToZenkakuKana
    ↓
[2. Reading Conversion] - ReadingConverterChain
├── AnalyzerConverter (uses analyzer)
├── KatakanaConverter (kanji → katakana)
└── KatakanaToAlphabetConverter (kana → romaji)
    ↓
[3. Analysis] - SuggestAnalyzer
└── Token extraction and language-specific processing
```

**Important:** Suggester has TWO ReadingConverter instances:
- `readingConverter` - For query/metadata fields
- `contentsReadingConverter` - For content/document fields

### 4. SuggestSettings (Configuration)

**Location:** `src/main/java/org/codelibs/fess/suggest/settings/SuggestSettings.java`

**Manages:**
- All configuration stored in OpenSearch
- Analyzer settings (reading, normalize, contents analyzers)
- Bad word and elevate word lists
- Array settings (supported fields)
- Timeout configurations

**Default Timeouts:**
```
searchTimeout: 15s
indexTimeout: 1m
bulkTimeout: 1m
indicesTimeout: 1m
clusterTimeout: 1m
scrollTimeout: 1m
```

### 5. SuggestItem (Domain Entity)

**Location:** `src/main/java/org/codelibs/fess/suggest/entity/SuggestItem.java`

**Core Attributes:**
```java
String text                    // Suggestion text
ZonedDateTime timestamp        // Creation time
long queryFreq                 // How often searched
long docFreq                   // How often in documents
float userBoost                // User-defined boost
String[][] readings            // Phonetic readings
String[] tags                  // Categorization
String[] roles                 // Access control
String[] languages             // Language variants
Kind[] kinds                   // DOCUMENT, QUERY, or USER
```

**Kind Types:**
- `DOCUMENT` - From indexed documents
- `QUERY` - From query logs
- `USER` - User-provided suggestions

### 6. Asynchronous Pattern (Deferred/Promise)

**Location:** `src/main/java/org/codelibs/fess/suggest/concurrent/Deferred.java`

**Usage Pattern:**
```java
// Async with callbacks
suggester.suggest()
    .setQuery("search")
    .execute()
    .done(response -> {
        // Handle success
        response.getItems().forEach(item ->
            System.out.println(item.getText())
        );
    })
    .error(throwable -> {
        // Handle error
        System.err.println("Error: " + throwable.getMessage());
    });

// Blocking wait
SuggestResponse response = suggester.suggest()
    .setQuery("search")
    .execute()
    .getResponse();  // Blocks until complete (default: 1 minute)
```

---

## Development Workflow

### Build Commands

```bash
# Compile
mvn compile

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=SuggesterTest

# Package JAR
mvn package

# Install to local repository
mvn install

# Generate test coverage report
mvn clean jacoco:prepare-agent test jacoco:report
# Report location: target/site/jacoco/index.html
```

### Code Quality

```bash
# Format code (Eclipse formatter)
mvn formatter:format

# Check license headers
mvn license:check

# Apply license headers
mvn license:format

# Generate JavaDoc
mvn javadoc:javadoc
```

### Adding New Features

**Standard Process:**

1. **Understand Existing Code**
   - Read related source files in `src/main/java/`
   - Check existing tests in `src/test/java/`
   - Review integration patterns with OpenSearch

2. **Write Implementation**
   - Follow existing naming conventions
   - Use appropriate design patterns (see Architecture Overview)
   - Add comprehensive JavaDoc for public APIs
   - Avoid over-engineering - keep solutions simple

3. **Add Tests**
   - Write unit tests for isolated logic
   - Add integration tests with embedded OpenSearch
   - Follow existing test patterns (see Testing Guide)

4. **Code Quality Checks**
   ```bash
   mvn formatter:format
   mvn license:format
   mvn test
   ```

5. **Documentation**
   - Update JavaDoc for changed/new classes
   - Update README.md if public API changes
   - Add inline comments only where logic isn't self-evident

### Resource Files

**Index Settings and Mappings:**
- `src/main/resources/suggest_indices/suggest.json` - Index settings
- `src/main/resources/suggest_indices/suggest/mappings-default.json` - Field mappings

**Default Configuration:**
- `src/main/resources/suggest_settings/` - Default analyzer and settings configurations

**Modifying Resources:**
- Changes to mappings require index recreation
- Test resource changes with `SuggesterResourceLoadingTest`
- Validate JSON format before committing

---

## Testing Guide

### Test Framework

- **Framework:** JUnit 4
- **OpenSearch Integration:** opensearch-runner (embedded instance)
- **Analyzers:** Kuromoji for Japanese, standard for English

### Test Class Setup Pattern

```java
public class MyTest {
    private static OpenSearchRunner runner;
    private static Client client;

    @BeforeClass
    public static void beforeClass() {
        runner = new OpenSearchRunner();
        runner.build(newConfigs()
            .clusterName("TestCluster")
            .numOfNode(1));
        client = runner.client();
    }

    @Before
    public void before() {
        // Clean indices before each test
        runner.admin().indices().prepareDelete("_all").execute().actionGet();
    }

    @AfterClass
    public static void afterClass() {
        runner.close();
        runner.clean();
    }

    @Test
    public void testSomething() {
        // Test implementation
    }
}
```

### Key Test Classes

| Test Class | Coverage |
|-----------|----------|
| `SuggesterRefactoringTest` | Index lifecycle, alias management |
| `SuggestIndexerTest` | Indexing operations, content parsing |
| `DefaultContentsParserTest` | Text processing pipeline |
| `NormalizerChainTest` | Normalizer composition |
| `SuggestIndexWriterTest` | Bulk writing, merging logic |
| `SuggestRequestTest` | Query building, scoring |

### Testing Best Practices

1. **Integration Tests**
   - Use embedded OpenSearch for realistic scenarios
   - Test full pipeline (index → query → result)
   - Verify index alias operations

2. **Unit Tests**
   - Test normalizers/converters in isolation
   - Validate content parser behavior
   - Check request/response serialization

3. **Edge Cases**
   - Duplicate word handling
   - Bad word exclusion
   - Timeout scenarios
   - Index alias boundary conditions

4. **Test Data**
   - Use realistic multilingual content
   - Include edge cases (empty strings, special characters)
   - Test both Japanese and English text

### Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=SuggesterRefactoringTest

# Specific test method
mvn test -Dtest=SuggesterRefactoringTest#testSwitchIndex

# With debug output
mvn surefire:test -Dmaven.surefire.debug=true

# With coverage
mvn clean jacoco:prepare-agent test jacoco:report
```

---

## Common Tasks

### Task 1: Add New Normalizer

**Example:** Add a custom normalizer to convert special characters

1. **Create normalizer class:**
```java
package org.codelibs.fess.suggest.normalizer;

public class CustomNormalizer implements Normalizer {
    @Override
    public String normalize(String text, String field, String... langs) {
        // Implementation
        return processedText;
    }
}
```

2. **Add to chain:**
```java
Normalizer normalizer = new NormalizerChain(
    new ICUNormalizer("NFKC"),
    new CustomNormalizer()
);

Suggester suggester = Suggester.builder()
    .normalizer(normalizer)
    .build(client, "my-suggest");
```

3. **Test:**
```java
@Test
public void testCustomNormalizer() {
    CustomNormalizer normalizer = new CustomNormalizer();
    String result = normalizer.normalize("input", "field");
    assertEquals("expected", result);
}
```

**Files to reference:**
- `src/main/java/org/codelibs/fess/suggest/normalizer/ICUNormalizer.java`
- `src/test/java/org/codelibs/fess/suggest/normalizer/ICUNormalizerTest.java`

### Task 2: Modify Index Mappings

**Example:** Add new field to suggestion index

1. **Update mapping file:**
   - Edit: `src/main/resources/suggest_indices/suggest/mappings-default.json`
   - Add new field definition

2. **Update constants:**
```java
// In FieldNames.java
public static final String NEW_FIELD = "new_field";
```

3. **Update SuggestItem:**
```java
// Add getter/setter
public String getNewField() { return newField; }
public void setNewField(String value) { this.newField = value; }

// Update getSource() method to include new field
// Update parseSource() method to read new field
```

4. **Recreate indices:**
```java
// In test or maintenance code
suggester.createNextIndex();  // Creates index with new mapping
```

**Files to modify:**
- `src/main/resources/suggest_indices/suggest/mappings-default.json`
- `src/main/java/org/codelibs/fess/suggest/constants/FieldNames.java`
- `src/main/java/org/codelibs/fess/suggest/entity/SuggestItem.java`

### Task 3: Implement Custom Content Parser

**Example:** Parse custom document format

1. **Implement ContentsParser:**
```java
package org.codelibs.fess.suggest.index.contents;

public class CustomContentsParser implements ContentsParser {
    @Override
    public List<SuggestItem> parseDocument(Map<String, Object> document,
                                          String[] supportedFields,
                                          String[] tagFieldNames,
                                          String[] roleFieldNames,
                                          String langFieldName) {
        // Custom parsing logic
        return suggestItems;
    }
}
```

2. **Use in indexer:**
```java
SuggestIndexer indexer = suggester.indexer();
indexer.setContentsParser(new CustomContentsParser());
indexer.indexFromDocument(documents);
```

**Files to reference:**
- `src/main/java/org/codelibs/fess/suggest/index/contents/DefaultContentsParser.java`
- `src/test/java/org/codelibs/fess/suggest/index/contents/DefaultContentsParserTest.java`

### Task 4: Debug Suggestion Scoring

**Understanding the scoring pipeline:**

1. **Query Construction** (in `SuggestRequest.java:buildQuery()`):
   - Last term: Prefix query (e.g., "sea" matches "search", "seattle")
   - Other terms: Term queries (exact matches)

2. **Function Score Application** (in `SuggestRequest.java:buildFunctionScoreQuery()`):
   ```java
   FunctionScoreQuery with:
   ├── Prefix match boost (weight: prefixMatchWeight, default 2.0)
   ├── Document frequency (field: docFreq, modifier: LOG2P)
   ├── Query frequency (field: queryFreq, modifier: LOG2P)
   └── User boost (field: userBoost, raw value)
   ```

3. **Debugging steps:**
   ```java
   // Enable detailed scoring
   SuggestResponse response = suggester.suggest()
       .setQuery("search")
       .setSuggestDetail(true)  // Include full SuggestItem details
       .execute()
       .getResponse();

   // Inspect scores
   response.getItems().forEach(item -> {
       System.out.println("Text: " + item.getText());
       System.out.println("Score: " + item.getScore());
       System.out.println("DocFreq: " + item.getDocFreq());
       System.out.println("QueryFreq: " + item.getQueryFreq());
       System.out.println("UserBoost: " + item.getUserBoost());
   });
   ```

**Files to reference:**
- `src/main/java/org/codelibs/fess/suggest/request/suggest/SuggestRequest.java` (lines 200-350)

### Task 5: Index Lifecycle Management

**Typical workflow for index updates:**

```java
Suggester suggester = Suggester.builder().build(client, "my-suggest");

// 1. Initial setup (first time)
suggester.createIndexIfNothing();

// 2. Create new index for bulk updates
suggester.createNextIndex();
// Now: update alias points to new index, search alias still on old

// 3. Index content into new index
SuggestIndexer indexer = suggester.indexer();
// ... indexing operations ...

// 4. Switch search traffic to new index
suggester.switchIndex();
// Now: both aliases point to new index

// 5. Cleanup old indices
suggester.removeDisableIndices();
// Deletes indices without aliases
```

**Alias States:**

```
After createIndexIfNothing():
  my-suggest           → my-suggest.20250123120000
  my-suggest.update    → my-suggest.20250123120000

After createNextIndex():
  my-suggest           → my-suggest.20250123120000 (old, still serving reads)
  my-suggest.update    → my-suggest.20250123120100 (new, receiving writes)

After switchIndex():
  my-suggest           → my-suggest.20250123120100 (new, serving reads)
  my-suggest.update    → my-suggest.20250123120100 (new, receiving writes)

After removeDisableIndices():
  my-suggest.20250123120000 deleted (no aliases)
```

**Files to reference:**
- `src/main/java/org/codelibs/fess/suggest/Suggester.java` (lines 186-342)
- `src/test/java/org/codelibs/fess/suggest/SuggesterRefactoringTest.java`

---

## Code Conventions

### Java Style

- **Formatter:** Eclipse formatter configuration at `src/config/eclipse/formatter/java.xml`
- **Indentation:** 4 spaces (no tabs)
- **Line Length:** 140 characters maximum
- **License Headers:** Required on all `.java` files (use `mvn license:format`)

### Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Classes | PascalCase | `SuggestIndexer` |
| Interfaces | PascalCase | `ReadingConverter` |
| Methods | camelCase | `createIndexIfNothing()` |
| Constants | UPPER_SNAKE_CASE | `DEFAULT_MAX_READING_NUM` |
| Packages | lowercase | `org.codelibs.fess.suggest` |

### JavaDoc Requirements

- **All public classes** must have class-level JavaDoc
- **All public methods** must have method-level JavaDoc with:
  - Description of what the method does
  - `@param` for each parameter
  - `@return` for return values
  - `@throws` for exceptions

**Example:**
```java
/**
 * Creates a new SuggestRequestBuilder for querying suggestions.
 * The builder allows configuring query parameters such as query text,
 * size, tags, roles, and languages before executing the suggestion request.
 *
 * @return A SuggestRequestBuilder instance configured with the current
 *         client, reading converter, and normalizer.
 */
public SuggestRequestBuilder suggest() {
    return new SuggestRequestBuilder(client, readingConverter, normalizer)
        .setIndex(getSearchAlias(index));
}
```

### Code Organization

- **Package by Feature:** Group related classes by functionality
- **Minimize Public API:** Only expose what's necessary
- **Immutability:** Prefer immutable objects where possible
- **Null Safety:** Use `Objects.requireNonNull()` for required parameters
- **Logging:** Use appropriate log levels (debug, info, warn, error)

### Exception Handling

```java
// Good: Specific exception with context
if (indices.size() != EXPECTED_INDEX_COUNT) {
    if (logger.isDebugEnabled()) {
        logger.debug("Unexpected indices num: {}", indices.size());
    }
    throw new SuggesterException("Unexpected indices num: " + indices.size());
}

// Bad: Generic exception, no logging
if (indices.size() != 1) {
    throw new RuntimeException("Error");
}
```

### Resource Management

```java
// Good: try-with-resources
try (InputStream is = getClass().getClassLoader()
        .getResourceAsStream("suggest_indices/suggest.json")) {
    if (is == null) {
        throw new IOException("Resource not found");
    }
    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
}

// Bad: Manual closing
InputStream is = getClass().getClassLoader()
    .getResourceAsStream("suggest_indices/suggest.json");
String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
is.close();  // Can be skipped if exception occurs
```

---

## Troubleshooting

### Common Issues

#### Issue: Index Creation Fails

**Symptoms:**
```
SuggesterException: Failed to create index
```

**Debugging:**
1. Check OpenSearch cluster health:
   ```java
   ClusterHealthResponse health = client.admin().cluster()
       .prepareHealth()
       .execute()
       .actionGet();
   System.out.println("Cluster status: " + health.getStatus());
   ```

2. Verify index settings and mappings:
   - Check JSON files in `src/main/resources/suggest_indices/`
   - Validate JSON syntax
   - Ensure analyzer configurations are valid

3. Check permissions:
   - Verify OpenSearch user has index creation permissions
   - Check cluster settings for index limits

**Files to check:**
- `src/main/resources/suggest_indices/suggest.json`
- `src/main/resources/suggest_indices/suggest/mappings-default.json`

#### Issue: Suggestions Not Appearing

**Symptoms:**
- Empty results from `suggester.suggest()`
- Expected suggestions missing

**Debugging:**
1. Verify documents are indexed:
   ```java
   long count = suggester.getAllWordsNum();
   System.out.println("Total words: " + count);
   ```

2. Refresh indices:
   ```java
   suggester.refresh();
   ```

3. Check query normalization:
   ```java
   String normalized = normalizer.normalize("your query", "field");
   System.out.println("Normalized: " + normalized);
   ```

4. Verify index aliases:
   ```java
   GetAliasesResponse aliases = client.admin().indices()
       .prepareGetAliases(suggester.getIndex())
       .execute()
       .actionGet();
   // Check which index the search alias points to
   ```

5. Test direct OpenSearch query:
   ```java
   SearchResponse response = client.prepareSearch(suggester.getIndex())
       .setQuery(QueryBuilders.matchAllQuery())
       .execute()
       .actionGet();
   System.out.println("Total hits: " + response.getHits().getTotalHits());
   ```

#### Issue: Bad Words Not Filtering

**Symptoms:**
- Words that should be excluded appear in suggestions

**Debugging:**
1. Check bad word settings:
   ```java
   String[] badWords = suggester.settings().badword().get();
   System.out.println("Bad words: " + Arrays.toString(badWords));
   ```

2. Verify bad word matching logic in `SuggestItem.java:isBadWord()`:
   - Bad words are normalized before comparison
   - Matching uses `String.contains()` for partial matches

3. Update bad words:
   ```java
   suggester.indexer()
       .addBadWord(new String[]{"spam", "inappropriate"})
       .execute();
   ```

#### Issue: Memory/Performance Problems

**Symptoms:**
- OutOfMemoryError
- Slow query/indexing performance
- High CPU usage

**Solutions:**

1. **Increase JVM heap:**
   ```bash
   export MAVEN_OPTS="-Xmx2g -Xms512m"
   mvn test
   ```

2. **Optimize batch size:**
   ```java
   // Reduce batch size for bulk operations
   indexer.indexFromDocument(reader,
       2,    // threads (reduce if high CPU)
       50    // batch size (reduce if high memory)
   );
   ```

3. **Configure timeouts:**
   ```java
   SuggestSettings.TimeoutSettings timeouts =
       suggester.settings().getTimeoutSettings();
   timeouts.setSearchTimeout(TimeValue.timeValueSeconds(30));
   ```

4. **Monitor index sizes:**
   ```bash
   # Via OpenSearch API
   curl -X GET "localhost:9200/_cat/indices/my-suggest*?v"
   ```

5. **Limit reading conversions:**
   ```java
   ReadingConverterChain converter = new ReadingConverterChain();
   // Limit max readings to reduce memory
   converter.addConverter(new KatakanaConverter(), 5);
   ```

#### Issue: Test Failures

**Symptoms:**
- Intermittent test failures
- OpenSearch cluster startup issues

**Solutions:**

1. **Increase cluster timeout:**
   ```java
   @BeforeClass
   public static void beforeClass() {
       runner = new OpenSearchRunner();
       runner.build(newConfigs()
           .clusterName("TestCluster")
           .numOfNode(1)
           .useLogger());  // Enable logging
       runner.waitForCluster(Status.YELLOW, 30, TimeUnit.SECONDS);
       client = runner.client();
   }
   ```

2. **Clean indices between tests:**
   ```java
   @Before
   public void before() {
       runner.admin().indices().prepareDelete("_all").execute().actionGet();
       runner.admin().cluster().prepareHealth().setWaitForYellowStatus()
           .execute().actionGet();
   }
   ```

3. **Check port conflicts:**
   - OpenSearch runner uses random ports by default
   - Check if specific ports are required and available

### Logging Configuration

**Enable debug logging for troubleshooting:**

In test resources (`src/test/resources/log4j2.xml`):
```xml
<Logger name="org.codelibs.fess.suggest" level="DEBUG"/>
```

**Log categories:**
- `org.codelibs.fess.suggest.Suggester` - Index operations
- `org.codelibs.fess.suggest.index.SuggestIndexer` - Indexing details
- `org.codelibs.fess.suggest.request` - Query processing

---

## Important Implementation Notes

### Thread Safety

- **Suggester:** Thread-safe for queries
- **SuggestIndexer:** Thread-safe for indexing operations
- **SuggestSettings:** NOT thread-safe for modifications (use synchronization)
- **ReadingConverter/Normalizer:** Should be stateless and thread-safe

### Performance Considerations

1. **Bulk Operations:**
   - Use `SuggestIndexer.index(SuggestItem[])` for batch indexing
   - Larger batches (100-500) are more efficient but use more memory

2. **Async Operations:**
   - Query log indexing supports async with Deferred callbacks
   - Use thread pool size appropriate to system resources

3. **Index Refresh:**
   - Don't call `refresh()` too frequently (expensive operation)
   - OpenSearch auto-refreshes by default every 1 second

4. **Reading Conversions:**
   - Limit `maxReadingNum` to prevent memory issues
   - Consider disabling reading converters for large content

### OpenSearch Version Compatibility

- Designed for OpenSearch 2.x+
- Also compatible with Elasticsearch (client abstraction layer)
- Test with target OpenSearch version before deploying

### Character Encoding

- All text processing uses UTF-8
- Normalizers handle Unicode correctly (ICUNormalizer)
- Be aware of full-width/half-width character conversions in Japanese

---

## Quick Reference

### Essential Files

| File | Purpose |
|------|---------|
| `Suggester.java` | Main entry point, index management |
| `SuggestIndexer.java` | Indexing operations |
| `SuggestRequest.java` | Query building, scoring logic |
| `SuggestSettings.java` | Configuration management |
| `SuggestItem.java` | Domain model, serialization |
| `DefaultContentsParser.java` | Text processing pipeline |
| `Deferred.java` | Async pattern implementation |

### Key Constants

```java
// Field names (FieldNames.java)
TEXT, READING_PREFIX, QUERY_FREQ, DOC_FREQ, USER_BOOST, KINDS, TIMESTAMP

// Default values
MAX_READING_NUM = 10
PREFIX_MATCH_WEIGHT = 2.0f
SUGGEST_DETAIL_DEFAULT = true
```

### Useful Commands

```bash
# Build and test
mvn clean install

# Format and license
mvn formatter:format license:format

# Coverage report
mvn clean jacoco:prepare-agent test jacoco:report

# Run single test
mvn test -Dtest=SuggesterRefactoringTest#testSwitchIndex

# Skip tests
mvn install -DskipTests
```

---

## Additional Resources

- **Project README:** `/home/user/fess-suggest/README.md`
- **POM file:** `/home/user/fess-suggest/pom.xml`
- **OpenSearch Documentation:** https://opensearch.org/docs/
- **Fess Project:** https://fess.codelibs.org/

---

*This document is maintained as part of the Fess Suggest project. Update it when making significant architectural changes or adding new features.*

**Last Updated:** 2025-01-23
