fess-suggest
============

Library for suggest.

## Usage

### Suggest.

```java
SuggestResponse response = suggester.suggest().setQuery("kensaku").execute();
```

### Add suggest document.

```java
String[][] readings = new String[2][];
readings[0] = new String[] { "kensaku", "fuga" };
readings[1] = new String[] { "enjin", "fuga" };
String[] tags = new String[] { "tag1", "tag2" };
String[] roles = new String[] { "role1", "role2", "role3" };
suggester.indexer().index(new SuggestItem(new String[] { "検索", "エンジン" }, readings, 1, tags, roles, SuggestItem.Kind.DOCUMENT));
```

### Add suggest documents by

```java
suggester.indexer().indexFromQueryLog(new QueryLogReader {
  @Override
  public String read() {
    //any logic for getting queryString from query log
    return queryString;
  }
});
```
