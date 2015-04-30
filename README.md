
fess-suggest
============

Library for suggest.

## Usage

### Create suggester instance

```java
String suggestId = "id";
Suggester suggester = Suggester.builder().build(client, suggestId);

```

### Suggest

```java
SuggestResponse response = suggester.suggest().setQuery("kensaku").execute().getResponse();
```

### Suggest async

```
suggester.suggest().setQuery("kensaku").execute()
  .done(
    response -> {}
  ).error(
    t -> {}
  );
```

### Add suggest document

```java
String[][] readings = new String[2][];
readings[0] = new String[] { "kensaku", "fuga" };
readings[1] = new String[] { "enjin", "fuga" };
String[] tags = new String[] { "tag1", "tag2" };
String[] roles = new String[] { "role1", "role2", "role3" };
suggester.indexer().index(new SuggestItem(new String[] { "検索", "エンジン" }, readings, 1, tags, roles, SuggestItem.Kind.DOCUMENT));
```

### Add suggest documents from source of index

```java
DocumentReader reader = new ESSourceReader(
    client,
    suggester.settings(),
    "contentIndexName",
    "contentTypeName");
suggester.indexer().indexFromDocument(reader, 2, 100).getResponse();
```

### Add suggest document from queryLog

```java
QueryLog queryLog = new QueryLog("field1:value1", null);
suggester.indexer().indexFromQueryLog(queryLog);
```
