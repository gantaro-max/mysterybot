# 実装指示書: フェーズ1 — 設定ファイル管理の統一

## 背景・目的

現在、設定が3ファイルに分散していてメンテナンスが困難。

| ファイル | 現状 |
|:--|:--|
| `application.properties` | gitignore済み。シークレット＋非シークレットが混在 |
| `application.yml` | コミット済み。非シークレット設定のみ（応急処置的に追加） |
| `application.properties.example` | コミット済み。上2つと内容が重複 |

**目標**: `application.properties` 1本に統一し、シークレットは `${ENV_VAR:ローカルデフォルト}` 形式で外部注入できるようにする。gitignore も不要にする。

---

## 実装対象ファイル

- `.gitignore` — `application.properties` の除外行を削除
- `src/main/resources/application.properties` — 内容を全面書き換え
- `src/main/resources/application.yml` — **削除**
- `src/main/resources/application.properties.example` — **削除**
- `README.md` — セットアップ手順を更新

---

## 実装仕様

### 1. `.gitignore` の修正

以下の行を削除する。

```
src/main/resources/application.properties
```

### 2. `application.properties` の全面書き換え

以下の内容に置き換える。シークレットはすべて `${ENV_VAR:デフォルト値}` 形式にする。

```properties
spring.application.name=mysterybot

# データソース（本番はRenderの環境変数で上書き）
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3307/mystery_game}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:gantaro}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:gan70668}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# MyBatis
mybatis.configuration.map-underscore-to-camel-case=true
mybatis.mapper-locations=classpath:mappers/*.xml

# LINE Bot（本番はRenderの環境変数で上書き）
line.bot.channel-token=${LINE_BOT_CHANNEL_TOKEN:}
line.bot.channel-secret=${LINE_BOT_CHANNEL_SECRET:}
line.bot.handler.path=/callback
line.bot.friend-url=${LINE_BOT_FRIEND_URL:https://lin.ee/o5eoNJi}

# ファイルアップロード上限
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# アプリURL（本番はRenderの環境変数で上書き）
mysterybot.app-url=${MYSTERYBOT_APP_URL:http://localhost:8080}

# DB初期化（必要なときだけコメントアウト解除）
# spring.sql.init.mode=always
```

### 3. `application.yml` を削除

ファイルを削除する。設定内容はすべて上記 `application.properties` に含まれる。

### 4. `application.properties.example` を削除

`application.properties` 自体がテンプレートを兼ねるため不要。

### 5. `README.md` の「ローカル環境のセットアップ」セクションを更新

以下の変更を行う。

- 「設定ファイルを作成」セクションから `cp` コマンドとコピー手順を削除する
- 代わりに以下の説明に置き換える：

```markdown
**2. 設定の確認**

`src/main/resources/application.properties` がリポジトリに含まれています。
ローカルのDockerDB（ポート3307）はデフォルト設定で動作します。

LINE Bot をローカルでテストする場合のみ、環境変数を設定してください：

```bash
export LINE_BOT_CHANNEL_TOKEN=your_token
export LINE_BOT_CHANNEL_SECRET=your_secret
export MYSTERYBOT_APP_URL=https://xxxx.ngrok-free.app
```
```

---

## 制約・注意事項

- **ローカルDockerDBのパスワード `gan70668` はデフォルト値として残してよい**。本番のTiDBパスワードはRender環境変数で上書きされるため、ファイルに含まれるデフォルト値は本番に影響しない。
- **LINE トークンのデフォルトは空文字のまま**にする。ローカルでBot機能を使わない場合は空でも起動する。
- `application.yml` と `application.properties` が両方存在するとSpring Bootは両方読み込み、設定が競合する可能性がある。**必ず `application.yml` を先に削除**してから `application.properties` を書き換えること。
- `application.properties.example` への参照が `CONTRIBUTING.md`・`AGENTS.md`・`README.md` に残っている場合は合わせて修正すること。

---

## 完了条件

- [ ] `application.yml` が存在しない
- [ ] `application.properties.example` が存在しない
- [ ] `application.properties` がgitignoreされていない（`git status` で追跡対象になっている）
- [ ] `application.properties` のシークレット項目がすべて `${ENV_VAR:...}` 形式になっている
- [ ] `./gradlew bootRun` でローカル起動できる（DBはDockerが起動している前提）
- [ ] `./gradlew build` が成功する
- [ ] `README.md` のセットアップ手順が新しい構成に合っている
