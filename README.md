# MysteryBot (謎解きイベントプラットフォーム)

LINE Botを活用した、周遊型・イベント型謎解きゲーム作成プラットフォームです。
1つのシステムで複数のイベント（テナント）を同時に稼働させることができる「マルチテナント方式」を採用しています。

## 🚀 主な機能

- **マルチテナント管理**: 企業やイベントごとに「グループID」を発行し、独立して管理可能。
- **Web管理画面**:
  - シナリオ（問題・正解・画像・ヒント）のCRUD操作
  - **レスポンシブ対応**: スマホからでも快適に操作可能
  - **カタログ機能**: 管理者が用意した「マスター問題」をワンクリックでインポート
  - リアルタイムランキング表示
- **LINE Bot**:
  - 回答の自動判定とシナリオ進行
  - 画像のセキュア配信（UUIDベースのURL）
  - ヒント機能、進捗の自動保存
  - **遊び方ガイド**: 「遊び方」「ヘルプ」コマンド対応
  - **制限時間機能**: イベント開始から24時間で自動締め切り

## 🌍 インフラ・デプロイ構成

このアプリケーションは、以下のモダンな構成での運用を想定しています。

- **ソースコード管理**: GitHub
- **アプリケーションサーバー**: Render (Web Service)
  - GitHubへのプッシュをトリガーに自動デプロイ
- **データベース**: TiDB Serverless (MySQL互換)
  - スケーラブルでサーバーレスなクラウドデータベースを採用

## 🛠 技術スタック

- **言語**: Java 21
- **フレームワーク**: Spring Boot 3.x (4.0.1)
- **テンプレートエンジン**: Thymeleaf + Bootstrap 5
- **データベース**: MySQL 8.0 / TiDB
- **O/Rマッパー**: MyBatis
- **外部API**: LINE Messaging API

## ⚙️ 環境構築と起動

### 1. 前提条件

- JDK 21 がインストールされていること
- Docker (ローカルDB起動用)
- LINE Developers アカウント（Messaging API）

### 2. データベースの起動 (ローカル開発)

付属の `docker-compose.yml` を使用してMySQLを起動します。

```bash
docker-compose up -d
```

- **ポート**: `3307`
- **ユーザー/パスワード**: `gantaro` / `YOUR_DB_PASSWORD`
- **データベース名**: `mystery_game`

### 3. 設定ファイルの編集

`src/main/resources/application.properties` を環境に合わせて修正します。

```properties
# LINE Bot API設定
line.bot.channel-token=YOUR_CHANNEL_TOKEN
line.bot.channel-secret=YOUR_CHANNEL_SECRET
line.bot.friend-url=[https://lin.ee/xxxxxxx](https://lin.ee/xxxxxxx)

# アプリケーションURL (重要)
# ※ Botが画像を送信する際に使用します
# ローカル開発時: ngrok等のURL (例: [https://xxxx.ngrok-free.app](https://xxxx.ngrok-free.app))
# 本番環境: Render等のURL (例: [https://mysterybot.onrender.com](https://mysterybot.onrender.com))
mysterybot.app-url=[https://your-app.onrender.com](https://your-app.onrender.com)
```

### 4. アプリケーションの起動

Gradleを使用してアプリケーションを起動します。

```bash
./gradlew bootRun
```

## 📖 使い方

1.  **管理者ログイン**: ブラウザで `/auth/login` にアクセス。
    - 初期アカウント作成は `/auth/register` から。
2.  **イベント作成**: ID（例: `demo`）とパスワードを設定してイベントを作成。
3.  **シナリオ登録**: ダッシュボードから「シナリオ編集」へ移動し、問題を追加。
    - 「カタログから選ぶ」を使えば、既成の良質な問題をすぐに導入可能。
4.  **本番開始**: ダッシュボードの「イベントを本番開始する」ボタンを押下（24時間のカウントダウン開始）。
5.  **プレイ開始**: 参加者はQRコードからBotを友だち追加し、「開始 [イベントID]」と送信。

## 📂 ディレクトリ構成

- `src/main/java/com/gantaro/mysterybot/controller` - URLエンドポイント (Admin/User/Auth/Bot)
- `src/main/java/com/gantaro/mysterybot/service` - ビジネスロジック (Game/Admin)
- `src/main/java/com/gantaro/mysterybot/util` - LINEメッセージ生成ヘルパー
- `src/main/resources/mappers` - SQL定義 (MyBatis XML)
- `src/main/resources/templates` - HTML画面 (Thymeleaf)
