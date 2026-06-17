# MysteryBot セキュリティ修正 実装指示書

> Codex向け実装仕様書。上から順に実装すること。
> 各タスクは独立したコミットとして分けることを推奨。

---

## 前提知識

- **言語/FW:** Java 21 / Spring Boot 4.0.1 / Spring Security 7.x (Jakarta EE 11ベース)
- **ORM:** MyBatis（XMLマッパー方式）
- **テンプレート:** Thymeleaf
- **認証方式:** セッションベース。`HttpSession` の `loginGroupId` 属性でユーザーを識別。
  - `"admin"` ならスーパーAdmin、それ以外ならイベント主催者
- **重要制約:** `/callback` エンドポイントはLINE外部サーバーからのPOSTを受け取るため、CSRFトークン検証を除外すること

---

## タスク一覧

| ID | 内容 | 優先度 |
|:--|:--|:--|
| [TASK-1](#task-1-secrets) | 秘密情報をリポジトリから除外 | 今すぐ |
| [TASK-2](#task-2-testcontroller) | デバッグエンドポイント削除 | 今すぐ |
| [TASK-3](#task-3-idor) | IDOR修正（謎問題の所有者検証） | 今すぐ |
| [TASK-4](#task-4-password) | パスワードBCryptハッシュ化 | 早急 |
| [TASK-5](#task-5-csrf) | Spring Security追加とCSRF保護 | 早急 |
| [TASK-6](#task-6-groupid) | groupId予約語バリデーション | 早急 |
| [TASK-7](#task-7-upload) | ファイルアップロードのMIME検証 | 計画的 |
| [TASK-8](#task-8-impersonate) | 管理者なりすまし設計修正 | 計画的 |

---

## TASK-1: 秘密情報をリポジトリから除外 {#task-1-secrets}

### 1-A: `.gitignore` に `application.properties` を追加

**対象ファイル:** `.gitignore`（プロジェクトルートに存在する場合は追記、なければ新規作成）

末尾に以下を追加：
```
src/main/resources/application.properties
```

### 1-B: `application.properties.example` を新規作成

**新規作成ファイル:** `src/main/resources/application.properties.example`

内容（実際の値はすべてプレースホルダーに置き換える）：
```properties
spring.application.name=mysterybot
spring.datasource.url=jdbc:mysql://localhost:3307/mystery_game
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
mybatis.configuration.map-underscore-to-camel-case=true
mybatis.mapper-locations=classpath:mappers/*.xml
line.bot.channel-token=YOUR_LINE_CHANNEL_TOKEN
line.bot.channel-secret=YOUR_LINE_CHANNEL_SECRET
line.bot.handler.path=/callback
line.bot.friend-url=https://lin.ee/YOUR_FRIEND_LINK
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
mysterybot.app-url=https://your-app-url.example.com
# spring.sql.init.mode=always
```

### 1-C: 現在の `application.properties` から秘密情報を削除

**対象ファイル:** `src/main/resources/application.properties`

`spring.datasource.password`、`line.bot.channel-token`、`line.bot.channel-secret` の値を
プレースホルダーに置き換える（1-Bと同じ形式）。

> **注意:** Gitの履歴から既存の秘密情報を削除するには `git filter-repo` の実行が別途必要。
> それは開発者が手動で行うこと（このタスクのスコープ外）。

---

## TASK-2: デバッグエンドポイント削除 {#task-2-testcontroller}

### 削除するファイル

```
src/main/java/com/gantaro/mysterybot/controller/TestController.java
```

このファイルを**完全に削除**する。他のファイルへの参照はないため、削除のみでOK。

---

## TASK-3: IDOR修正（謎問題の所有者検証） {#task-3-idor}

**問題:** `EventAdminService.getRiddle(id)` はIDのみで謎問題を取得するため、
他テナントのIDを指定すれば誰でも参照・更新・削除できる。

### 3-A: `EventAdminService.java` に所有者チェックメソッドを追加

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/service/EventAdminService.java`

既存の `getRiddle(Integer id)` メソッド（74行目付近）を以下の2メソッドに置き換える：

```java
// 既存メソッドはそのまま残す（GameServiceなど内部からの呼び出し用）
public Riddle getRiddle(Integer id) {
    return riddleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("謎が見つかりません:ID" + id));
}

// 所有者チェック付きの新メソッドを追加（Controllerから呼ぶ）
public Riddle getRiddleOwnedBy(Integer id, String groupId) {
    Riddle riddle = getRiddle(id);
    if (!groupId.equals(riddle.getGroupId())) {
        throw new SecurityException("この謎問題へのアクセス権がありません");
    }
    return riddle;
}
```

既存の `updateRiddle` メソッド（98行目付近）の先頭に所有者チェックを追加：

```java
@Transactional
public void updateRiddle(Integer id, String groupId, String question, String answer,
        String nextMsg, String hintMsg, Integer imageId) {
    // 所有者チェック（変更点: groupIdパラメータを追加して検証）
    Riddle resultRiddle = getRiddleOwnedBy(id, groupId);
    resultRiddle.setQuestion(question);
    resultRiddle.setAnswer(answer);
    resultRiddle.setNextMsg(nextMsg);
    resultRiddle.setHintMsg(hintMsg);
    if (imageId != null) {
        resultRiddle.setImageId(imageId);
    }
    riddleRepository.update(resultRiddle);
}
```

既存の `deleteRiddle` メソッド（117行目付近）に所有者チェックを追加：

```java
@Transactional
public void deleteRiddle(Integer id, String groupId) {
    // 所有者チェック（変更点: groupIdパラメータを追加して検証）
    getRiddleOwnedBy(id, groupId);
    solvedHistoryRepository.deleteByRiddleId(id);
    riddleRepository.delete(id);
}
```

### 3-B: `UserController.java` の呼び出し箇所を修正

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/controller/UserController.java`

**編集対象1: `editRiddle` メソッド（97行目付近）**

```java
// 変更前
@GetMapping("/riddles/edit/{id}")
public String editRiddle(@PathVariable Integer id, Model model) {
    if (getLoginGroupId() == null)
        return "redirect:/auth/login";
    Riddle riddle = eventAdminService.getRiddle(id);
    model.addAttribute("riddle", riddle);
    return "user/riddle_edit";
}

// 変更後
@GetMapping("/riddles/edit/{id}")
public String editRiddle(@PathVariable Integer id, Model model) {
    String groupId = getLoginGroupId();
    if (groupId == null)
        return "redirect:/auth/login";
    try {
        Riddle riddle = eventAdminService.getRiddleOwnedBy(id, groupId);
        model.addAttribute("riddle", riddle);
        return "user/riddle_edit";
    } catch (SecurityException e) {
        return "redirect:/user/riddles";
    }
}
```

**編集対象2: `updateRiddle` メソッド（106行目付近）**

```java
// 変更前
@PostMapping("/riddles/update")
public String updateRiddle(@RequestParam Integer id, @RequestParam String question,
        @RequestParam String answer, @RequestParam String nextMsg,
        @RequestParam(required = false) String hintMsg,
        @RequestParam(required = false) MultipartFile imageFile) throws IOException {

    if (getLoginGroupId() == null)
        return "redirect:/auth/login";

    Riddle oldRiddle = eventAdminService.getRiddle(id);
    Integer imageId = oldRiddle.getImageId();

    if (imageFile != null && !imageFile.isEmpty()) {
        imageId = eventAdminService.uploadImage(imageFile);
    }

    eventAdminService.updateRiddle(id, question, answer, nextMsg, hintMsg, imageId);

    return "redirect:/user/riddles";
}

// 変更後
@PostMapping("/riddles/update")
public String updateRiddle(@RequestParam Integer id, @RequestParam String question,
        @RequestParam String answer, @RequestParam String nextMsg,
        @RequestParam(required = false) String hintMsg,
        @RequestParam(required = false) MultipartFile imageFile) throws IOException {

    String groupId = getLoginGroupId();
    if (groupId == null)
        return "redirect:/auth/login";

    try {
        Riddle oldRiddle = eventAdminService.getRiddleOwnedBy(id, groupId);
        Integer imageId = oldRiddle.getImageId();

        if (imageFile != null && !imageFile.isEmpty()) {
            imageId = eventAdminService.uploadImage(imageFile);
        }

        eventAdminService.updateRiddle(id, groupId, question, answer, nextMsg, hintMsg, imageId);
    } catch (SecurityException e) {
        return "redirect:/user/riddles";
    }

    return "redirect:/user/riddles";
}
```

**編集対象3: `deleteRiddle` メソッド（131行目付近）**

```java
// 変更前
@PostMapping("/riddles/delete/{id}")
public String deleteRiddle(@PathVariable Integer id) {
    if (getLoginGroupId() == null)
        return "redirect:/auth/login";
    eventAdminService.deleteRiddle(id);
    return "redirect:/user/riddles";
}

// 変更後
@PostMapping("/riddles/delete/{id}")
public String deleteRiddle(@PathVariable Integer id) {
    String groupId = getLoginGroupId();
    if (groupId == null)
        return "redirect:/auth/login";
    try {
        eventAdminService.deleteRiddle(id, groupId);
    } catch (SecurityException e) {
        return "redirect:/user/riddles";
    }
    return "redirect:/user/riddles";
}
```

---

## TASK-4: パスワードBCryptハッシュ化 {#task-4-password}

### 4-A: `build.gradle` に Spring Security を追加

**対象ファイル:** `build.gradle`

`dependencies` ブロックに以下を追加：

```groovy
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'
```

> Spring Boot 4.0はSpring Security 7.xを使用するが、Thymeleafのエクストラは
> `springsecurity6` を指定する（ライブラリのバージョン番号であり、Security 7にも対応している）。

### 4-B: `SecurityConfig.java` を新規作成

**新規作成ファイル:** `src/main/java/com/gantaro/mysterybot/config/SecurityConfig.java`

```java
package com.gantaro.mysterybot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 認証はコントローラーのセッションチェックで行うため、Spring Securityの認証は無効化
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            // すべてのリクエストを許可（認証チェックは各コントローラーで実施）
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            // CSRF: /callback（LINE Webhook）のみ除外
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/callback")
            )
            // セッション固定攻撃対策（ログイン時に新しいセッションIDを発行）
            .sessionManagement(session -> session
                .sessionFixation().newSession()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 4-C: `EventAdminService.java` でBCryptを使用

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/service/EventAdminService.java`

**インポート追加：**
```java
import org.springframework.security.crypto.password.PasswordEncoder;
```

**フィールド追加（`@RequiredArgsConstructor` で自動注入）：**
```java
private final PasswordEncoder passwordEncoder;
```

**`login` メソッド（39行目付近）を修正：**
```java
// 変更前
public boolean login(String groupId, String password) {
    Optional<TeamGroup> group = teamGroupRepository.findByGroupId(groupId);
    if (group.isEmpty())
        return false;
    String savedPass = group.get().getAdminPass();
    return savedPass != null && savedPass.equals(password);
}

// 変更後
public boolean login(String groupId, String password) {
    Optional<TeamGroup> group = teamGroupRepository.findByGroupId(groupId);
    if (group.isEmpty())
        return false;
    String savedPass = group.get().getAdminPass();
    if (savedPass == null) return false;
    return passwordEncoder.matches(password, savedPass);
}
```

**`createEvent` メソッド（47行目付近）を修正：**
```java
// 変更前
newGroup.setAdminPass(password);

// 変更後
newGroup.setAdminPass(passwordEncoder.encode(password));
```

### 4-D: 管理者ダッシュボードからパスワード表示を削除

**対象ファイル:** `src/main/resources/templates/admin/dashboard.html`

**変更前（49行目付近）：**
```html
<th>パスワード</th>
```
↓ この `<th>` を削除

**変更前（61行目付近）：**
```html
<td>
    <span class="badge bg-secondary font-monospace" th:text="${event.adminPass}"></span>
</td>
```
↓ この `<td>` ブロック全体を削除

### 4-E: 既存パスワードのマイグレーション

既存のDBには平文パスワードが保存されている。
以下のSQLをローカルDBおよび本番TiDBで手動実行すること（Codexのスコープ外・開発者が実行）：

```sql
-- 既存の全パスワードをBCryptハッシュに更新する
-- 事前にJavaでBCrypt.encode(現在のパスワード)を実行してハッシュ値を生成し、
-- 各イベントのIDごとに UPDATE team_groups SET admin_pass = '[ハッシュ値]' WHERE group_id = '[ID]'; を実行すること。
-- または全ユーザーにパスワード再設定を案内する。
```

---

## TASK-5: CSRF保護の有効化（フォームへのCSRFトークン埋め込み） {#task-5-csrf}

TASK-4でSpring Securityを追加すると、Thymeleafの `th:action` を使用したフォームには
自動的にCSRFトークンが埋め込まれる。

**全HTMLテンプレートのフォームを `th:action` に変更する。**

### 変更対象ファイルと変更内容

#### `src/main/resources/templates/auth/register.html`（17行目）
```html
<!-- 変更前 -->
<form action="/auth/register" method="post">

<!-- 変更後 -->
<form th:action="@{/auth/register}" method="post">
```

#### `src/main/resources/templates/auth/login.html`
ログインフォームの `action` 属性を `th:action="@{/auth/login}"` に変更。

#### `src/main/resources/templates/user/riddles.html`
すべての `<form action="...">` を `<form th:action="@{...}">` に変更。

#### `src/main/resources/templates/user/riddle_edit.html`
同様にすべての `action` を `th:action` に変更。

#### `src/main/resources/templates/user/dashboard.html`
同様にすべての `action` を `th:action` に変更。

#### `src/main/resources/templates/user/catalog.html`
同様にすべての `action` を `th:action` に変更。

#### `src/main/resources/templates/admin/dashboard.html`（71, 80, 92行目）
すでに `th:action` を使用しているため変更不要（確認のみ）。

#### `src/main/resources/templates/admin/master_riddle_edit.html`
同様にすべての `action` を `th:action` に変更。

---

## TASK-6: groupId予約語バリデーション {#task-6-groupid}

### 6-A: `EventAdminService.createEvent` に予約語チェックを追加

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/service/EventAdminService.java`

`createEvent` メソッドの先頭（重複チェックの前）に追加：

```java
private static final java.util.Set<String> RESERVED_GROUP_IDS =
    java.util.Set.of("admin", "system", "root", "superadmin", "test");

@Transactional
public void createEvent(String groupId, String groupName, String password) {
    // 予約語チェック
    if (RESERVED_GROUP_IDS.contains(groupId.toLowerCase())) {
        throw new IllegalArgumentException("そのイベントIDは使用できません");
    }

    // フォーマットチェック（半角英数字・ハイフン・アンダーバーのみ、3〜30文字）
    if (!groupId.matches("^[a-zA-Z0-9_-]{3,30}$")) {
        throw new IllegalArgumentException("イベントIDは半角英数字・ハイフン・アンダーバーのみ、3〜30文字で入力してください");
    }

    // 重複チェック（既存）
    if (teamGroupRepository.findByGroupId(groupId).isPresent()) {
        throw new IllegalArgumentException("そのイベントIDは既に使用されています");
    }

    TeamGroup newGroup = new TeamGroup();
    newGroup.setGroupId(groupId);
    newGroup.setGroupName(groupName);
    newGroup.setAdminPass(passwordEncoder.encode(password));
    newGroup.setIsRandomOrder(false);
    teamGroupRepository.insert(newGroup);
}
```

### 6-B: エラーメッセージを `AuthController` に反映

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/controller/AuthController.java`

`register` メソッドのcatchブロックのエラーメッセージを修正：

```java
// 変更前
} catch (Exception e) {
    model.addAttribute("error", "登録失敗: IDが重複している可能性があります");
    return "auth/register";
}

// 変更後
} catch (IllegalArgumentException e) {
    model.addAttribute("error", e.getMessage());
    return "auth/register";
} catch (Exception e) {
    model.addAttribute("error", "登録に失敗しました。もう一度お試しください");
    return "auth/register";
}
```

---

## TASK-7: ファイルアップロードのMIME検証 {#task-7-upload}

### 7-A: `EventAdminService.uploadImage` でマジックバイト検査を追加

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/service/EventAdminService.java`

`uploadImage` メソッドの先頭にマジックバイト検査を追加：

```java
@Transactional
public Integer uploadImage(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty())
        return null;

    byte[] originalData = file.getBytes();

    // マジックバイトで実際のファイル形式を検証
    if (!isAllowedImageBytes(originalData)) {
        throw new IllegalArgumentException("画像ファイル（JPEG/PNG/GIF）のみアップロードできます");
    }

    // Content-Typeヘッダーは信頼せず、マジックバイトから判定した形式を使う
    byte[] savedData;
    try (ByteArrayInputStream bis = new ByteArrayInputStream(originalData);
            ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Thumbnails.of(bis).width(800).outputFormat("jpg")
                .outputQuality(0.8).toOutputStream(bos);
        savedData = bos.toByteArray();
    } catch (Exception e) {
        savedData = originalData;
    }

    RiddleImage img = new RiddleImage();
    img.setData(savedData);
    img.setMimeType("image/jpeg"); // 常にjpegとして保存（攻撃者のContent-Typeを使わない）
    img.setUuid(UUID.randomUUID().toString());
    riddleImageRepository.insert(img);

    return img.getId();
}

// マジックバイト検査ヘルパー
private boolean isAllowedImageBytes(byte[] data) {
    if (data.length < 4) return false;

    // JPEG: FF D8 FF
    if (data[0] == (byte)0xFF && data[1] == (byte)0xD8 && data[2] == (byte)0xFF)
        return true;

    // PNG: 89 50 4E 47
    if (data[0] == (byte)0x89 && data[1] == (byte)0x50
            && data[2] == (byte)0x4E && data[3] == (byte)0x47)
        return true;

    // GIF: 47 49 46 38
    if (data[0] == (byte)0x47 && data[1] == (byte)0x49
            && data[2] == (byte)0x46 && data[3] == (byte)0x38)
        return true;

    return false;
}
```

### 7-B: `ImageController` でContent-Typeを固定

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/controller/ImageController.java`

画像を返すエンドポイントで、DBに保存されたMIMEタイプをそのまま使わず、
`image/jpeg` に固定（または `image/png`, `image/gif` を許可リストから選択）：

現在の実装を確認し、`ResponseEntity` を返すメソッドで：
```java
// 変更前（DBのmimeTypeをそのまま使用している箇所）
.contentType(MediaType.parseMediaType(image.getMimeType()))

// 変更後（常にJPEGとして配信）
.contentType(MediaType.IMAGE_JPEG)
```

---

## TASK-8: 管理者なりすまし設計修正 {#task-8-impersonate}

### 8-A: `AdminController` でなりすまし前のadminセッションを保持

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/controller/AdminController.java`

**`impersonate` メソッドを修正（46行目付近）：**

```java
// 変更前
@PostMapping("/impersonate/{groupId}")
public String impersonate(@PathVariable String groupId) {
    if (!"admin".equals(getLoginGroupId())) {
        return "redirect:/auth/login";
    }
    session.setAttribute("loginGroupId", groupId);
    return "redirect:/user/dashboard";
}

// 変更後
@PostMapping("/impersonate/{groupId}")
public String impersonate(@PathVariable String groupId) {
    if (!"admin".equals(getLoginGroupId())) {
        return "redirect:/auth/login";
    }
    // なりすまし前の admin セッションを別キーで保持
    session.setAttribute("originalAdminId", "admin");
    session.setAttribute("loginGroupId", groupId);
    return "redirect:/user/dashboard";
}
```

**なりすまし解除エンドポイントを追加（`AdminController` 末尾に追記）：**

```java
// S8. なりすまし解除
@PostMapping("/end-impersonate")
public String endImpersonate() {
    String originalId = (String) session.getAttribute("originalAdminId");
    if (originalId == null) {
        return "redirect:/auth/login";
    }
    session.removeAttribute("originalAdminId");
    session.setAttribute("loginGroupId", originalId);
    return "redirect:/admin/dashboard";
}
```

### 8-B: `UserController.dashboard` になりすまし解除ボタンを表示

**対象ファイル:** `src/main/resources/templates/user/dashboard.html`

`UserController.dashboard` メソッドで `originalAdminId` をモデルに追加するため、
コントローラーも修正：

**`UserController.java` の `dashboard` メソッドを修正：**
```java
@GetMapping("/dashboard")
public String dashboard(Model model, HttpSession session) {
    String groupId = getLoginGroupId();
    if (groupId == null)
        return "redirect:/auth/login";

    TeamGroup group = eventAdminService.getEvent(groupId);
    model.addAttribute("group", group);
    model.addAttribute("botFriendUrl", botFriendUrl);

    // なりすまし中かどうかをテンプレートに伝える
    boolean isImpersonating = session.getAttribute("originalAdminId") != null;
    model.addAttribute("isImpersonating", isImpersonating);

    return "user/dashboard";
}
```

> Note: `HttpSession session` はコンストラクタインジェクション済みのフィールドを使うか、
> メソッド引数で受け取るかは既存コードに合わせること。

**`user/dashboard.html` のナビバー付近に以下を追加：**
```html
<!-- なりすまし中の場合のみ表示 -->
<div th:if="${isImpersonating}" class="alert alert-warning text-center mb-0 py-2" role="alert">
    <strong>なりすましモード中</strong>
    <form th:action="@{/admin/end-impersonate}" method="post" class="d-inline ms-3">
        <button type="submit" class="btn btn-warning btn-sm">管理者画面に戻る</button>
    </form>
</div>
```

---

## 実装後の確認事項

- [ ] `./gradlew build` がエラーなく通ること
- [ ] `./gradlew bootRun` でアプリが起動すること
- [ ] 以下の動作確認：
  - [ ] 新規イベント登録 → ログイン → ダッシュボード表示
  - [ ] `groupId=admin` での登録が弾かれること
  - [ ] 別テナントの謎問題IDでアクセスすると `/user/riddles` にリダイレクトされること
  - [ ] `/test/riddles/demo` が404またはエラーになること
  - [ ] 画像以外のファイルアップロードが拒否されること
  - [ ] CSRFトークンなしのPOSTが拒否されること（`/callback` は除く）
  - [ ] 管理者なりすまし後に「管理者画面に戻る」ボタンで戻れること
