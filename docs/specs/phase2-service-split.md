# 実装指示書: フェーズ2 — EventAdminService の分割

## 背景・目的

`EventAdminService` が 330 行超の God Service になっており、ログイン・リドルCRUD・画像処理・カタログ・管理者操作をすべて1クラスで扱っている。責務を分割して保守性を高める。

---

## 分割方針

| 新クラス | 責務 | 移動するメソッド |
|:--|:--|:--|
| `AuthService` | ログイン・BCrypt・イベント登録 | `login()`, `createEvent()` |
| `RiddleService` | リドルCRUD・画像アップロード | `getRiddle()`, `getRiddleOwnedBy()`, `getRiddles()`, `registerRiddle()`, `updateRiddle()`, `deleteRiddle()`, `uploadImage()`, `isAllowedImageBytes()`（private）, `validateImageDimensions()`（private） |
| `CatalogService` | マスター問題CRUD・インポート | `getCatalog()`, `importFromCatalog()`, `registerMasterRiddle()`, `getMasterRiddle()`, `updateMasterRiddle()`, `deleteMasterRiddle()` |
| `EventAdminService`（縮小） | イベント取得・開始・設定・ランキング・管理者操作 | `getEvent()`, `getAllEvents()`, `getRanking()`, `startEvent()`, `updateEventSettings()`, `deleteEvent()`, `resetEventTime()` |

---

## 実装対象ファイル

### 新規作成
- `src/main/java/com/gantaro/mysterybot/service/AuthService.java`
- `src/main/java/com/gantaro/mysterybot/service/RiddleService.java`
- `src/main/java/com/gantaro/mysterybot/service/CatalogService.java`

### 変更
- `src/main/java/com/gantaro/mysterybot/service/EventAdminService.java` — 上記メソッドを削除し縮小
- `src/main/java/com/gantaro/mysterybot/controller/AuthController.java` — `EventAdminService` → `AuthService` に差し替え
- `src/main/java/com/gantaro/mysterybot/controller/UserController.java` — `EventAdminService` に加えて `RiddleService`・`CatalogService` を注入
- `src/main/java/com/gantaro/mysterybot/controller/AdminController.java` — `EventAdminService` に加えて `CatalogService` を注入

---

## 実装仕様

### `AuthService`

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    private final TeamGroupRepository teamGroupRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> RESERVED_GROUP_IDS =
            Set.of("admin", "system", "root", "superadmin", "test");

    // 現 EventAdminService.login() をそのまま移動
    @Transactional
    public boolean login(String groupId, String password) { ... }

    // 現 EventAdminService.createEvent() をそのまま移動
    @Transactional
    public void createEvent(String groupId, String groupName, String password) { ... }

    // 現 EventAdminService.isBCryptHash() をそのまま移動（private）
    private boolean isBCryptHash(String value) { ... }
}
```

### `RiddleService`

定数 `MAX_IMAGE_BYTES`, `MAX_IMAGE_PIXELS`, `MAX_IMAGE_DIMENSION` も移動する。

```java
@Service
@RequiredArgsConstructor
public class RiddleService {
    private final RiddleRepository riddleRepository;
    private final RiddleImageRepository riddleImageRepository;
    private final SolvedHistoryRepository solvedHistoryRepository;

    // 以下をそのまま移動
    public Riddle getRiddle(Integer id) { ... }
    public Riddle getRiddleOwnedBy(Integer id, String groupId) { ... }
    public List<Riddle> getRiddles(String groupId) { ... }
    public void registerRiddle(...) { ... }
    public void updateRiddle(...) { ... }
    public void deleteRiddle(Integer id, String groupId) { ... }
    public Integer uploadImage(MultipartFile file) throws IOException { ... }
    private boolean isAllowedImageBytes(byte[] data) { ... }
    private void validateImageDimensions(BufferedImage image) { ... }
}
```

### `CatalogService`

`importFromCatalog` は内部で `registerRiddle` を呼ぶため、`RiddleService` に依存する。

```java
@Service
@RequiredArgsConstructor
public class CatalogService {
    private final MasterRiddleRepository masterRiddleRepository;
    private final RiddleService riddleService;  // importFromCatalog で使用

