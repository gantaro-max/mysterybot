# MysteryBot (謎解きイベントプラットフォーム)

LINE Botを活用した、周遊型・イベント型謎解きゲーム作成プラットフォームです。
1つのシステムで複数のイベント（テナント）を同時に稼働させることができる「マルチテナント方式」を採用しています。

## 🚀 主な機能

* **マルチテナント管理**: 企業やイベントごとに「グループID」を発行し、独立して管理可能。
* **Web管理画面**:
    * シナリオ（問題・正解・画像・ヒント）のCRUD操作
    * リアルタイムランキング表示
    * 「カタログ」機能による問題のインポート
* **LINE Bot**:
    * 回答の自動判定
    * 画像の表示（UUIDによるセキュアな配信）
    * ヒント機能、進捗保存
    * 遊び方ガイド表示

## 🛠 技術スタック

* **言語**: Java 21
* **フレームワーク**: Spring Boot
* **テンプレートエンジン**: Thymeleaf + Bootstrap 5
* **データベース**: MySQL 8.0 (TiDB Serverless 対応)
* **O/Rマッパー**: MyBatis
* **外部API**: LINE Messaging API

## ⚙️ 環境構築と起動

### 1. 前提条件
* JDK 21 がインストールされていること
* Docker (データベース起動用)

### 2. データベースの起動
付属の `docker-compose.yml` を使用してMySQLを起動します。

```bash
docker-compose up -d
```

* **ポート**: `3307` (デフォルト設定)
* **ユーザー/パスワード**: `gantaro` / `gan70668`
* **データベース名**: `mystery_game`

### 3. 設定ファイルの編集
`src/main/resources/application.properties` を環境に合わせて修正します。
特に **LINE Bot設定** と **アプリケーションのURL** は必須です。

```properties
# LINE Bot API設定
line.bot.channel-token=YOUR_CHANNEL_TOKEN
line.bot.channel-secret=YOUR_CHANNEL_SECRET
line.bot.friend-url=[https://lin.ee/xxxxxxx](https://lin.ee/xxxxxxx)

# 画像表示用URL (デプロイ先のURLを指定)
# ローカルで試す場合はngrok等のURL、本番ならRenderのURL
mysterybot.app-url=[https://your-app.onrender.com](https://your-app.onrender.com)
```

※ `FlexMessageHelper.java` 内の `APP_URL` 定数も合わせて確認してください。

### 4. アプリケーションの起動
Gradleを使用してアプリケーションを起動します。

```bash
./gradlew bootRun
```

## 📖 使い方

1.  **管理者ログイン**: ブラウザで `http://localhost:8080/auth/login` にアクセス。
2.  **イベント作成**: 「新規イベント作成」からID（例: `demo`）を登録。
3.  **シナリオ登録**: ログイン後、ダッシュボードから問題を追加。
4.  **プレイ開始**: LINE Botを友だち追加し、トーク画面で「開始 demo」と送信。

## 📂 ディレクトリ構成

* `src/main/java/com/gantaro/mysterybot/controller` - URLエンドポイント定義
* `src/main/java/com/gantaro/mysterybot/service` - ビジネスロジック
* `src/main/resources/mappers` - SQL定義 (MyBatis)
* `src/main/resources/templates` - HTML画面 (Thymeleaf)