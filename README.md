# MysteryBot — 謎解きイベントプラットフォーム

LINE Bot を活用した、周遊型・イベント型謎解きゲーム作成プラットフォームです。
1つのシステムで複数のイベント（テナント）を同時に稼働させる「マルチテナント方式」を採用しています。

---

## 主な機能

- **マルチテナント管理**: イベントごとに「グループID」を発行し、独立して管理
- **Web管理画面**: 問題・正解・画像・ヒントのCRUD操作（レスポンシブ対応）
- **カタログ機能**: 管理者が用意したマスター問題をワンクリックでインポート
- **LINE Bot**: 回答自動判定・シナリオ進行・ヒント・遊び方ガイド
- **リアルタイムランキング**: クリアタイム順位をスクリーン表示
- **制限時間**: イベント開始から24時間で自動締め切り

---

## 技術スタック

| 項目 | 内容 |
|:--|:--|
| 言語 | Java 21 |
| フレームワーク | Spring Boot 4.0.1 / Spring Security 7.x |
| ORM | MyBatis（XMLマッパー方式） |
| テンプレート | Thymeleaf + Bootstrap 5 |
| DB（ローカル） | MySQL 8.0（Docker） |
| DB（本番） | TiDB Serverless（MySQL互換） |
| 外部API | LINE Messaging API（Flex Message） |
| ビルド | Gradle 9.x |
| ホスティング | Render（自動デプロイ） |

---

## ローカル環境のセットアップ

### 前提条件
- JDK 21
- Docker（ローカルDB起動用）
- LINE Developersアカウント（Messaging API）
- ngrok（ローカルでBotをテストする場合）

### 手順

**1. リポジトリをクローン**
```bash
git clone https://github.com/gantaro-max/mysterybot.git
cd mysterybot
```

**2. 設定の確認**

`src/main/resources/application.properties` がリポジトリに含まれています。
ローカルのDockerDB（ポート3307）はデフォルト設定で動作します。

LINE Bot をローカルでテストする場合のみ、環境変数を設定してください：

```bash
export LINE_BOT_CHANNEL_TOKEN=your_token
export LINE_BOT_CHANNEL_SECRET=your_secret
export MYSTERYBOT_APP_URL=https://xxxx.ngrok-free.app
```

**3. ローカルDBを起動**
```bash
docker-compose up -d
```
- ポート: `3307`
- DBが初期化されない場合は `application.properties` の `spring.sql.init.mode=always` を一時的に有効化

**4. アプリを起動**
```bash
./gradlew bootRun
```

**5. LINE Bot用にngrokを設定（Botテスト時のみ）**
```bash
ngrok http 8080
# 出力されたHTTPS URLをLINE DevelopersコンソールのWebhook URLに設定
# MYSTERYBOT_APP_URLにも同じURLを設定
```

---

## 本番デプロイ（Render）

GitHub の `main` ブランチへの push で Render が自動デプロイします。

### Render 環境変数（必須）

| Key | 説明 |
|:--|:--|
| `LINE_BOT_CHANNEL_TOKEN` | LINE Developersで取得 |
| `LINE_BOT_CHANNEL_SECRET` | LINE Developersで取得 |
| `LINE_BOT_FRIEND_URL` | LINE公式アカウントの友だち追加URL |
| `MYSTERYBOT_APP_URL` | `https://mysterybot.onrender.com` |
| `SPRING_DATASOURCE_URL` | TiDB Serverlessの接続URL |
| `SPRING_DATASOURCE_USERNAME` | TiDBユーザー名 |
| `SPRING_DATASOURCE_PASSWORD` | TiDBパスワード |

---

## ディレクトリ構成

```
src/main/java/com/gantaro/mysterybot/
├── config/
│   └── SecurityConfig.java         # Spring Security設定（CSRF・BCrypt）
├── controller/
│   ├── AdminController.java        # スーパーAdmin用 (/admin)
│   ├── AuthController.java         # ログイン・登録・ログアウト (/auth)
│   ├── ImageController.java        # 画像配信・認証不要 (/public)
│   ├── LineWebhookController.java  # LINE Webhook (/callback)
│   └── UserController.java         # イベント主催者用 (/user)
├── service/
│   ├── EventAdminService.java      # Web管理ロジック（BCrypt・IDOR対策含む）
│   └── GameService.java            # ゲーム進行ロジック
├── entity/                         # DBエンティティ
├── repository/                     # MyBatisリポジトリ
├── dto/
│   └── GameResult.java
└── util/
    └── FlexMessageHelper.java      # LINEメッセージ生成

src/main/resources/
├── application.properties          # 設定（環境変数で本番値を上書き）
├── schema.sql                      # DDL
├── mappers/                        # MyBatis XMLマッパー
└── templates/                      # Thymeleaf HTMLテンプレート
    ├── admin/                      # スーパーAdmin画面
    ├── auth/                       # ログイン・登録画面
    └── user/                       # イベント主催者画面
```

---

## ドキュメント

| ファイル | 内容 |
|:--|:--|
| [docs/requirements.md](docs/requirements.md) | 要件定義 |
| [docs/architecture.md](docs/architecture.md) | アーキテクチャ・クラス設計・ゲームフロー |
| [docs/database.md](docs/database.md) | テーブル設計 |
| [docs/api.md](docs/api.md) | エンドポイント設計・Bot コマンド一覧 |
| [docs/operator-guide.md](docs/operator-guide.md) | イベント主催者向けマニュアル |
| [CONTRIBUTING.md](CONTRIBUTING.md) | 開発者向けガイドライン・環境構築 |
| [SECURITY.md](SECURITY.md) | セキュリティポリシー・対応チェックリスト |
| [CHANGELOG.md](CHANGELOG.md) | バージョン履歴 |
| [AGENTS.md](AGENTS.md) | AI エージェント向けガイド |
