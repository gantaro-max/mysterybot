package com.gantaro.mysterybot.controller;

import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.gantaro.mysterybot.entity.Player;
import com.gantaro.mysterybot.entity.Riddle;
import com.gantaro.mysterybot.entity.TeamGroup;
import com.gantaro.mysterybot.service.EventAdminService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final EventAdminService eventAdminService;
    private final HttpSession session;

    @Value("${line.bot.friend-url}")
    private String botFriendUrl;

    private String getLoginGroupId() {
        return (String) session.getAttribute("loginGroupId");
    }

    // 1. ダッシュボード
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/auth/login";

        TeamGroup group = eventAdminService.getEvent(groupId);
        model.addAttribute("group", group);
        model.addAttribute("botFriendUrl", botFriendUrl);
        return "user/dashboard";
    }

    // 2. ランキング
    @GetMapping("/ranking")
    public String ranking(Model model) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/auth/login";

        TeamGroup group = eventAdminService.getEvent(groupId);
        List<Player> ranking = eventAdminService.getRanking(groupId);

        model.addAttribute("group", group);
        model.addAttribute("ranking", ranking);
        return "user/ranking";
    }

    // 3. 謎一覧
    @GetMapping("/riddles")
    public String listRiddles(Model model) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/auth/login";

        TeamGroup group = eventAdminService.getEvent(groupId);
        List<Riddle> riddles = eventAdminService.getRiddles(groupId);

        model.addAttribute("group", group);
        model.addAttribute("riddles", riddles);
        model.addAttribute("groupId", groupId);
        return "user/riddles";
    }

    // 4. 謎の登録 (★修正: 画像とヒントに対応)
    @PostMapping("/riddles/add")
    public String addRiddle(@RequestParam String question, @RequestParam String answer,
            @RequestParam String nextMsg, @RequestParam(required = false) String hintMsg, // ★追加
            @RequestParam(required = false) MultipartFile imageFile // ★追加
    ) throws IOException { // ★追加
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/auth/login";

        // 画像をアップロードしてIDを取得（なければnull）
        Integer imageId = eventAdminService.uploadImage(imageFile);

        // サービスへ全データを渡す
        eventAdminService.registerRiddle(groupId, question, answer, nextMsg, imageId, hintMsg);
        return "redirect:/user/riddles";
    }

    // 5. 編集画面
    @GetMapping("/riddles/edit/{id}")
    public String editRiddle(@PathVariable Integer id, Model model) {
        if (getLoginGroupId() == null)
            return "redirect:/auth/login";
        Riddle riddle = eventAdminService.getRiddle(id);
        model.addAttribute("riddle", riddle);
        return "user/riddle_edit";
    }

    // 6. 更新処理 (画像とヒントに対応)
    @PostMapping("/riddles/update")
    public String updateRiddle(@RequestParam Integer id, @RequestParam String question,
            @RequestParam String answer, @RequestParam String nextMsg,
            @RequestParam(required = false) String hintMsg,
            @RequestParam(required = false) MultipartFile imageFile) throws IOException {

        if (getLoginGroupId() == null)
            return "redirect:/auth/login";

        // 1. まず現在の情報を取得（古い画像IDを知るため）
        Riddle oldRiddle = eventAdminService.getRiddle(id);
        Integer imageId = oldRiddle.getImageId();

        // 2. 新しい画像がアップロードされていれば保存してIDを更新
        if (imageFile != null && !imageFile.isEmpty()) {
            imageId = eventAdminService.uploadImage(imageFile);
        }

        // 3. 更新実行
        eventAdminService.updateRiddle(id, question, answer, nextMsg, hintMsg, imageId);

        return "redirect:/user/riddles";
    }

    // 7. 削除処理
    @PostMapping("/riddles/delete/{id}")
    public String deleteRiddle(@PathVariable Integer id) {
        if (getLoginGroupId() == null)
            return "redirect:/auth/login";
        eventAdminService.deleteRiddle(id);
        return "redirect:/user/riddles";
    }

    // 8. 設定変更
    @PostMapping("/riddles/settings")
    public String updateSettings(@RequestParam(required = false) Boolean isRandom) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/auth/login";
        eventAdminService.updateEventSettings(groupId, isRandom != null);
        return "redirect:/user/riddles";
    }

    // 9. イベント開始
    @PostMapping("/start-event")
    public String startEvent(Model model) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/auth/login";
        TeamGroup group = eventAdminService.getEvent(groupId);
        if (group.getStartedAt() != null)
            return "redirect:/user/dashboard";
        eventAdminService.startEvent(groupId);
        return "redirect:/user/dashboard";
    }

    // ▼▼▼ カタログ機能 (主催者用) ▼▼▼

    // 10. カタログ一覧表示
    @GetMapping("/catalog")
    public String catalog(Model model) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/auth/login";

        // カタログデータを取得
        model.addAttribute("catalog", eventAdminService.getCatalog());
        // 主催者は管理者ではないのでfalse
        model.addAttribute("isSuperAdmin", false);
        // 「戻る」ボタンのリンク先
        model.addAttribute("backLink", "/user/riddles");

        return "user/catalog"; // templates/user/catalog.html を表示
    }

    // 11. カタログからインポート
    @PostMapping("/catalog/import")
    public String importRiddle(@RequestParam Integer id) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/auth/login";

        eventAdminService.importFromCatalog(groupId, id);
        return "redirect:/user/riddles";
    }

}
