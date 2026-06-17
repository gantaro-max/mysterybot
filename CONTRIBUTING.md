# 開発ガイドライン — MysteryBot

## セットアップ

```bash
# リポジトリのクローン
git clone git@github.com:gantaro-max/mysterybot.git
cd mysterybot

# ローカル DB 起動（Docker）
docker-compose up -d

# LINE Bot をローカルでテストする場合のみ環境変数を設定
export LINE_BOT_CHANNEL_TOKEN=your_token
export LINE_BOT_CHANNEL_SECRET=your_secret
export MYSTERYBOT_APP_URL=https://xxxx.ngrok-free.app

# アプリ起動
./gradlew bootRun
```

### LINE Bot のローカルテスト

LINE Webhook は HTTPS 必須なので ngrok が必要。

```bash
ngrok http 8080
# 出力された HTTPS URL を LINE Developers コンソールの Webhook URL に設定
# MYSTERYBOT_APP_URL にも同じ URL を設定
```

---

## コマンド

| コマンド | 説明 |
|:--|:--|
| `./gradlew bootRun` | アプリ起動 |
| `./gradlew build` | ビルド |
| `./gradlew test` | テスト実行 |
| `docker-compose up -d` | ローカル DB 起動 |

---

## コーディング規約

### コントローラー

- 認証チェックはコントローラーの冒頭で `session.getAttribute("loginGroupId")` を確認する
- リドル操作は必ず `EventAdminService.getRiddleOwnedBy(id, groupId)` で所有権を確認してからサービスを呼ぶ
- 新しいリドル操作エンドポイントを追加する場合も必ず所有権チェックを含める

### フォーム

- すべての POST フォームは `th:action` を使う（CSRF トークンが自動付与される）
- `action=` を直書きしてはいけない

### SQL

- SQL は `src/main/resources/mappers/` の XML に書く
- リポジトリインターフェースに対応するメソッドを定義する

### セキュリティ

- パスワードは `BCryptPasswordEncoder` でハッシュ化して保存する（平文保存禁止）
- ファイルアップロードはマジックバイト検証 + サイズ上限チェックを行う
- 詳細は [SECURITY.md](SECURITY.md) を参照

---

## ブランチ運用

- `main` ブランチへの push で Render が自動デプロイする
- 機能追加・バグ修正はフィーチャーブランチを切って PR でマージする

---

## 設定ファイルの扱い

| ファイル | 用途 | Git |
|:--|:--|:--|
| `application.properties` | 共通設定と環境変数デフォルト | コミット済み |

シークレットは `application.properties` に直接書かず、Render と同じキー名の環境変数で設定する。
詳細は [README.md](README.md) の「Render 環境変数」表を参照。

---

## AI エージェント向け情報

Codex / GitHub Copilot など AI エージェントが参照する追加情報は [AGENTS.md](AGENTS.md) を参照。
Claude Code 向けの詳細は [CLAUDE.md](CLAUDE.md) を参照。
