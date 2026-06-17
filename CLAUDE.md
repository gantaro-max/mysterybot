# CLAUDE.md — MysteryBot 開発ガイド

## プロジェクト概要

LINE Botを使ったマルチテナント対応の謎解きイベントプラットフォーム。
1つのサーバーで複数のイベント（テナント）を同時運用できる。

## 技術スタック

| 項目 | 内容 |
|:--|:--|
| 言語 | Java 21 |
| フレームワーク | Spring Boot 4.0.1 |
| ORM | MyBatis（XMLマッパー方式） |
| テンプレート | Thymeleaf + Bootstrap 5 |
| DB | MySQL 8.0 / TiDB Serverless（MySQL互換） |
| 外部API | LINE Messaging API（Flex Message） |
| ビルド | Gradle 9.x |

## 主要コマンド

```bash
# ローカルDB起動（Docker）
docker-compose up -d

# アプリ起動
./gradlew bootRun

# ビルド
./gradlew build

# テスト
./gradlew test
```

## 環境設定

`src/main/resources/application.properties` を編集する。
- `line.bot.channel-token` / `line.bot.channel-secret`: LINE Developers コンソールで取得
- `mysterybot.app-url`: アプリの公開URL（画像配信URLに使用）
  - ローカル: ngrokなどのURL
  - 本番: `https://mysterybot.onrender.com`
- ローカルDB: `localhost:3307`（docker-compose.ymlで定義）
- 本番DB: TiDB Serverless（`application.properties`の`spring.datasource.url`を変更）

## アーキテクチャ

### ロール別URLとコントローラー

| ロール | URLプレフィックス | コントローラー |
|:--|:--|:--|
| 認証 | `/auth` | `AuthController` |
| スーパーAdmin | `/admin` | `AdminController` |
| イベント主催者 | `/user` | `UserController` |
| LINE Bot Webhook | `/callback` | `LineWebhookController` |
| 画像配信 | `/public` | `ImageController` |

### 認証方式
- セッションベース（`HttpSession`に`loginGroupId`を格納）
- `groupId == "admin"` の場合はスーパーAdmin権限

### サービス層
- `EventAdminService`: Web管理画面のビジネスロジック全般（ログイン、イベント・謎のCRUD、カタログ、画像アップロード）
- `GameService`: LINE Botのゲーム進行ロジック（参加、回答判定、ヒント、リセット）

### LINEメッセージ
- `FlexMessageHelper`: Flex Messageカードを生成するユーティリティ
  - `createQuestionMessage()`: 青いカード（出題時）
  - `createCorrectMessage()`: 緑のカード（正解時）

## ディレクトリ構成

```
src/main/java/com/gantaro/mysterybot/
├── controller/
│   ├── AdminController.java        # スーパーAdmin用
│   ├── AuthController.java         # ログイン・新規登録
│   ├── ImageController.java        # 画像配信（認証不要）
│   ├── LineWebhookController.java  # LINE Webhook
│   ├── TestController.java         # 開発用テスト
│   └── UserController.java         # イベント主催者用
├── service/
│   ├── EventAdminService.java      # Web管理ロジック
│   └── GameService.java            # ゲーム進行ロジック
├── entity/                         # DBエンティティ
│   ├── TeamGroup.java              # イベント
│   ├── Player.java                 # 参加者
│   ├── Riddle.java                 # 謎問題
│   ├── RiddleImage.java            # 画像（BLOBストレージ）
│   ├── MasterRiddle.java           # カタログ用マスタ問題
│   └── SolvedHistory.java          # 回答履歴
├── repository/                     # MyBatisリポジトリ
├── dto/
│   └── GameResult.java             # ゲーム処理結果DTO
└── util/
    └── FlexMessageHelper.java      # LINEメッセージ生成

src/main/resources/
├── application.properties          # 設定ファイル（シークレット含む）
├── schema.sql                      # DDL（ローカル初期化用）
├── mappers/                        # MyBatis XMLマッパー
└── templates/                      # Thymeleaf HTMLテンプレート
    ├── admin/                      # スーパーAdmin画面
    ├── auth/                       # ログイン・登録画面
    └── user/                       # イベント主催者画面
```

## データベーススキーマ

| テーブル | 用途 |
|:--|:--|
| `team_groups` | イベント管理（group_id, started_at等） |
| `riddles` | 謎問題（group_idに紐づく） |
| `players` | 参加者（line_user_id + group_idで一意） |
| `solved_histories` | どのプレイヤーが何問解いたかの履歴 |
| `riddle_images` | 画像バイナリ（LONGBLOB）、UUIDで公開URL生成 |
| `master_riddles` | カタログ用の共有マスタ問題 |

## ゲームフロー（LINE Bot）

1. プレイヤーが「開始 [groupId]」を送信 → `joinGame()`
2. チーム名入力 → `processAnswer()` が名前として登録し第1問を送信
3. 回答送信 → `processAnswer()` が正誤判定、正解なら次問
4. 全問正解 → `finished_at` に記録、ランキングに反映
5. 「ヒント」 → `getHint()` でヒント返信
6. 「リセット」 → `resetGame()` でプレイヤーデータ削除

**制限時間**: `team_groups.started_at` から24時間で自動締め切り

## 画像配信の仕組み

- 画像はDB（`riddle_images.data`）にLONGBLOBで保存
- アップロード時にUUIDを生成し、公開URLは `/public/image/{uuid}`
- 認証不要エンドポイント（LINE BotからアクセスできるようにするためBasic認証なし）
- アップロード時にThumbnailatorで800px幅・JPEG80%品質にリサイズ

## デプロイ

- **ソース**: GitHub → Render（pushで自動デプロイ）
- **DB**: TiDB Serverless（`application.properties`の接続先を変更）
- `schema.sql`は本番では手動実行（`spring.sql.init.mode`はコメントアウト済み）

## ローカルLINE Bot開発

LINE WebhookはHTTPS必須なのでngrokが必要：

```bash
ngrok http 8080
# 出力されたURLをLINE Developersコンソールのwebhook URLに設定
# application.propertiesのmysterybot.app-urlも同じURLに変更
```
