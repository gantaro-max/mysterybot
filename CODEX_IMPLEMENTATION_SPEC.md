# MysteryBot セキュリティ修正 実装指示書 v2

> Codex向け実装仕様書。
> **【重要】`git filter-repo` によってワーキングツリーがリセットされ、前回実装した変更がすべて失われた。本書は現在のファイル状態を前提に再実装するための指示書である。**
> 上から順に実装し、各タスクを独立したコミットとして分けること。

---

## 前提知識

- **言語/FW:** Java 21 / Spring Boot 4.0.1 / Spring Security 7.x (Jakarta EE 11ベース)
- **ORM:** MyBatis（XMLマッパー方式）
- **テンプレート:** Thymeleaf
- **認証方式:** セッションベース。`HttpSession` の `loginGroupId` 属性でユーザーを識別。
  - `"admin"` ならスーパーAdmin、それ以外ならイベント主催者
- **重要制約:** `/callback` エンドポイントはLINE外部サーバーからのPOSTを受け取るため、CSRFトークン検証を除外すること
- **`build.gradle` は対応済み:** `spring-boot-starter-security` と `thymeleaf-extras-springsecurity6` は追加済み
- **`SecurityConfig.java` は作成済み:** `src/main/java/com/gantaro/mysterybot/config/SecurityConfig.java` に存在する

---

## タスク一覧

