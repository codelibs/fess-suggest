
fess-suggest
============

Library for suggest.

## Usage

### Suggester生成

```java
String suggestId = "id";
Suggester suggester = Suggester.builder().build(client, suggestId);

```

Suggesterを生成すると指定したIDに対応した以下の設定がElasticsearchのインデックスに作成されます。

* .suggest
    * suggesterの設定
    * インデックス/タイプ名
    * tagフィールド名
    * roleフィールド名
* .suggest-array
    * suggesterの設定（配列）
    * 対応フィールド名

２回目以降のインスタンス生成時には、作成済みの設定が再利用されます。

### Suggestリクエスト

```java
SuggestResponse response = suggester.suggest().setQuery("kensaku").execute().getResponse();
```

### Suggest async widh lambda

```
suggester.suggest().setQuery("kensaku").execute()
  .done(
    response -> {}
  ).error(
    t -> {}
  );
```

### サジェストドキュメントの登録

```java
String[][] readings = new String[2][];
readings[0] = new String[] { "kensaku", "fuga" };
readings[1] = new String[] { "enjin", "fuga" };
String[] tags = new String[] { "tag1", "tag2" };
String[] roles = new String[] { "role1", "role2", "role3" };
suggester.indexer().index(new SuggestItem(new String[] { "検索", "エンジン" }, readings, 1, tags, roles, SuggestItem.Kind.DOCUMENT));
```

### Add suggest documents from source of index

Elasticsearchにインデックスされているコンテンツを解析してサジェストドキュメントを生成します。

```java
DocumentReader reader = new ESSourceReader(
    client,
    suggester.settings(),
    "contentIndexName",
    "contentTypeName");
suggester.indexer().indexFromDocument(reader, 2, 100).getResponse();
```

### Add suggest document from queryLog

クエリーログ（query_string）を解析してサジェストドキュメントを登録

```java
QueryLog queryLog = new QueryLog("field1:value1", null);
suggester.indexer().indexFromQueryLog(queryLog);
```

### Add suggest document from search words

検索語を解析してサジェストドキュメントを登録

```java
SuggestIndexResponse indexResponse = suggester.indexer().indexFromSearchWord("検索　エンジン", null, null, null, 1);
```
