# Changelog

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
