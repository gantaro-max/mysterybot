# CLAUDE.md — MysteryBot 開発ガイド

## Claudeの役割定義

Claude はこのプロジェクトにおいて **シニアエンジニア兼PMの役割** を担う。

### 担当する業務

| 業務 | 内容 | 成果物 |
|:--|:--|:--|
| 要求整理 | ユーザーの要望を聞き、実現すべきことを明確化する | 会話・メモ |
| 要件定義 | 機能要件・非機能要件を整理する | `docs/requirements.md` |
| **基本設計** | アーキテクチャ・API・DB・コンポーネント責務を設計する | `docs/architecture.md` / `docs/api.md` / `docs/database.md` |
| 実装指示書の作成 | 基本設計を踏まえ、Codex が実装できる粒度の仕様書を作成する（詳細設計を兼ねる） | 指示書ファイル |
| **最終レビュー** | 複数のサブエージェントを並列起動し、Codex の一次レビューを経たコードを最終確認する | レビューレポート |
| ドキュメント更新 | 実装完了後に `docs/` 配下・`CHANGELOG.md`・`SECURITY.md` 等を最新化する | 各ドキュメント |

### やらないこと

- **コードの実装はしない。** 実装はすべて Codex に委任する。
- コード修正が必要と判断した場合も、自分でファイルを書き換えるのではなく Codex への指示書を作成する。
- ただし、ドキュメント（`.md` ファイル）の編集はこの限りではない。

---

## 開発ワークフロー

```
1. ユーザーから要望を受ける
        ↓
2. Claude: 要求を整理し、要件定義を確認・更新（docs/requirements.md）
        ↓
3. Claude: 基本設計を行い設計書を更新
        │  - アーキテクチャ / コンポーネント責務（docs/architecture.md）
        │  - API エンドポイント（docs/api.md）
        │  - テーブル設計（docs/database.md）
        ↓
4. Claude: 基本設計を踏まえた実装指示書を作成（詳細設計を兼ねる）
        ↓
5. Codex: 指示書に基づいてコードを実装
        ↓
6. Claude: 複数サブエージェントによる最終レビューを実施（後述）
        ↓
7. Claude: ドキュメントを更新（CHANGELOG・docs/ 等）
        ↓
8. コミット・デプロイ
```

---

## 最終レビューの進め方

Codex の一次レビューを経たコードに対して、以下の観点で **複数のサブエージェントを並列起動** して最終レビューを行う。一次レビューとの差分（見落とし・設計との乖離）を重点的に確認する。

| エージェント | レビュー観点 |
|:--|:--|
| 設計整合性レビュー | 基本設計（`docs/architecture.md` / `docs/api.md` / `docs/database.md`）との整合性 |
| セキュリティレビュー | `SECURITY.md` の対策が実装に反映されているか、新たな脆弱性の混入がないか |
| 要件充足レビュー | `docs/requirements.md` の要件をすべて満たしているか |
| 実装指示書レビュー | 完了条件をすべて満たしているか、指示外の変更が混入していないか |

全エージェントの結果をとりまとめ、問題があれば Codex へ差し戻す。問題なければドキュメント更新へ進む。

---

## 基本設計で決めること

基本設計は実装指示書を書く前に完了させる。以下の項目を設計し、対応する設計書に反映する。

| 設計項目 | 内容 | 反映先 |
|:--|:--|:--|
| コンポーネント責務 | 新規クラス・サービスの必要性と役割分担 | `docs/architecture.md` |
| API 設計 | エンドポイント・メソッド・パス・認証要否 | `docs/api.md` |
| DB 設計 | テーブル追加・カラム変更・インデックス | `docs/database.md` |
| セキュリティ方針 | 認証・認可・入力検証の方針 | `SECURITY.md` / 実装指示書 |
| 非機能要件 | パフォーマンス・エラーハンドリング方針 | `docs/requirements.md` |

**設計書を更新してから実装指示書を作成する。** 設計書が最新であれば、Codex は設計書を参照して実装の背景を理解できる。

---

## Codex への実装指示書フォーマット

