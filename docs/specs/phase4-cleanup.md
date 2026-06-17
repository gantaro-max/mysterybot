# 実装指示書: フェーズ4 — 二重所有権チェック解消・DTO整備

## 背景・目的

フェーズ2・3 完了後に残る2つの小さな問題を解消する。

1. **`updateRiddle` で所有権チェックが2回走る**: コントローラーで `getRiddleOwnedBy()` を呼び、サービスの `updateRiddle()` 内部でも再度 `getRiddleOwnedBy()` を呼んでいる。
2. **`registerMasterRiddle` / `updateMasterRiddle` の引数が多すぎる**: 1文字変数名で可読性が低い。DTO に置き換える。

---

## 実装対象ファイル

### 新規作成
- `src/main/java/com/gantaro/mysterybot/dto/MasterRiddleRequest.java`

### 変更
- `src/main/java/com/gantaro/mysterybot/controller/UserController.java`
- `src/main/java/com/gantaro/mysterybot/service/RiddleService.java`（フェーズ2完了後）
- `src/main/java/com/gantaro/mysterybot/service/CatalogService.java`（フェーズ2完了後）
- `src/main/java/com/gantaro/mysterybot/controller/AdminController.java`

---

## 実装仕様

### 1. 二重所有権チェックの解消

#### 問題の箇所（`UserController.updateRiddle()`）

```java
// 現状: コントローラーで1回目
Riddle oldRiddle = eventAdminService.getRiddleOwnedBy(id, groupId);
Integer imageId = oldRiddle.getImageId();
if (imageFile != null && !imageFile.isEmpty()) {
    imageId = riddleService.uploadImage(imageFile);
}
// サービス内部でも getRiddleOwnedBy を呼んでいる（2回目）
riddleService.updateRiddle(id, groupId, question, answer, nextMsg, hintMsg, imageId);
```

#### 修正方針

`RiddleService.updateRiddle()` が「旧imageIdの引き継ぎ」も内部で処理するよう責務を移す。コントローラーは画像ファイルとIDだけ渡せばよい。

#### `RiddleService.updateRiddle()` の新シグネチャ

```java
@Transactional
public void updateRiddle(Integer id, String groupId, String question, String answer,
        String nextMsg, String hintMsg, MultipartFile imageFile) throws IOException {

    Riddle riddle = getRiddleOwnedBy(id, groupId);  // ここで1回だけ所有権チェック

    riddle.setQuestion(question);
    riddle.setAnswer(answer);
    riddle.setNextMsg(nextMsg);
    riddle.setHintMsg(hintMsg);

    if (imageFile != null && !imageFile.isEmpty()) {
        Integer newImageId = uploadImage(imageFile);  // 新画像があれば更新
        riddle.setImageId(newImageId);
    }
    // 画像がない場合は既存の imageId をそのまま維持（riddle オブジェクトから取得済み）

    riddleRepository.update(riddle);
}
```

#### `UserController.updateRiddle()` の修正後

```java
@PostMapping("/riddles/update")
public String updateRiddle(@RequestParam Integer id, @RequestParam String question,
        @RequestParam String answer, @RequestParam String nextMsg,
        @RequestParam(required = false) String hintMsg,
        @RequestParam(required = false) MultipartFile imageFile) throws IOException {

    String groupId = getLoginGroupId();
    try {
        riddleService.updateRiddle(id, groupId, question, answer, nextMsg, hintMsg, imageFile);
    } catch (SecurityException e) {
        return "redirect:/user/riddles";
    } catch (IllegalArgumentException e) {
        return "redirect:/user/riddles?error=invalidImage";
    }
    return "redirect:/user/riddles";
}
```

---

### 2. `MasterRiddleRequest` DTO の作成

```java
package com.gantaro.mysterybot.dto;

public record MasterRiddleRequest(
        String question,
        String answer,
        String nextMsg,
        String hintMsg,
        Integer imageId,
        String category
) {}
```

#### `CatalogService` のメソッドシグネチャ変更

```java
// 変更前
public void registerMasterRiddle(String q, String a, String n, String h, Integer imgId, String cat)
public void updateMasterRiddle(Integer id, String question, String answer, String nextMsg,
        String hintMsg, Integer imageId, String category)

// 変更後
public void registerMasterRiddle(MasterRiddleRequest req)
public void updateMasterRiddle(Integer id, MasterRiddleRequest req)
```

内部ロジックは変えず、`req.question()` / `req.answer()` 等でフィールドにアクセスする。

#### `AdminController` の呼び出し側変更

```java
// addMasterRiddle() メソッド内
Integer imgId = riddleService.uploadImage(imageFile);
catalogService.registerMasterRiddle(
        new MasterRiddleRequest(question, answer, nextMsg, hintMsg, imgId, category));

// updateMasterRiddle() メソッド内
Integer imgId = riddleService.uploadImage(imageFile);
catalogService.updateMasterRiddle(id,
        new MasterRiddleRequest(question, answer, nextMsg, hintMsg, imgId, category));
```

---

## 制約・注意事項

- `RiddleService.updateRiddle()` のシグネチャが変わるため、他に呼び出し箇所があれば合わせて修正する（現状はコントローラーのみ）。
- `MasterRiddleRequest` は Java 16+ の `record` を使う。Java 21 環境なので問題ない。
- ロジック自体は変えない。リファクタリングのみ。

---

## 完了条件

- [ ] `UserController.updateRiddle()` 内で `getRiddleOwnedBy()` を直接呼んでいない
- [ ] `RiddleService.updateRiddle()` が `MultipartFile` を受け取り、内部で画像処理と所有権チェックを一括して行っている
- [ ] `MasterRiddleRequest` レコードが作成されている
- [ ] `CatalogService.registerMasterRiddle()` と `updateMasterRiddle()` が `MasterRiddleRequest` を受け取るシグネチャになっている
- [ ] `./gradlew build` が成功する
- [ ] リドル編集・マスター問題登録・更新の動作が変わっていない
