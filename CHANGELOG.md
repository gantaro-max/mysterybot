# Changelog

## [2.3.0] - 2026-06-17

### Changed

- **設定統一（Phase 1）**: `application.properties` / `application.yml` / `application.properties.example` の3ファイルを `application.properties` に統合。シークレットは `${ENV_VAR:default}` 形式で環境変数注入。`.gitignore` 除外を解除し、コミット可能な設定ファイルに変更。
- **サービス層分割（Phase 2）**: 330行の God Service（`EventAdminService`）を4サービスに分解。
  - `AuthService`: ログイン・BCrypt認証・イベント登録
  - `RiddleService`: リドルCRUD・画像アップロード・IDOR チェック
  - `CatalogService`: マスター問題CRUD・カタログインポート
  - `EventAdminService`: イベント取得・ランキング・開始/設定変更・削除に縮小
- **認証インターセプター（Phase 3）**: `AuthInterceptor`（`HandlerInterceptor`）を導入し、`/user/**` と `/admin/**` の認証・認可チェックを一元化。コントローラー11箇所の重複チェックコードを削除。
- **リファクタリング（Phase 4）**: `RiddleService.updateRiddle()` を `MultipartFile` 受け取りに変更し、所有権チェックと画像処理をサービス内で一括実施（二重チェック解消）。`MasterRiddleRequest` Java record DTO を導入し `CatalogService` の引数を整理。

### Added

- `LineBotAutoConfigurationEnvironmentPostProcessor`: LINE SDK の channel-secret 未設定時の起動エラーを回避する `EnvironmentPostProcessor` を追加。
- `MasterRiddleRequest` DTO（Java record）を `dto/` パッケージに追加。
- `AuthInterceptorTest`・`RiddleServiceTest`・`CatalogServiceTest` を追加。

---

## [2.2.0] - 2026-06-17

### Security
- **BCrypt認証導入**: パスワードを BCryptPasswordEncoder でハッシュ化して保存・検証するよう変更。平文パスワードの既存ユーザーはログイン時に自動移行。
- **セッション固定攻撃対策**: ログイン・新規登録成功時に `request.changeSessionId()` を実行。
- **CSRF保護**: Spring Security の `SecurityFilterChain` を導入。全フォームに `th:action` でCSRFトークンを自動付与。LINE Webhook (`/callback`) のみ除外。
- **ログアウトPOST化**: `GET /auth/logout` を `POST` に変更し、フォームからのみ実行可能に。
- **IDOR対策**: `getRiddleOwnedBy(id, groupId)` による所有権チェックをリドル編集・更新・削除の全操作に適用。
- **予約グループIDブロック**: `/auth/register` で "admin", "system" 等の予約IDを登録不可に。
- **ファイルアップロード検証**: マジックバイト検査（JPEG/PNG/GIF）・ファイルサイズ上限・デコードボム攻撃対策を追加。
- **Content-Type固定**: `ImageController` の画像配信を `MediaType.IMAGE_JPEG` 固定にし stored XSS を防止。
- **デバッグエンドポイント削除**: 未認証で全謎問題と回答を返す `TestController` を削除。
- **なりすまし安全化**: `originalAdminId` セッション属性で管理者IDを保持し、`/admin/end-impersonate` で正常復帰できるよう修正。なりすまし中は警告バナーを表示。
- **Git履歴クリーン**: `git filter-repo` で過去のコミットから認証情報を消去。

---

## [2.1.0] - 2026-01-13

### Added

- **カタログ機能**: 管理者が作成した「マスター問題」を、主催者が自分のイベントにインポートできる機能を追加。
- **UUID 対応**: 画像の公開 URL に推測不可能な UUID を採用し、セキュリティを強化 (`/public/image/{uuid}`)。
- **遊び方ガイド**: Bot に「遊び方」「ヘルプ」コマンドを追加。
- **強制リセット機能**: アプリ管理者が、進行中のイベントを「準備中」の状態に戻す機能を追加。
- **24 時間制限**: イベント開始から 24 時間経過後、自動的に参加・回答を締め切るロジックを追加。

### Changed

- **コントローラー構成の刷新**:
  - `/admin`: アプリ管理者（スーパー Admin）用
  - `/user`: イベント主催者用
  - `/auth`: 認証用
  - 上記のように URL 設計を完全に分離・整理。
- **データベース**: `riddle_images` テーブルに `uuid` カラムを追加。
- **リファクタリング**: `FlexMessageHelper` を DI コンポーネント化し、環境変数の読み込みを改善。

### Fixed

- 新規問題登録時に、画像とヒントが正しく保存されないバグを修正。

## [2.0.0] - 2026-01-09

### Added

- マルチテナント機能の実装（グループ ID によるイベント分割）。
- ランダム出題モードの実装。
- リアルタイムランキング機能の実装。

## [1.0.0] - Initial Release

- LINE Bot による基本的な謎解き機能（固定シナリオ）。
