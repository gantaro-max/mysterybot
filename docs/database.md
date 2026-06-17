# データベース設計 — MysteryBot

ORM: MyBatis（XML マッパー方式）
DDL: `src/main/resources/schema.sql`

---

## テーブル一覧

| テーブル | 用途 |
|:--|:--|
| `team_groups` | イベント管理 |
| `riddles` | 謎問題（イベントに紐づく） |
| `players` | 参加者（LINE ユーザー × イベント） |
| `solved_histories` | 正解履歴 |
| `riddle_images` | 画像バイナリ（UUID で公開URL生成） |
| `master_riddles` | カタログ用共有マスタ問題 |

---

## team_groups（イベント管理）

| カラム | 型 | 説明 |
|:--|:--|:--|
| `group_id` | VARCHAR(PK) | イベントID（例: `wedding2024`） |
| `group_name` | VARCHAR | イベント名 |
| `admin_pass` | VARCHAR | BCrypt ハッシュ化パスワード |
| `is_random_order` | BOOLEAN | ランダム出題モードフラグ |
| `started_at` | TIMESTAMP | イベント開始日時（null = 準備中） |

---

## riddles（謎問題）

| カラム | 型 | 説明 |
|:--|:--|:--|
| `id` | INT(PK) | 自動採番 |
| `group_id` | VARCHAR(FK) | 所属イベント（team_groups 参照） |
| `stage_no` | INT | 出題順序 |
| `question` | TEXT | 問題文 |
| `answer` | VARCHAR | 正解（カンマ区切りで複数可） |
| `hint_msg` | VARCHAR | ヒントメッセージ |
| `next_msg` | TEXT | 正解時のメッセージ |
| `image_id` | INT(FK) | 画像ID（riddle_images 参照、null 可） |

---

## players（参加者）

| カラム | 型 | 説明 |
|:--|:--|:--|
| `id` | INT(PK) | 自動採番 |
| `line_user_id` | VARCHAR | LINE User ID |
| `group_id` | VARCHAR(FK) | 参加イベント |
| `current_stage` | INT | 現在の進行度 |
| `player_name` | VARCHAR | チーム名/個人名 |
| `start_at` | DATETIME | ゲーム開始時刻 |
| `finished_at` | DATETIME | 全問クリア時刻（null = 未クリア） |

ユニーク制約: `(line_user_id, group_id)`

---

## riddle_images（画像ストレージ）

| カラム | 型 | 説明 |
|:--|:--|:--|
| `id` | INT(PK) | 内部管理ID |
| `uuid` | VARCHAR(36) | **公開URL用ID**（`/public/image/{uuid}`） |
| `data` | LONGBLOB | 画像バイナリ（Thumbnailator でリサイズ済み） |
| `mime_type` | VARCHAR | MIME タイプ（`image/jpeg` 等） |

---

## master_riddles（カタログ）

| カラム | 型 | 説明 |
|:--|:--|:--|
| `id` | INT(PK) | マスタID |
| `category` | VARCHAR | カテゴリ（初級・結婚式等） |
| `question` | TEXT | 問題文 |
| `answer` | VARCHAR | 正解 |
| `next_msg` | TEXT | 正解メッセージ |
| `hint_msg` | VARCHAR | ヒント |
| `image_id` | INT(FK) | 画像ID（null 可） |

---

## solved_histories（正解履歴）

プレイヤーがどの問題を解いたかを記録するテーブル。
詳細は `src/main/resources/schema.sql` を参照。