    public List<MasterRiddle> getCatalog() { ... }
    public void importFromCatalog(String groupId, Integer masterRiddleId) {
        MasterRiddle m = masterRiddleRepository.findById(masterRiddleId);
        if (m == null) return;
        riddleService.registerRiddle(groupId, m.getQuestion(), m.getAnswer(),
                m.getNextMsg(), m.getImageId(), m.getHintMsg());
    }
    public void registerMasterRiddle(...) { ... }
    public MasterRiddle getMasterRiddle(Integer id) { ... }
    public void updateMasterRiddle(...) { ... }
    public void deleteMasterRiddle(Integer id) { ... }
}
```

### `EventAdminService`（縮小後）

以下のメソッドのみを残す。不要になった import も削除する。

```java
@Service
@RequiredArgsConstructor
public class EventAdminService {
    private final TeamGroupRepository teamGroupRepository;
    private final PlayerRepository playerRepository;
    private final RiddleRepository riddleRepository;

    public TeamGroup getEvent(String groupId) { ... }
    public List<TeamGroup> getAllEvents() { ... }
    public List<Player> getRanking(String groupId) { ... }
    public void startEvent(String groupId) { ... }
    public void updateEventSettings(String groupId, Boolean isRandom) { ... }
    public void deleteEvent(String groupId) { ... }
    public void resetEventTime(String groupId) { ... }
}
```

### コントローラーの修正

#### `AuthController`
- フィールドを `EventAdminService` → `AuthService` に変更
- `eventAdminService.login()` → `authService.login()`
- `eventAdminService.createEvent()` → `authService.createEvent()`

#### `UserController`
- `RiddleService riddleService` と `CatalogService catalogService` フィールドを追加
- メソッド別に呼び出し先を変更：

| 現在の呼び出し | 変更後 |
|:--|:--|
| `eventAdminService.uploadImage()` | `riddleService.uploadImage()` |
| `eventAdminService.registerRiddle()` | `riddleService.registerRiddle()` |
| `eventAdminService.getRiddleOwnedBy()` | `riddleService.getRiddleOwnedBy()` |
| `eventAdminService.updateRiddle()` | `riddleService.updateRiddle()` |
| `eventAdminService.deleteRiddle()` | `riddleService.deleteRiddle()` |
| `eventAdminService.getRiddles()` | `riddleService.getRiddles()` |
| `eventAdminService.getCatalog()` | `catalogService.getCatalog()` |
| `eventAdminService.importFromCatalog()` | `catalogService.importFromCatalog()` |

`getEvent()` と `getRanking()` は引き続き `eventAdminService` を使う。

#### `AdminController`
- `CatalogService catalogService` フィールドを追加
- メソッド別に呼び出し先を変更：

| 現在の呼び出し | 変更後 |
|:--|:--|
| `eventAdminService.uploadImage()` | `riddleService.uploadImage()` — AdminController にも `RiddleService` を注入する |
| `eventAdminService.getCatalog()` | `catalogService.getCatalog()` |
| `eventAdminService.registerMasterRiddle()` | `catalogService.registerMasterRiddle()` |
| `eventAdminService.getMasterRiddle()` | `catalogService.getMasterRiddle()` |
| `eventAdminService.updateMasterRiddle()` | `catalogService.updateMasterRiddle()` |
| `eventAdminService.deleteMasterRiddle()` | `catalogService.deleteMasterRiddle()` |

---

## 制約・注意事項

- **既存の動作は一切変えない。** リファクタリングなのでメソッドのロジック自体は変更禁止。
- `@Transactional` アノテーションは元のメソッドに付いていたものをそのまま引き継ぐ。
- コンストラクタインジェクションは `@RequiredArgsConstructor`（Lombok）を使う。
- Spring の循環依存を避けるため、`EventAdminService` は新しい3サービスに依存しない。

---

## 完了条件

- [ ] `AuthService`, `RiddleService`, `CatalogService` の3クラスが作成されている
- [ ] `EventAdminService` から移動したメソッドが削除されている
- [ ] `./gradlew build` が成功する
- [ ] ローカル起動後、ログイン・リドル追加・カタログインポートの動作が変わっていない
