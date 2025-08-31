# Fess Suggest

[![Java CI with Maven](https://github.com/codelibs/fess-suggest/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/fess-suggest/actions/workflows/maven.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.codelibs.fess/fess-suggest/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.codelibs.fess/fess-suggest)

A powerful Java library that provides intelligent search suggestion functionality built on top of OpenSearch/Elasticsearch. It offers auto-completion, query suggestions, and popular word analytics for search applications.

## Key Features

- **Smart Query Suggestions**: Real-time auto-completion and search suggestions
- **Multi-language Support**: Built-in support for Japanese text processing with Kuromoji analyzer
- **Popular Words Analytics**: Track and analyze frequently searched terms
- **Flexible Text Processing**: Configurable converters and normalizers for text transformation
- **OpenSearch Integration**: Seamless integration with OpenSearch/Elasticsearch clusters
- **Asynchronous Operations**: Non-blocking suggestion requests with callback support
- **Index Management**: Automatic index creation, switching, and maintenance
- **Customizable Scoring**: User boost, document frequency, and query frequency weighting

## Technology Stack

- **Java**: 21+ (configured via parent POM)
- **OpenSearch**: Latest (provided scope)
- **Apache Lucene**: Query parsing and text analysis
- **ICU4J**: Unicode text processing and normalization
- **JUnit 4**: Testing framework
- **Maven**: Build and dependency management

## Quick Start

### Prerequisites

- Java 21 or higher
- OpenSearch/Elasticsearch cluster (2.x+ recommended)
- Maven 3.8+ for building from source

### Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.codelibs.fess</groupId>
    <artifactId>fess-suggest</artifactId>
    <version>15.2.0-SNAPSHOT</version>
</dependency>
```

### Basic Usage

#### 1. Create Suggester Instance

```java
import org.codelibs.fess.suggest.Suggester;
import org.opensearch.client.Client;

// Initialize with your OpenSearch client
String suggestId = "my-suggest-index";
Suggester suggester = Suggester.builder().build(client, suggestId);
```

#### 2. Add Suggestion Documents

```java
import org.codelibs.fess.suggest.entity.SuggestItem;

// Create suggestion item with text, readings, and metadata
String[][] readings = new String[2][];
readings[0] = new String[]{"kensaku", "engine"};
readings[1] = new String[]{"search", "injin"};

String[] tags = new String[]{"technology", "search"};
String[] roles = new String[]{"admin", "user"};

SuggestItem item = new SuggestItem(
    new String[]{"Search Engine", "検索エンジン"}, // text variations
    readings,                                      // pronunciation readings
    1,                                            // boost score
    tags,                                         // categorization tags
    roles,                                        // access roles
    SuggestItem.Kind.DOCUMENT                     // suggestion type
);

suggester.indexer().index(item);
```

#### 3. Get Suggestions

```java
import org.codelibs.fess.suggest.request.suggest.SuggestResponse;

// Synchronous suggestion request
SuggestResponse response = suggester.suggest()
    .setQuery("sea")                    // user input
    .setSize(10)                        // max suggestions
    .execute()
    .getResponse();

// Process suggestions
response.getItems().forEach(item -> {
    System.out.println("Suggestion: " + item.getText()[0]);
    System.out.println("Score: " + item.getScore());
});
```

#### 4. Asynchronous Suggestions

```java
suggester.suggest()
    .setQuery("sea")
    .execute()
    .done(response -> {
        // Handle successful response
        response.getItems().forEach(item -> 
            System.out.println("Async suggestion: " + item.getText()[0])
        );
    })
    .error(throwable -> {
        // Handle error
        System.err.println("Error: " + throwable.getMessage());
    });
```

## Advanced Usage

### Index from Existing Documents

```java
import org.codelibs.fess.suggest.index.contents.document.ESSourceReader;

// Index suggestions from existing Elasticsearch documents
DocumentReader reader = new ESSourceReader(
    client,
    suggester.settings(),
    "content-index",        // source index
    "document"             // document type
);

suggester.indexer()
    .indexFromDocument(reader, 2, 100)  // threads=2, batch=100
    .getResponse();
```

### Index from Query Logs

```java
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;

// Add suggestions from search query logs
QueryLog queryLog = new QueryLog("user search query", "user123");
suggester.indexer().indexFromQueryLog(queryLog);
```

### Popular Words Analytics

```java
import org.codelibs.fess.suggest.request.popularwords.PopularWordsResponse;

PopularWordsResponse popularWords = suggester.popularWords()
    .setSize(20)                    // top 20 words
    .setQuery("tech*")              // filter pattern
    .execute()
    .getResponse();

popularWords.getItems().forEach(item -> {
    System.out.println("Popular: " + item.getText() + 
                      " (freq: " + item.getDocFreq() + ")");
});
```

### Custom Text Processing

```java
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.converter.ReadingConverterChain;
import org.codelibs.fess.suggest.normalizer.ICUNormalizer;

// Configure custom settings
SuggestSettings settings = SuggestSettings.builder()
    .analyzer(analyzers -> {
        // Configure custom analyzers
        analyzers.addAnalyzer("custom", customAnalyzerSettings);
    })
    .build();

Suggester customSuggester = Suggester.builder()
    .settings(settings)
    .readingConverter(new ReadingConverterChain())
    .normalizer(new ICUNormalizer())
    .build(client, "custom-suggest");
```

## Configuration Options

### Suggester Settings

```java
import org.codelibs.fess.suggest.settings.SuggestSettingsBuilder;

SuggestSettings settings = SuggestSettingsBuilder.builder()
    .arraySettings(arraySettings -> {
        arraySettings.setDefaultBadWords(new String[]{"spam", "inappropriate"});
    })
    .analyzerSettings(analyzerSettings -> {
        analyzerSettings.setReadingAnalyzer("kuromoji_reading");
        analyzerSettings.setNormalizeAnalyzer("keyword");
    })
    .badWordSettings(badWordSettings -> {
        badWordSettings.setList(new String[]{"blocked", "terms"});
    })
    .elevateWordSettings(elevateWordSettings -> {
        elevateWordSettings.setElevateWords(Collections.singletonList(
            new ElevateWord("priority", 2.0f, Collections.emptyList())
        ));
    })
    .build();
```

### Request Parameters

```java
SuggestResponse response = suggester.suggest()
    .setQuery("search term")
    .setSize(10)                           // max results
    .setTags(new String[]{"category"})      // filter by tags
    .setRoles(new String[]{"user"})         // filter by roles  
    .setLanguages(new String[]{"en", "ja"}) // language preference
    .setFields(new String[]{"title"})       // search specific fields
    .execute()
    .getResponse();
```

## Project Structure

```
src/
├── main/java/org/codelibs/fess/suggest/
│   ├── Suggester.java              # Main suggester API
│   ├── SuggesterBuilder.java       # Builder pattern implementation
│   ├── entity/
│   │   ├── SuggestItem.java        # Core suggestion data structure
│   │   └── ElevateWord.java        # Promoted words configuration
│   ├── request/                    # Request/Response handling
│   │   ├── suggest/                # Suggestion requests
│   │   └── popularwords/           # Popular words requests
│   ├── index/                      # Indexing functionality
│   │   ├── SuggestIndexer.java     # Main indexing API
│   │   ├── contents/               # Content parsers and readers
│   │   └── writer/                 # Index writing strategies
│   ├── converter/                  # Text conversion utilities
│   ├── normalizer/                 # Text normalization
│   ├── analysis/                   # Custom analyzers
│   └── settings/                   # Configuration management
├── main/resources/
│   ├── suggest_indices/            # Index mappings and settings
│   └── suggest_settings/           # Default configurations
└── test/                          # Comprehensive test suite
```

## Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/codelibs/fess-suggest.git
cd fess-suggest

# Compile the project
mvn compile

# Run tests
mvn test

# Package JAR
mvn package

# Install to local repository
mvn install
```

### Code Quality Commands

```bash
# Format code (Eclipse formatter)
mvn formatter:format

# Check/apply license headers
mvn license:check
mvn license:format

# Generate test coverage report
mvn jacoco:prepare-agent test jacoco:report

# Generate API documentation
mvn javadoc:javadoc
```

### Testing

The project uses JUnit 4 with embedded OpenSearch for integration testing:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=SuggesterTest

# Run with verbose output
mvn surefire:test -Dmaven.surefire.debug=true
```

### Running Tests with Coverage

```bash
mvn clean jacoco:prepare-agent test jacoco:report
```

Coverage reports are generated in `target/site/jacoco/`.

## Common Use Cases

### E-commerce Search

```java
// Product search suggestions
SuggestItem product = new SuggestItem(
    new String[]{"iPhone 15 Pro", "Apple iPhone 15 Pro"},
    new String[][]{{"iphone"}, {"apple", "phone"}},
    1.5f,                               // boost popular products
    new String[]{"electronics", "mobile"},
    new String[]{"customer"},
    SuggestItem.Kind.DOCUMENT
);
suggester.indexer().index(product);
```

### Content Management

```java
// Article/blog suggestions
DocumentReader reader = new ESSourceReader(
    client, 
    suggester.settings(), 
    "articles-index", 
    "article"
);
suggester.indexer().indexFromDocument(reader, 4, 50);
```

### Search Analytics

```java
// Track user queries for analytics
QueryLog userQuery = new QueryLog("machine learning tutorials", "user456");
suggester.indexer().indexFromQueryLog(userQuery);

// Get trending searches
PopularWordsResponse trending = suggester.popularWords()
    .setSize(10)
    .execute()
    .getResponse();
```

## Troubleshooting

### Common Issues

**Index Creation Fails**
- Verify OpenSearch cluster is accessible
- Check index permissions and mappings
- Ensure sufficient cluster resources

**Suggestions Not Appearing**
- Confirm documents are indexed: `suggester.refresh()`
- Check query formatting and filters
- Verify analyzer configurations

**Performance Issues**
- Increase thread pool size in SuggesterBuilder
- Optimize batch sizes for indexing operations
- Review OpenSearch cluster performance

**Memory Usage**
- Configure appropriate JVM heap settings
- Monitor index sizes and optimize mappings
- Use streaming for large data imports

### Debug Logging

Enable debug logging for detailed troubleshooting:

```java
// Add to your logging configuration
logger.debug.org.codelibs.fess.suggest=DEBUG
```

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Follow coding standards: `mvn formatter:format`
4. Add tests for new functionality
5. Ensure all tests pass: `mvn test`
6. Check license headers: `mvn license:format`
7. Commit changes: `git commit -m 'Add amazing feature'`
8. Push to branch: `git push origin feature/amazing-feature`
9. Open a Pull Request

### Code Style

- Use Eclipse formatter configuration (`src/config/eclipse/formatter/java.xml`)
- Follow existing naming conventions
- Add comprehensive JavaDoc for public APIs
- Write unit and integration tests

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