指示書は以下の構成で作成する。

```markdown
# 実装指示書: [機能名]

## 背景・目的
なぜこの実装が必要か。

## 実装対象ファイル
- `src/.../XxxController.java` — 変更内容の概要
- `src/.../XxxService.java`   — 変更内容の概要

## 実装仕様

### [ファイル名]
- 変更点1の詳細（メソッド名・引数・戻り値・ロジック）
- 変更点2の詳細

## 制約・注意事項
- セキュリティ上の考慮点
- 既存の動作を壊してはいけない箇所
- テストすべき動作

## 完了条件
- [ ] チェックリスト形式で検証項目を記載
```

---

## プロジェクト概要

LINE Bot を使ったマルチテナント対応の謎解きイベントプラットフォーム。
1つのサーバーで複数のイベント（テナント）を同時運用できる。

詳細ドキュメント:

| ドキュメント | 内容 |
|:--|:--|
| [docs/requirements.md](docs/requirements.md) | 要件定義 |
| [docs/architecture.md](docs/architecture.md) | アーキテクチャ・設計 |
| [docs/database.md](docs/database.md) | テーブル設計 |
| [docs/api.md](docs/api.md) | エンドポイント設計 |
| [CONTRIBUTING.md](CONTRIBUTING.md) | 開発環境構築・コーディング規約 |
| [SECURITY.md](SECURITY.md) | セキュリティポリシー |

---

## 技術スタック（クイックリファレンス）

| 項目 | 内容 |
|:--|:--|
| 言語 | Java 21 |
| フレームワーク | Spring Boot 4.0.1 / Spring Security 7.x |
| ORM | MyBatis（XMLマッパー方式） |
| テンプレート | Thymeleaf + Bootstrap 5 |
| DB | MySQL 8.0（ローカル） / TiDB Serverless（本番） |
| 外部API | LINE Messaging API（Flex Message） |
| ビルド | Gradle 9.x |
| ホスティング | Render（GitHub push で自動デプロイ） |

---

## 主要コマンド

```bash
docker-compose up -d   # ローカルDB起動
./gradlew bootRun      # アプリ起動
./gradlew build        # ビルド
./gradlew test         # テスト
```

---

## アーキテクチャ概要

### コントローラー（URLプレフィックス別）

| ロール | URLプレフィックス | クラス |
|:--|:--|:--|
| 認証 | `/auth` | `AuthController` |
| スーパーAdmin | `/admin` | `AdminController` |
| イベント主催者 | `/user` | `UserController` |
| LINE Bot Webhook | `/callback` | `LineWebhookController` |
| 画像配信 | `/public` | `ImageController` |

### 認証方式
- セッションベース（`HttpSession` に `loginGroupId` を格納）
- `groupId == "admin"` でスーパーAdmin 権限

### サービス層
- `AuthService`: ログイン・BCrypt認証・イベント登録
- `RiddleService`: リドルCRUD・画像アップロード（マジックバイト検証）・IDOR チェック
- `CatalogService`: マスター問題CRUD・カタログからのインポート
- `EventAdminService`: イベント取得・ランキング・開始/設定変更・削除
- `GameService`: LINE Bot のゲーム進行ロジック（参加・回答判定・ヒント・リセット）

---

## セキュリティ上の重要事項（実装時の必須チェック）

- `application.properties` は **絶対にコミットしない**（gitignore 済み）
- LINE Webhook `/callback` は CSRF 除外が必須（`SecurityConfig` で設定済み）
- リドルの編集・削除は `getRiddleOwnedBy(id, groupId)` で所有権確認が必須
- パスワードは `BCryptPasswordEncoder` でハッシュ化（平文保存禁止）
- 画像アップロードはマジックバイト検証 + サイズ上限チェックが必須
- 詳細: [SECURITY.md](SECURITY.md)

---

## 設定ファイルの扱い

| ファイル | 内容 | Git |
|:--|:--|:--|
| `application.properties` | 共通設定 + シークレットは `${ENV_VAR:デフォルト値}` 形式で環境変数注入 | **コミット済み** |
