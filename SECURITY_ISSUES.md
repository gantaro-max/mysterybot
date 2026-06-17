# セキュリティ対応 チェックリスト

セキュリティレビュー日: 2026-06-17
全項目対応完了: 2026-06-17

## 凡例
- [ ] 未対応
- [x] 対応済み

---

## 優先度1: 今すぐ（Critical）

### SEC-1: 認証情報のGitリポジトリへのコミット
- [x] LINEチャンネルトークン・シークレットをローテーション（LINE Developersコンソール）
- [x] DBパスワードをローテーション（TiDB Serverless）
- [x] `application.properties` から実際の値を削除し、`application.properties.example` を作成
- [x] `.gitignore` に `application.properties` を追加
- [x] Git履歴から秘密情報を削除（`git filter-repo` を使用）

---

### SEC-2: 認証不要デバッグエンドポイントが全謎問題（答え含む）を公開
- [x] `TestController.java` を削除

---

### SEC-3: IDOR — 他テナントの謎問題を読み書き・削除できる
- [x] `EventAdminService.getRiddleOwnedBy(id, groupId)` で所有者チェックを追加
- [x] `EventAdminService.updateRiddle()` に `groupId` パラメータと所有者チェックを追加
- [x] `EventAdminService.deleteRiddle(id, groupId)` に所有者チェックを追加
- [x] `UserController` の編集・削除エンドポイントで所有権検証を実施

---

## 優先度2: 早急（Critical / High）

### SEC-4: パスワードの平文保存
- [x] Spring Security の `BCryptPasswordEncoder` を依存関係に追加
- [x] 登録時にBCryptでハッシュ化して保存
- [x] ログイン時に `BCryptPasswordEncoder.matches()` で検証（平文からの段階移行ロジック付き）
- [x] 既存ユーザー（admin/STM/demo）のパスワードをBCryptに移行（TiDB上で直接UPDATE）

---

### SEC-5: CSRF保護なし / セッション固定攻撃
- [x] Spring Security の `SecurityFilterChain` でCSRF保護を有効化
- [x] `/callback`（LINE Webhook）のみCSRF除外
- [x] 全ThymeleafフォームのHTMLを `action=` から `th:action=` に変更（CSRFトークン自動付与）
- [x] ログイン・新規登録成功時に `request.changeSessionId()` でセッション固定攻撃を防止
- [x] ログアウトを `@GetMapping` → `@PostMapping` に変更、フォームでPOST送信

---

### SEC-6: /auth/register 経由のスーパーAdmin権限奪取
- [x] 登録時に予約 `groupId`（"admin", "system", "root", "superadmin", "test"）をブロック
- [x] `createEvent()` でバリデーション実施、`IllegalArgumentException` をスロー
- [x] `AuthController` で `IllegalArgumentException` と汎用例外を分けてキャッチ

---

## 優先度3: 計画的に対応

### SEC-7: 任意MIMEタイプでのファイルアップロード → 格納型XSS
- [x] マジックバイト検査（JPEG: `FF D8 FF` / PNG: `89 50 4E 47` / GIF: `47 49 46 38`）を実装
- [x] `ImageController` で `Content-Type` を `MediaType.IMAGE_JPEG` に固定
- [x] ファイルサイズ上限（10MB）チェックを追加
- [x] 画像デコードボム攻撃対策（`validateImageDimensions()` で寸法検証）

---

### SEC-8: 管理者なりすまし後に管理者権限が永久消失
- [x] なりすまし前に `originalAdminId` セッション属性を保存
- [x] `POST /admin/end-impersonate` エンドポイントを追加して管理者画面へ復帰
- [x] なりすまし中はダッシュボードに警告バナーを表示

---

## 進捗サマリー

| ID | タイトル | 優先度 | ステータス |
|:--|:--|:--|:--|
| SEC-1 | 認証情報のGit漏洩 | 今すぐ | 対応済み |
| SEC-2 | 認証不要デバッグエンドポイント | 今すぐ | 対応済み |
| SEC-3 | IDOR（謎問題の所有者チェック） | 今すぐ | 対応済み |
| SEC-4 | パスワード平文保存 | 早急 | 対応済み |
| SEC-5 | CSRF保護なし・セッション固定 | 早急 | 対応済み |
| SEC-6 | Admin権限奪取（register経由） | 早急 | 対応済み |
| SEC-7 | ファイルアップロードXSS | 計画的 | 対応済み |
| SEC-8 | 管理者なりすまし設計バグ | 計画的 | 対応済み |
