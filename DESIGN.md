# 謎解きイベントプラットフォーム 設計書 (MysteryBot)

## 1. 概要 (Overview)
LINE Botを活用した、周遊型・イベント型謎解きゲーム作成プラットフォーム。
「グループID（テナント）」を分けることで、1つのシステムで複数の企業やイベント（結婚式余興、社内レクリエーションなど）を同時に稼働させることを可能とする。

## 2. 要件定義 (Requirements)

### 2.1 ターゲットユーザー
1.  **管理者 (Game Master):** 謎解きイベントを主催したい人。問題を作成・管理する。
2.  **プレイヤー (Player):** LINEを使って謎解きに参加する一般ユーザー。

### 2.2 機能要件
#### 【管理者機能】 (REST API / 管理画面)
* **謎の登録・編集・削除 (CRUD):**
    * 問題文、正解キーワード、ヒント、順序（第何問目か）を設定できる。
    * グループIDを指定して登録する。
* **進捗確認:**
    * どのプレイヤーが今どこまで進んでいるかを確認できる。

#### 【プレイヤー機能】 (LINE Bot)
* **ゲーム開始:**
    * QRコード等を読み込み、特定の「グループID」に紐づくゲームを開始する。
* **回答送信:**
    * LINEのトーク画面で答えを入力する。
* **正誤判定 (自動):**
    * Botが答えを照合し、「正解！次の問題へ」または「不正解」を即座に返信する。
* **進捗保存:**
    * 途中離脱しても、続きから再開できる。

### 2.3 非機能要件
* **レスポンス速度:** LINEの返信は3秒以内に行う（Lambda/Cloud Function等のコールドスタート対策含む）。
* **同時接続:** 1イベントあたり最大100名程度の同時アクセスに耐える設計。

---

## 3. 基本設計 (Basic Design)

### 3.1 アーキテクチャ
* **Backend:** Java 21, Spring Boot 4.01
* **Database:** MySQL 8.0 (Docker)
* **ORM:** MyBatis
* **Interface:** LINE Messaging API (Webhook)

### 3.2 処理フロー (シーケンス概要)
1.  **User** -> (メッセージ送信) -> **LINE Platform**
2.  **LINE Platform** -> (Webhook POST) -> **Spring Boot (Controller)**
3.  **Controller** -> **Service** (メッセージ解析・正誤判定ロジック)
4.  **Service** -> **Mapper** (DB問い合わせ: 正解取得・進捗更新)
5.  **Service** -> **LINE SDK** (返信メッセージ生成)
6.  **Spring Boot** -> (API Call) -> **LINE Platform** -> **User**

---

## 4. テーブル設計 (Schema Design)

マルチテナント（グループ分け）を実現するためのDB構造。

### ER図 (イメージ)
`team_groups` --(1:N)--> `riddles`
`team_groups` --(1:N)--> `players`
`players` --(1:N)--> `progress`

### 4.1 team_groups (イベント・グループ管理)
イベントごとの設定を持つ親テーブル。

| Column Name | Type | Key | Description |
| :--- | :--- | :--- | :--- |
| `group_id` | VARCHAR(50) | PK | グループ識別子 (例: "company_a", "wedding_2024") |
| `group_name` | VARCHAR(100)| | イベント名 |
| `admin_pass` | VARCHAR(255)| | 簡易認証用パスワード |
| `created_at` | DATETIME | | 作成日時 |

### 4.2 riddles (謎・問題マスタ)
問題の中身。`group_id` と `stage_no` で一意になる。

| Column Name | Type | Key | Description |
| :--- | :--- | :--- | :--- |
| `id` | INT | PK | 自動採番ID |
| `group_id` | VARCHAR(50) | FK | どのグループの問題か |
| `stage_no` | INT | | 第何問目か (1, 2, 3...) |
| `question` | TEXT | | 問題文 (画像URL等も可) |
| `answer` | VARCHAR(255)| | 正解キーワード (完全一致/正規表現) |
| `next_msg` | TEXT | | 正解時のメッセージ (次のストーリー) |

### 4.3 players (プレイヤー情報)
LINEユーザーと、現在参加しているグループの紐付け。

| Column Name | Type | Key | Description |
| :--- | :--- | :--- | :--- |
| `id` | INT | PK | 自動採番ID |
| `line_user_id`| VARCHAR(255)| | LINEの固有ID (Uxxxxxxxx...) |
| `group_id` | VARCHAR(50) | FK | 現在参加中のイベント |
| `current_stage`| INT | | 現在挑戦中のステージ番号 (初期値: 1) |
| `last_active` | DATETIME | | 最終アクセス日時 |

### 4.4 progress (回答履歴・ログ)
誰がいつ、どの問題をクリアしたか。

| Column Name | Type | Key | Description |
| :--- | :--- | :--- | :--- |
| `id` | INT | PK | 自動採番ID |
| `player_id` | INT | FK | プレイヤーID |
| `riddle_id` | INT | FK | 解いた問題ID |
| `is_cleared` | BOOLEAN | | クリアフラグ |
| `cleared_at` | DATETIME | | クリア日時 |

---

## 5. 詳細設計 (API Endpoints)

開発フェーズ1で作るべき主要API。

### 5.1 管理者用 API (REST)

| Method | Path | Description |
| :--- | :--- | :--- |
| **POST** | `/api/admin/riddles` | 新しい謎を登録する |
| **GET** | `/api/admin/riddles/{groupId}` | そのグループの謎一覧を取得 |
| **PUT** | `/api/admin/riddles/{id}` | 謎の内容を修正する |
| **DELETE** | `/api/admin/riddles/{id}` | 謎を削除する |

### 5.2 LINE Webhook

| Method | Path | Description |
| :--- | :--- | :--- |
| **POST** | `/api/callback` | LINEからのイベント受信 (全ロジックの入り口) |

---

## 6. クラス設計 (Java/Spring Boot)

### Package Structure
```text
com.gantaro.mysterybot
├── controller
│   ├── AdminController.java (管理者API: 謎やグループの登録)
│   └── LineWebhookController.java (LINE受付: ゲーム進行)
├── service
│   ├── GameService.java (正誤判定、ステージ進行ロジック)
│   └── PlayerService.java (ユーザー登録、状態管理)
├── repository (MyBatis)
│   ├── GroupRepository.java
│   ├── RiddleRepository.java
│   ├── PlayerRepository.java
│   └── ProgressRepository.java
└── entity (Data Model)
    ├── Group.java
    ├── Riddle.java
    ├── Player.java
    └── Progress.java