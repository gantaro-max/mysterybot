# AGENTS.md — MysteryBot Codex ガイド

## Codexの役割定義

Codex はこのプロジェクトにおいて **コーダー・テスター・一次レビューリーダーの役割** を担う。

### 担当する業務

| 業務 | 内容 | 成果物 |
|:--|:--|:--|
| **実装** | Claude が作成した実装指示書をもとにコードを実装する | ソースコード |
| **テスト** | 実装した機能の動作確認・テストコードの作成を行う | テスト結果・テストコード |
| **一次レビュー（リーダー）** | 複数のサブエージェントを並列起動し、実装コードの一次レビューをとりまとめる | レビューレポート |

### やらないこと

- 要件定義・基本設計は行わない。設計上の疑問は実装前に Claude へフィードバックする。
- ドキュメント（`docs/` 配下・`CHANGELOG.md` 等）の更新は行わない。ドキュメント更新は Claude が担当する。
- 実装指示書にない範囲の機能追加・リファクタリングは勝手に行わない。

---

## 開発ワークフロー（Codex視点）

```
1. Claude から実装指示書を受け取る
        ↓
2. 実装指示書と設計書（docs/）を読み、不明点があれば着手前に確認する
        ↓
3. コードを実装する
        ↓
4. テストを実行し、完了条件をすべて満たすことを確認する
        ↓
5. 複数サブエージェントによる一次レビューを実施する（後述）
        ↓
6. レビュー結果と実装内容を Claude へ報告する
```

---

## 一次レビューの進め方

実装完了後、以下の観点で **複数のサブエージェントを並列起動** して一次レビューを行い、結果をとりまとめて Claude へ報告する。

| エージェント | レビュー観点 |
|:--|:--|
| セキュリティレビュー | 認証・認可・CSRF・IDOR・入力検証・XSS の抜け漏れ |
| ロジックレビュー | 実装指示書の完了条件を満たしているか、ロジックの正確性 |
| コード品質レビュー | 既存の設計パターンとの整合性、可読性、不要なコードの混入 |
| テストレビュー | テストケースの網羅性、エッジケースの検証 |

レビュー結果はエージェントごとにまとめ、問題があれば自己修正してから Claude へ渡す。

---

## コーディング規約

### コントローラー
- リドル操作は必ず `EventAdminService.getRiddleOwnedBy(id, groupId)` で所有権を確認する
- 新しいエンドポイントを追加する場合、`docs/api.md` の設計と一致させる

### フォーム
- すべての POST フォームは `th:action` を使う（CSRF トークンが自動付与される）
- `action=` の直書き禁止

### SQL
- SQL は `src/main/resources/mappers/` の XML に書く
- リポジトリインターフェースに対応するメソッドを定義する

### セキュリティ（実装時の必須チェック）
- パスワードは `BCryptPasswordEncoder` でハッシュ化（平文保存禁止）
- ファイルアップロードはマジックバイト検証 + サイズ上限チェックを行う
- シークレットは `application.properties` に直接書かず、環境変数で注入する
- LINE Webhook `/callback` は CSRF 除外が必須（`SecurityConfig` で設定済み）

---

## プロジェクト概要（クイックリファレンス）

- Java 21 / Spring Boot 4.0.1 / MyBatis XML マッパー / Thymeleaf + Bootstrap 5
- セッションベース認証（`HttpSession` の `loginGroupId`）、`"admin"` でスーパーAdmin 権限
- `SecurityFilterChain` で CSRF 保護（`/callback` のみ除外）

### コントローラー構成

| プレフィックス | クラス | 役割 |
|:--|:--|:--|
| `/auth` | `AuthController` | ログイン・登録・ログアウト |
| `/admin` | `AdminController` | スーパーAdmin 管理 |
| `/user` | `UserController` | イベント主催者操作 |
| `/callback` | `LineWebhookController` | LINE Webhook |
| `/public` | `ImageController` | 画像配信（認証不要） |

### 主要コマンド

```bash
./gradlew bootRun    # アプリ起動
./gradlew build      # ビルド
./gradlew test       # テスト実行
docker-compose up -d # ローカルDB起動
```

### 設計書・仕様書の参照先

| ドキュメント | 内容 |
|:--|:--|
| [docs/requirements.md](docs/requirements.md) | 要件定義 |
| [docs/architecture.md](docs/architecture.md) | アーキテクチャ・設計 |
| [docs/database.md](docs/database.md) | テーブル設計 |
| [docs/api.md](docs/api.md) | エンドポイント設計 |
| [SECURITY.md](SECURITY.md) | セキュリティポリシー |
| [CLAUDE.md](CLAUDE.md) | Claude（PM）の役割・ワークフロー |
