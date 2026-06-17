# API エンドポイント設計 — MysteryBot

認証方式: セッションベース（`HttpSession` の `loginGroupId` で判断）

---

## 認証 (`AuthController`)

| メソッド | パス | 説明 | 認証 |
|:--|:--|:--|:--|
| GET | `/auth/login` | ログイン画面 | 不要 |
| POST | `/auth/login` | ログイン処理 | 不要 |
| GET | `/auth/register` | 新規登録画面 | 不要 |
| POST | `/auth/register` | 新規登録処理 | 不要 |
| POST | `/auth/logout` | ログアウト（POST のみ） | 必要 |

---

## アプリ管理者 (`AdminController`)

すべて `groupId == "admin"` のセッションが必要。

| メソッド | パス | 説明 |
|:--|:--|:--|
| GET | `/admin` | 全イベント一覧 |
| POST | `/admin/impersonate/{groupId}` | 指定イベントへなりすましログイン |
| POST | `/admin/end-impersonate` | なりすまし終了・管理者画面へ復帰 |
| POST | `/admin/reset-time/{groupId}` | イベント開始時間の強制リセット |
| GET | `/admin/catalog` | カタログ（マスタ問題）一覧 |
| POST | `/admin/catalog/add-master` | マスタ問題の登録 |
| POST | `/admin/catalog/delete/{id}` | マスタ問題の削除 |

---

## イベント主催者 (`UserController`)

`loginGroupId` がセッションに存在することが条件。

| メソッド | パス | 説明 |
|:--|:--|:--|
| GET | `/user/dashboard` | ダッシュボード |
| POST | `/user/start-event` | イベント本番開始（24h カウントダウン開始） |
| GET | `/user/riddles` | シナリオ（謎）一覧 |
| POST | `/user/riddles/add` | 謎の新規登録（画像アップロード含む） |
| GET | `/user/riddles/edit/{id}` | 謎の編集画面 |
| POST | `/user/riddles/update/{id}` | 謎の更新（所有権チェックあり） |
| POST | `/user/riddles/delete/{id}` | 謎の削除（所有権チェックあり） |
| GET | `/user/catalog` | カタログ一覧（インポート元） |
| POST | `/user/catalog/import` | カタログから自イベントへコピー |
| GET | `/user/ranking` | リアルタイムランキング |
| POST | `/user/settings` | 出題設定（ランダムモード切替） |

---

## LINE Webhook (`LineWebhookController`)

| メソッド | パス | 説明 | CSRF |
|:--|:--|:--|:--|
| POST | `/callback` | LINE Webhook 受信 | 除外（LINE サーバーからのリクエスト） |

### Bot コマンド一覧（プレイヤー向け）

| メッセージ | 動作 |
|:--|:--|
| `開始 {groupId}` | ゲーム参加（新規または再開） |
| チーム名（参加直後） | チーム名登録、第1問送信 |
| 回答文字列 | 正誤判定、正解なら次問送信 |
| `ヒント` | 現在の問題のヒントを返信 |
| `遊び方` / `ヘルプ` | 操作ガイドを返信 |
| `リセット` | 自分のプレイデータを削除 |

---

## 画像配信 (`ImageController`)

| メソッド | パス | 説明 | 認証 |
|:--|:--|:--|:--|
| GET | `/public/image/{uuid}` | 画像バイナリを返す | 不要 |

- UUID は推測不可能なランダム値
- Content-Type は `image/jpeg` 固定（stored XSS 対策）
- LINE Bot 側から直接アクセスされるため認証不要
