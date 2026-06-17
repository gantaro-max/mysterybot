# 実装指示書: フェーズ3 — 認証チェックの共通化（HandlerInterceptor）

## 背景・目的

現在、認証チェックがすべてのコントローラーメソッドの冒頭に重複して書かれている。

```java
// UserController（10箇所）
String groupId = getLoginGroupId();
if (groupId == null) return "redirect:/auth/login";

// AdminController（8箇所）
if (!"admin".equals(getLoginGroupId())) return "redirect:/auth/login";
```

`HandlerInterceptor` で一元管理し、コントローラーから重複コードを削除する。

---

## 実装対象ファイル

### 新規作成
- `src/main/java/com/gantaro/mysterybot/config/AuthInterceptor.java`
- `src/main/java/com/gantaro/mysterybot/config/WebConfig.java`

### 変更
- `src/main/java/com/gantaro/mysterybot/controller/UserController.java` — 認証チェック削除
- `src/main/java/com/gantaro/mysterybot/controller/AdminController.java` — 認証チェック削除

---

## 実装仕様

### `AuthInterceptor`

```java
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession(false);
        String path = request.getRequestURI();

        if (path.startsWith("/admin/")) {
            String loginGroupId = (session != null)
                    ? (String) session.getAttribute("loginGroupId") : null;
            if (!"admin".equals(loginGroupId)) {
                response.sendRedirect("/auth/login");
                return false;
            }
        } else if (path.startsWith("/user/")) {
            String loginGroupId = (session != null)
                    ? (String) session.getAttribute("loginGroupId") : null;
            if (loginGroupId == null) {
                response.sendRedirect("/auth/login");
                return false;
            }
        }
        return true;
    }
}
```

### `WebConfig`

```java
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/user/**", "/admin/**");
    }
}
```

### `UserController` の修正

各メソッドから以下の2行を削除する（全11メソッド）。

```java
// 削除対象
String groupId = getLoginGroupId();
if (groupId == null) return "redirect:/auth/login";
```

削除後、`groupId` は以下で取得する（インターセプターが通過済みなので null にならない）。

```java
String groupId = getLoginGroupId();
```

`getLoginGroupId()` メソッド自体（`session.getAttribute("loginGroupId")` を返す private メソッド）は残す。

### `AdminController` の修正

各メソッドから以下を削除する（全8メソッド）。

```java
// 削除対象（パターン1）
if (!"admin".equals(getLoginGroupId())) return "redirect:/auth/login";

// 削除対象（パターン2）
if (!"admin".equals(getLoginGroupId()))
    return "redirect:/auth/login";
```

`getLoginGroupId()` メソッド自体は残す。

---

## 制約・注意事項

- `/auth/**` と `/public/**` と `/callback` はインターセプターの対象外にする（`addPathPatterns` で `/user/**` と `/admin/**` のみ指定する）。
- `AdminController.forceDeleteEvent()` にある **admin 自身の削除ガード** は認証チェックとは別ロジックなので削除しない。
  ```java
  if ("admin".equals(groupId)) {
      return "redirect:/admin/dashboard?error=admin_cannot_delete";
  }
  ```
- `UserController.startEvent()` にある **イベント開始済みチェック** も削除しない。
  ```java
  if (group.getStartedAt() != null) return "redirect:/user/dashboard";
  ```
- インターセプターは Spring MVC レイヤーで動作するため、LINE Webhook（`/callback`）には干渉しない。

---

## 完了条件

- [ ] `AuthInterceptor` と `WebConfig` が作成されている
- [ ] `UserController` の全メソッドから `if (groupId == null) return "redirect:/auth/login";` が削除されている
- [ ] `AdminController` の全メソッドから `if (!"admin".equals(...)) return "redirect:/auth/login";` が削除されている
- [ ] `./gradlew build` が成功する
- [ ] 未ログイン状態で `/user/dashboard` にアクセスすると `/auth/login` にリダイレクトされる
- [ ] 非Admin で `/admin/dashboard` にアクセスすると `/auth/login` にリダイレクトされる
- [ ] `/auth/login` や `/public/image/{uuid}` は認証なしでアクセスできる
