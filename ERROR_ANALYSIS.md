# ビルドエラーの分析と解決策

## 問題の概要

`mvn package` を実行すると以下のエラーが発生：

```
[FATAL] Non-resolvable parent POM for org.codelibs.fess:fess-suggest:15.4.0-SNAPSHOT:
The following artifacts could not be resolved: org.codelibs.fess:fess-parent:pom:15.4.0-SNAPSHOT (absent):
Could not transfer artifact org.codelibs.fess:fess-parent:pom:15.4.0-SNAPSHOT from/to snapshots.central.sonatype.com
```

## 根本原因

### 1. ネットワークの問題
- `central.sonatype.com` への接続に失敗（"Temporary failure in name resolution"）
- parent POM（fess-parent:15.4.0-SNAPSHOT）がダウンロードできない
- SNAPSHOTバージョンのため、リモートリポジトリへのアクセスが必須

### 2. 潜在的なAPI互換性の問題
Transport ClientでのPIT API使用について、以下のメソッドの存在確認が必要：
- `client.createPit(CreatePitRequest)`
- `client.deletePit(DeletePitRequest, ActionListener)`
- `SearchRequestBuilder.setPointInTime(PointInTimeBuilder)`
- `SearchRequestBuilder.searchAfter(Object[])`

これらのメソッドがTransport Clientに存在しない場合、コンパイルエラーが発生します。

## 解決策

### オプション1: ネットワーク問題の解決
1. ネットワーク接続を確認
2. プロキシ設定を確認（必要に応じてMaven settings.xmlで設定）
3. 再度 `mvn package` を実行

### オプション2: 一時的にリリースバージョンのparent POMを使用
```bash
# pom.xmlのparentセクションを変更
git show cdde2e6:pom.xml | grep -A 4 "<parent>" > parent_section.txt
# 15.3.0（リリースバージョン）に変更してビルド
```

### オプション3: API実装の確認と修正

OpenSearch Transport ClientでPIT APIが利用できない場合、以下の代替案を検討：

#### 代替案A: REST High Level Clientの使用
- 既に `PitApiIntegrationTest.java` で実装済み
- TestContainersでの統合テストには適している
- しかし、既存のコードベース全体を変更する必要がある

#### 代替案B: Scroll APIの改善版を使用
PIT APIが利用できない場合、Scroll APIのベストプラクティスを適用：
- タイムアウトの適切な設定
- Scroll IDの確実なクリーンアップ
- エラーハンドリングの改善

#### 代替案C: Transport ClientでのPIT API実装を確認
OpenSearch 2.x/3.x のドキュメントやソースコードで、Transport ClientでのPIT API使用方法を確認：
- `org.opensearch.action.search.CreatePitRequest`
- `org.opensearch.action.search.CreatePitResponse`
- `org.opensearch.action.search.DeletePitRequest`

## 次のステップ

1. **immediate**: parent POMのダウンロード問題を解決
2. **verify**: OpenSearchのバージョンを確認（parent POMから継承）
3. **test**: コンパイルが成功したら、APIの互換性を確認
4. **fallback**: APIが利用できない場合、代替実装を検討

## 検証方法

parent POMが解決できたら、以下でコンパイルエラーを確認：
```bash
mvn clean compile
```

もしコンパイルエラーが出た場合、エラーメッセージから：
- 存在しないメソッドやクラスを特定
- OpenSearchのバージョンとAPIの互換性を確認
- 必要に応じて実装を修正