| ID | 内容 | 状態 |
|:--|:--|:--|
| [TASK-1](#task-1) | TestController削除 | 未対応 |
| [TASK-2](#task-2) | EventAdminService: BCrypt・IDOR・バリデーション | 未対応 |
| [TASK-3](#task-3) | AuthController: セッション固定対策・ログアウトPOST化 | 未対応 |
| [TASK-4](#task-4) | AdminController: なりすまし安全化・復帰エンドポイント追加 | 未対応 |
| [TASK-5](#task-5) | UserController: IDOR対策・なりすまし表示 | 未対応 |
| [TASK-6](#task-6) | ImageController: Content-Type固定 | 未対応 |
| [TASK-7](#task-7) | HTMLテンプレート: th:action・ログアウトPOST化 | 未対応 |

---

## TASK-1: TestController削除 {#task-1}

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/controller/TestController.java`

認証不要で全謎問題と正解を返す危険なエンドポイント。**ファイルごと削除する。**

```
# 削除コマンド
git rm src/main/java/com/gantaro/mysterybot/controller/TestController.java
```

---

## TASK-2: EventAdminService 修正 {#task-2}

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/service/EventAdminService.java`

### 2-A: インポートと依存関係追加

既存のimport群に以下を追加：

```java
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
```

クラスフィールドに `PasswordEncoder` を追加（`@RequiredArgsConstructor` で自動インジェクション）：

```java
// 既存フィールドの末尾に追加
private final PasswordEncoder passwordEncoder;
```

### 2-B: 予約語定数を追加

クラスフィールドの直後に追加：

```java
private static final Set<String> RESERVED_GROUP_IDS =
        Set.of("admin", "system", "root", "superadmin", "test");
```

### 2-C: `login()` メソッドをBCrypt対応に変更

**変更前:**
```java
public boolean login(String groupId, String password) {
    Optional<TeamGroup> group = teamGroupRepository.findByGroupId(groupId);
    if (group.isEmpty())
        return false;
    String savedPass = group.get().getAdminPass();
    return savedPass != null && savedPass.equals(password);
}
```

**変更後:**
```java
public boolean login(String groupId, String password) {
    Optional<TeamGroup> group = teamGroupRepository.findByGroupId(groupId);
    if (group.isEmpty())
        return false;
    String savedPass = group.get().getAdminPass();
    if (savedPass == null)
        return false;
    return passwordEncoder.matches(password, savedPass);
}
```

### 2-D: `createEvent()` にバリデーションとBCryptを追加

**変更前:**
```java
@Transactional
public void createEvent(String groupId, String groupName, String password) {
    if (teamGroupRepository.findByGroupId(groupId).isPresent()) {
        throw new IllegalArgumentException("そのイベントIDは既に使用されています");
    }
    TeamGroup newGroup = new TeamGroup();
    newGroup.setGroupId(groupId);
    newGroup.setGroupName(groupName);
    newGroup.setAdminPass(password);
    newGroup.setIsRandomOrder(false);
    teamGroupRepository.insert(newGroup);
}
```

**変更後:**
```java
@Transactional
public void createEvent(String groupId, String groupName, String password) {
    if (RESERVED_GROUP_IDS.contains(groupId.toLowerCase())) {
        throw new IllegalArgumentException("そのイベントIDは使用できません");
    }
    if (!groupId.matches("^[a-zA-Z0-9_-]{3,30}$")) {
        throw new IllegalArgumentException("イベントIDは半角英数字・ハイフン・アンダーバーのみ、3〜30文字で入力してください");
    }
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

### 2-E: `getRiddleOwnedBy()` メソッドを追加

`getRiddle()` メソッドの直後に追加：

```java
public Riddle getRiddleOwnedBy(Integer id, String groupId) {
    Riddle riddle = getRiddle(id);
    if (!groupId.equals(riddle.getGroupId())) {
        throw new SecurityException("この謎問題へのアクセス権がありません");
    }
    return riddle;
}
```

### 2-F: `updateRiddle()` シグネチャに `groupId` を追加して所有者チェック

**変更前:**
```java
@Transactional
public void updateRiddle(Integer id, String question, String answer, String nextMsg,
        String hintMsg, Integer imageId) {
    Riddle resultRiddle = getRiddle(id);
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

**変更後:**
```java
@Transactional
public void updateRiddle(Integer id, String groupId, String question, String answer, String nextMsg,
        String hintMsg, Integer imageId) {
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

### 2-G: `deleteRiddle()` シグネチャに `groupId` を追加して所有者チェック

**変更前:**
```java
@Transactional
public void deleteRiddle(Integer id) {
    solvedHistoryRepository.deleteByRiddleId(id);
    riddleRepository.delete(id);
}
```

**変更後:**
```java
@Transactional
public void deleteRiddle(Integer id, String groupId) {
    getRiddleOwnedBy(id, groupId);
    solvedHistoryRepository.deleteByRiddleId(id);
    riddleRepository.delete(id);
}
```

### 2-H: `uploadImage()` にマジックバイト検証を追加

**変更前:**
```java
@Transactional
public Integer uploadImage(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty())
        return null;

    byte[] originalData = file.getBytes();

    byte[] savedData;
    try (ByteArrayInputStream bis = new ByteArrayInputStream(originalData);
            ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Thumbnails.of(bis).width(800).outputFormat("jpg")
                .outputQuality(0.8).toOutputStream(bos);
        savedData = bos.toByteArray();
    } catch (Exception e) {
        throw new IOException("画像ファイルの処理に失敗しました");
    }

    RiddleImage img = new RiddleImage();
    img.setData(savedData);
    img.setMimeType(file.getContentType());
    img.setUuid(UUID.randomUUID().toString());
    riddleImageRepository.insert(img);

    return img.getId();
}
```

**変更後:**
```java
@Transactional
public Integer uploadImage(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty())
        return null;

    byte[] originalData = file.getBytes();
    if (!isAllowedImageBytes(originalData)) {
        throw new IllegalArgumentException("画像ファイル（JPEG/PNG/GIF）のみアップロードできます");
    }

    byte[] savedData;
    try (ByteArrayInputStream bis = new ByteArrayInputStream(originalData);
            ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Thumbnails.of(bis).width(800).outputFormat("jpg")
                .outputQuality(0.8).toOutputStream(bos);
        savedData = bos.toByteArray();
    } catch (Exception e) {
        throw new IllegalArgumentException("画像ファイルを処理できませんでした");
    }

    RiddleImage img = new RiddleImage();
    img.setData(savedData);
    img.setMimeType("image/jpeg");
    img.setUuid(UUID.randomUUID().toString());
    riddleImageRepository.insert(img);

    return img.getId();
}
```

### 2-I: `isAllowedImageBytes()` ヘルパーメソッドを追加

クラス末尾（`}` の直前）に追加：

```java
private boolean isAllowedImageBytes(byte[] data) {
    if (data.length < 4)
        return false;
    // JPEG: FF D8 FF
    if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF)
        return true;
    // PNG: 89 50 4E 47
    if (data[0] == (byte) 0x89 && data[1] == (byte) 0x50
            && data[2] == (byte) 0x4E && data[3] == (byte) 0x47)
        return true;
    // GIF: 47 49 46 38
    return data[0] == (byte) 0x47 && data[1] == (byte) 0x49
            && data[2] == (byte) 0x46 && data[3] == (byte) 0x38;
}
```

---

## TASK-3: AuthController 修正 {#task-3}

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/controller/AuthController.java`

### 3-A: インポート追加

```java
import jakarta.servlet.http.HttpServletRequest;
```

### 3-B: `login()` にセッション固定対策を追加

**変更前:**
```java
@PostMapping("/login")
public String login(@RequestParam String groupId, @RequestParam String password, Model model) {
    if (eventAdminService.login(groupId, password)) {
        session.setAttribute("loginGroupId", groupId);
```

**変更後:**
```java
@PostMapping("/login")
public String login(@RequestParam String groupId, @RequestParam String password, Model model,
        HttpServletRequest request) {
    if (eventAdminService.login(groupId, password)) {
        request.changeSessionId();
        session.setAttribute("loginGroupId", groupId);
```

### 3-C: `logout()` を `@GetMapping` から `@PostMapping` に変更

**変更前:**
```java
@GetMapping("/logout")
public String logout() {
    session.invalidate();
    return "redirect:/auth/login";
}
```

**変更後:**
```java
@PostMapping("/logout")
public String logout() {
    session.invalidate();
    return "redirect:/auth/login";
}
```

### 3-D: `register()` にセッション固定対策とエラーハンドリング改善

**変更前:**
```java
@PostMapping("/register")
public String register(@RequestParam String groupId, @RequestParam String groupName,
        @RequestParam String password, Model model) {
    try {
        eventAdminService.createEvent(groupId, groupName, password);
        session.setAttribute("loginGroupId", groupId);
        return "redirect:/user/dashboard";
    } catch (Exception e) {
        model.addAttribute("error", "登録失敗: IDが重複している可能性があります");
        return "auth/register";
    }
}
```

**変更後:**
```java
@PostMapping("/register")
public String register(@RequestParam String groupId, @RequestParam String groupName,
        @RequestParam String password, Model model, HttpServletRequest request) {
    try {
        eventAdminService.createEvent(groupId, groupName, password);
        request.changeSessionId();
        session.setAttribute("loginGroupId", groupId);
        return "redirect:/user/dashboard";
    } catch (IllegalArgumentException e) {
        model.addAttribute("error", e.getMessage());
        return "auth/register";
    } catch (Exception e) {
        model.addAttribute("error", "登録に失敗しました。もう一度お試しください");
        return "auth/register";
    }
}
```

---

## TASK-4: AdminController 修正 {#task-4}

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/controller/AdminController.java`

### 4-A: `impersonate()` を安全化

**変更前:**
```java
@PostMapping("/impersonate/{groupId}")
public String impersonate(@PathVariable String groupId) {
    if (!"admin".equals(getLoginGroupId())) {
        return "redirect:/auth/login";
    }

    session.setAttribute("loginGroupId", groupId);

    return "redirect:/user/dashboard";
}
```

**変更後:**
```java
@PostMapping("/impersonate/{groupId}")
public String impersonate(@PathVariable String groupId) {
    if (!"admin".equals(getLoginGroupId())) {
        return "redirect:/auth/login";
    }

    eventAdminService.getEvent(groupId); // 存在しないgroupIdなら例外で弾く
    session.setAttribute("originalAdminId", "admin");
    session.setAttribute("loginGroupId", groupId);

    return "redirect:/user/dashboard";
}
```

### 4-B: `endImpersonate()` エンドポイントを追加

`impersonate()` メソッドの直後に追加：

```java
@PostMapping("/end-impersonate")
public String endImpersonate() {
    String original = (String) session.getAttribute("originalAdminId");
    if (original == null) {
        return "redirect:/auth/login";
    }
    session.removeAttribute("originalAdminId");
    session.setAttribute("loginGroupId", original);
    return "redirect:/admin/dashboard";
}
```

---

## TASK-5: UserController 修正 {#task-5}

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/controller/UserController.java`

### 5-A: `dashboard()` になりすまし中フラグを追加

`dashboard()` メソッド内、`return "user/dashboard";` の直前に追加：

```java
model.addAttribute("isImpersonating", session.getAttribute("originalAdminId") != null);
```

### 5-B: `editRiddle()` にIDOR対策を追加

**変更前:**
```java
@GetMapping("/riddles/edit/{id}")
public String editRiddle(@PathVariable Integer id, Model model) {
    if (getLoginGroupId() == null)
        return "redirect:/auth/login";
    Riddle riddle = eventAdminService.getRiddle(id);
    model.addAttribute("riddle", riddle);
    return "user/riddle_edit";
}
```

**変更後:**
```java
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

### 5-C: `updateRiddle()` にIDOR対策を追加

**変更前:**
```java
    if (getLoginGroupId() == null)
        return "redirect:/auth/login";

    Riddle oldRiddle = eventAdminService.getRiddle(id);
    Integer imageId = oldRiddle.getImageId();

    if (imageFile != null && !imageFile.isEmpty()) {
        imageId = eventAdminService.uploadImage(imageFile);
    }

    eventAdminService.updateRiddle(id, question, answer, nextMsg, hintMsg, imageId);

    return "redirect:/user/riddles";
```

**変更後:**
```java
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
    } catch (IllegalArgumentException e) {
        return "redirect:/user/riddles?error=invalidImage";
    }

    return "redirect:/user/riddles";
```

### 5-D: `deleteRiddle()` にIDOR対策を追加

**変更前:**
```java
@PostMapping("/riddles/delete/{id}")
public String deleteRiddle(@PathVariable Integer id) {
    if (getLoginGroupId() == null)
        return "redirect:/auth/login";
    eventAdminService.deleteRiddle(id);
    return "redirect:/user/riddles";
}
```

**変更後:**
```java
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

### 5-E: `addRiddle()` に画像バリデーションエラーハンドリングを追加

`uploadImage()` と `registerRiddle()` の呼び出しを try-catch で囲む：

**変更前:**
```java
    Integer imageId = eventAdminService.uploadImage(imageFile);
    eventAdminService.registerRiddle(groupId, question, answer, nextMsg, imageId, hintMsg);
    return "redirect:/user/riddles";
```

**変更後:**
```java
    try {
        Integer imageId = eventAdminService.uploadImage(imageFile);
        eventAdminService.registerRiddle(groupId, question, answer, nextMsg, imageId, hintMsg);
    } catch (IllegalArgumentException e) {
        return "redirect:/user/riddles?error=invalidImage";
    }
    return "redirect:/user/riddles";
```

---

## TASK-6: ImageController 修正 {#task-6}

**対象ファイル:** `src/main/java/com/gantaro/mysterybot/controller/ImageController.java`

Content-TypeをDBから読まず、`IMAGE_JPEG` 固定に変更（Stored XSS防止）：

**変更前:**
```java
return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(riddleImage.get().getMimeType()))
        .body(riddleImage.get().getData());
```

**変更後:**
```java
return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_JPEG)
        .body(riddleImage.get().getData());
```

---

## TASK-7: HTMLテンプレート修正 {#task-7}

### 7-A: `auth/login.html`

**変更前:**
```html
<form action="/auth/login" method="post">
```
**変更後:**
```html
<form th:action="@{/auth/login}" method="post">
```

### 7-B: `auth/register.html`

**変更前:**
```html
<form action="/auth/register" method="post">
```
**変更後:**
```html
<form th:action="@{/auth/register}" method="post">
```

### 7-C: `user/dashboard.html`

変更1 — ログアウトリンクをPOSTフォームに変更：

**変更前:**
```html
<a href="/auth/logout" class="btn btn-outline-danger btn-sm">ログアウト</a>
```
**変更後:**
```html
<form th:action="@{/auth/logout}" method="post" class="d-inline">
    <button type="submit" class="btn btn-outline-danger btn-sm">ログアウト</button>
</form>
```

変更2 — イベント開始フォームのth:action化：

**変更前:**
```html
<form action="/user/start-event" method="post" onsubmit="return confirm(
```
**変更後:**
```html
<form th:action="@{/user/start-event}" method="post" onsubmit="return confirm(
```

変更3 — なりすまし警告バナーを `<body>` 直後に挿入：

```html
<div th:if="${isImpersonating}" class="alert alert-warning m-0 rounded-0 text-center" role="alert">
    <strong>管理者モードで閲覧中です。</strong>
    <form th:action="@{/admin/end-impersonate}" method="post" class="d-inline ms-3">
        <button type="submit" class="btn btn-sm btn-warning">管理者画面に戻る</button>
    </form>
</div>
```

### 7-D: `user/riddle_edit.html`

**変更前:**
```html
<form action="/user/riddles/update" method="post" enctype="multipart/form-data">
```
**変更後:**
```html
<form th:action="@{/user/riddles/update}" method="post" enctype="multipart/form-data">
```

### 7-E: `user/riddles.html`

変更1:
```html
<!-- 変更前 -->
<form action="/user/riddles/settings" method="post" class="d-flex align-items-center">
<!-- 変更後 -->
<form th:action="@{/user/riddles/settings}" method="post" class="d-flex align-items-center">
```

変更2:
```html
<!-- 変更前 -->
<form action="/user/riddles/add" method="post" enctype="multipart/form-data">
<!-- 変更後 -->
<form th:action="@{/user/riddles/add}" method="post" enctype="multipart/form-data">
```

---

## 完了確認チェックリスト

実装後、以下を確認すること：

```bash
# 1. コンパイルエラーがないこと
./gradlew compileJava

# 2. TestControllerが存在しないこと（エラーになればOK）
ls src/main/java/com/gantaro/mysterybot/controller/TestController.java

# 3. BCryptが使われていること
grep "passwordEncoder" src/main/java/com/gantaro/mysterybot/service/EventAdminService.java

# 4. IDOR対策が入っていること
grep "getRiddleOwnedBy" src/main/java/com/gantaro/mysterybot/service/EventAdminService.java
grep "getRiddleOwnedBy" src/main/java/com/gantaro/mysterybot/controller/UserController.java

# 5. logoutがPOSTになっていること
grep "PostMapping.*logout" src/main/java/com/gantaro/mysterybot/controller/AuthController.java

# 6. th:actionが全フォームにあること（action= のみが残っていないこと）
grep -rn 'form action=' src/main/resources/templates/
```
