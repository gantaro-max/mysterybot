# セキュリティ対応 チェックリスト

セキュリティレビュー日: 2026-06-17

## 凡例
- [ ] 未対応
- [x] 対応済み

---

## 優先度1: 今すぐ（Critical）

### SEC-1: 認証情報のGitリポジトリへのコミット
- [ ] LINEチャンネルトークン・シークレットをローテーション（LINE Developersコンソール）
- [ ] DBパスワードをローテーション（TiDB Serverless）
- [ ] `application.properties` から実際の値を削除し、`application.properties.example` を作成
- [ ] `.gitignore` に `application.properties` を追加
- [ ] Git履歴から秘密情報を削除（`git filter-repo` または BFG Repo-Cleaner を使用）

**対象ファイル:** `src/main/resources/application.properties`
**リスク:** DB直接アクセス、LINEユーザーへの任意メッセージ送信、Webhookなりすまし

---

### SEC-2: 認証不要デバッグエンドポイントが全謎問題（答え含む）を公開
- [ ] `TestController.java` を本番ビルドから削除、または `@Profile("local")` でローカル限定化

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/controller/TestController.java`
**リスク:** 誰でも任意イベントの全問題と答えを取得可能

---

### SEC-3: IDOR — 他テナントの謎問題を読み書き・削除できる
- [ ] `EventAdminService.getRiddle(id)` に所有者チェックを追加
- [ ] `EventAdminService.updateRiddle()` に所有者チェックを追加
- [ ] `EventAdminService.deleteRiddle(id)` に所有者チェックを追加
- [ ] `UserController` の編集・削除エンドポイントで `groupId` 一致を検証

**対象ファイル:**
- `src/main/java/com/gantaro/mysterybot/service/EventAdminService.java`
- `src/main/java/com/gantaro/mysterybot/controller/UserController.java`

**リスク:** テナント間のデータ読み取り・改ざん・削除

---

## 優先度2: 早急（Critical / High）

### SEC-4: パスワードの平文保存・平文表示
- [ ] Spring Security の `BCryptPasswordEncoder` を依存関係に追加
- [ ] 登録時にBCryptでハッシュ化して保存
- [ ] ログイン時に `BCryptPasswordEncoder.matches()` で検証
- [ ] 管理者ダッシュボードから `adminPass` の表示を削除
- [ ] 既存ユーザーのパスワードを再ハッシュ化するマイグレーションスクリプトを作成

**対象ファイル:**
- `src/main/java/com/gantaro/mysterybot/service/EventAdminService.java`
- `src/main/resources/templates/admin/dashboard.html`

**リスク:** DB漏洩時に全パスワードが即座に露出

---

### SEC-5: CSRF保護なし（全POSTエンドポイント）
- [ ] Spring Security を依存関係に追加
- [ ] CSRF保護を有効化（Spring Security デフォルトで有効）
- [ ] 全Thymeleafフォームに `th:action` を使用（自動でCSRFトークン埋め込み）
- [ ] セッション固定攻撃対策を有効化（Spring Security デフォルトで有効）

**対象ファイル:** 全コントローラー、全テンプレート
**リスク:** ログイン中ユーザーの意図しない操作を外部サイトから実行可能

---

### SEC-6: /auth/register 経由のスーパーAdmin権限奪取
- [ ] 登録時に `"admin"` などの予約 `groupId` をブロック
- [ ] `groupId` のバリデーション追加（英数字・ハイフンのみ、長さ制限）
- [ ] 管理者アカウントの初回セットアップをアプリ起動時に自動作成する仕組みに変更

**対象ファイル:**
- `src/main/java/com/gantaro/mysterybot/controller/AuthController.java`
- `src/main/java/com/gantaro/mysterybot/service/EventAdminService.java`

**リスク:** DBに admin 行がない状態で誰でもスーパーAdmin権限を取得可能

---

## 優先度3: 計画的に対応

### SEC-7: 任意MIMEタイプでのファイルアップロード → 格納型XSS
- [ ] ファイルのマジックバイト検査を実装（Tika または Apache Commons Imaging）
- [ ] `ImageController` で `Content-Type` を許可リスト（`image/jpeg`, `image/png`, `image/gif`）から設定
- [ ] アップロード済みの非画像ファイルをDBから削除

**対象ファイル:**
- `src/main/java/com/gantaro/mysterybot/service/EventAdminService.java`
- `src/main/java/com/gantaro/mysterybot/controller/ImageController.java`

**リスク:** HTMLファイルをアプリのオリジンで配信してJavaScriptを実行可能

---

### SEC-8: 管理者なりすまし後に管理者権限が永久消失
- [ ] なりすまし用に `originalAdminId` セッション属性を別途保持
- [ ] なりすまし解除エンドポイントを追加

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/controller/AdminController.java`
**リスク:** CSRFと組み合わせると管理者を一般ユーザーに強制格下げ可能

---

## 進捗サマリー

| ID | タイトル | 優先度 | ステータス |
|:--|:--|:--|:--|
| SEC-1 | 認証情報のGit漏洩 | 今すぐ | 未対応 |
| SEC-2 | 認証不要デバッグエンドポイント | 今すぐ | 未対応 |
| SEC-3 | IDOR（謎問題の所有者チェック） | 今すぐ | 未対応 |
| SEC-4 | パスワード平文保存 | 早急 | 未対応 |
| SEC-5 | CSRF保護なし | 早急 | 未対応 |
| SEC-6 | Admin権限奪取（register経由） | 早急 | 未対応 |
| SEC-7 | ファイルアップロードXSS | 計画的 | 未対応 |
| SEC-8 | 管理者なりすまし設計バグ | 計画的 | 未対応 |
