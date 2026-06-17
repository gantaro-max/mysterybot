# セキュリティポリシー — MysteryBot

## 脆弱性の報告

セキュリティ上の問題を発見した場合は、公開 Issue ではなく直接リポジトリオーナーへ連絡してください。

---

## 実装済みセキュリティ対策

セキュリティレビュー: 2026-06-17  
全項目対応完了: 2026-06-17

### SEC-1: 認証情報のGit漏洩対策
- [x] LINE チャンネルトークン・シークレットをローテーション
- [x] DB パスワードをローテーション
- [x] `application.properties` を gitignore 化し、`application.properties.example` を作成
- [x] Git 履歴から秘密情報を削除（`git filter-repo` 使用）

### SEC-2: 認証不要デバッグエンドポイントの削除
- [x] 全謎問題と回答を返す `TestController.java` を削除

### SEC-3: IDOR（他テナントの謎問題への不正アクセス）
- [x] `EventAdminService.getRiddleOwnedBy(id, groupId)` で所有者チェックを追加
- [x] 編集・更新・削除の全操作に所有権検証を適用

### SEC-4: パスワードの平文保存
- [x] `BCryptPasswordEncoder` でハッシュ化して保存
- [x] ログイン時に `BCryptPasswordEncoder.matches()` で検証
- [x] 既存ユーザーはログイン時に自動移行

### SEC-5: CSRF保護なし・セッション固定攻撃
- [x] Spring Security の `SecurityFilterChain` で CSRF 保護を有効化
- [x] `/callback`（LINE Webhook）のみ CSRF 除外
- [x] 全 Thymeleaf フォームを `th:action` に変更（CSRF トークン自動付与）
- [x] ログイン・新規登録成功時に `request.changeSessionId()` でセッション固定対策

### SEC-6: `/auth/register` 経由のスーパーAdmin権限奪取
- [x] 予約 groupId（"admin", "system", "root", "superadmin", "test"）を登録不可に設定

### SEC-7: ファイルアップロード経由の格納型XSS
- [x] マジックバイト検査（JPEG: `FF D8 FF` / PNG: `89 50 4E 47` / GIF: `47 49 46 38`）
- [x] `ImageController` で `Content-Type` を `MediaType.IMAGE_JPEG` に固定
- [x] ファイルサイズ上限（10MB）チェック
- [x] デコードボム攻撃対策（画像寸法検証）

### SEC-8: 管理者なりすまし後の権限消失
- [x] なりすまし前に `originalAdminId` セッション属性を保存
- [x] `POST /admin/end-impersonate` でアドミン画面へ正常復帰
- [x] なりすまし中はダッシュボードに警告バナーを表示

---

## 進捗サマリー

| ID | タイトル | 優先度 | ステータス |
|:--|:--|:--|:--|
| SEC-1 | 認証情報のGit漏洩 | Critical | 対応済み |
| SEC-2 | 認証不要デバッグエンドポイント | Critical | 対応済み |
| SEC-3 | IDOR（謎問題の所有者チェック） | Critical | 対応済み |
| SEC-4 | パスワード平文保存 | High | 対応済み |
| SEC-5 | CSRF保護なし・セッション固定 | High | 対応済み |
| SEC-6 | Admin権限奪取（register経由） | High | 対応済み |
| SEC-7 | ファイルアップロードXSS | Medium | 対応済み |
| SEC-8 | 管理者なりすまし設計バグ | Medium | 対応済み |
