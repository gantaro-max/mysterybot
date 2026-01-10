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
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

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
            return "redirect:/admin/login";

        TeamGroup group = eventAdminService.getEvent(groupId);
        model.addAttribute("group", group);
        model.addAttribute("botFriendUrl", botFriendUrl);
        return "admin/dashboard";
    }

    // 2. ランキング
    @GetMapping("/ranking")
    public String ranking(Model model) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/admin/login";

        TeamGroup group = eventAdminService.getEvent(groupId);
        List<Player> ranking = eventAdminService.getRanking(groupId);

        model.addAttribute("group", group);
        model.addAttribute("ranking", ranking);
        return "admin/ranking";
    }

    // 3. 謎一覧
    @GetMapping("/riddles")
    public String listRiddles(Model model) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/admin/login";

        TeamGroup group = eventAdminService.getEvent(groupId);
        List<Riddle> riddles = eventAdminService.getRiddles(groupId);

        model.addAttribute("group", group);
        model.addAttribute("riddles", riddles);
        model.addAttribute("groupId", groupId);
        return "admin/riddles";
    }

    // 4. 謎の登録 (★修正: 画像とヒントに対応)
    @PostMapping("/riddles/add")
    public String addRiddle(@RequestParam String question, @RequestParam String answer,
            @RequestParam String nextMsg, @RequestParam(required = false) String hintMsg, // ★追加
            @RequestParam(required = false) MultipartFile imageFile // ★追加
    ) throws IOException { // ★追加
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/admin/login";

        // 画像をアップロードしてIDを取得（なければnull）
        Integer imageId = eventAdminService.uploadImage(imageFile);

        // サービスへ全データを渡す
        eventAdminService.registerRiddle(groupId, question, answer, nextMsg, imageId, hintMsg);
        return "redirect:/admin/riddles";
    }

    // 5. 編集画面
    @GetMapping("/riddles/edit/{id}")
    public String editRiddle(@PathVariable Integer id, Model model) {
        if (getLoginGroupId() == null)
            return "redirect:/admin/login";
        Riddle riddle = eventAdminService.getRiddle(id);
        model.addAttribute("riddle", riddle);
        return "admin/riddle_edit";
    }

    // 6. 更新処理 (画像とヒントに対応)
    @PostMapping("/riddles/update")
    public String updateRiddle(@RequestParam Integer id, @RequestParam String question,
            @RequestParam String answer, @RequestParam String nextMsg,
            @RequestParam(required = false) String hintMsg,
            @RequestParam(required = false) MultipartFile imageFile) throws IOException {

        if (getLoginGroupId() == null)
            return "redirect:/admin/login";

        // 1. まず現在の情報を取得（古い画像IDを知るため）
        Riddle oldRiddle = eventAdminService.getRiddle(id);
        Integer imageId = oldRiddle.getImageId();

        // 2. 新しい画像がアップロードされていれば保存してIDを更新
        if (imageFile != null && !imageFile.isEmpty()) {
            imageId = eventAdminService.uploadImage(imageFile);
        }

        // 3. 更新実行
        eventAdminService.updateRiddle(id, question, answer, nextMsg, hintMsg, imageId);

        return "redirect:/admin/riddles";
    }

    // 7. 削除処理
    @PostMapping("/riddles/delete/{id}")
    public String deleteRiddle(@PathVariable Integer id) {
        if (getLoginGroupId() == null)
            return "redirect:/admin/login";
        eventAdminService.deleteRiddle(id);
        return "redirect:/admin/riddles";
    }

    // 8. 設定変更
    @PostMapping("/riddles/settings")
    public String updateSettings(@RequestParam(required = false) Boolean isRandom) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/admin/login";
        eventAdminService.updateEventSettings(groupId, isRandom != null);
        return "redirect:/admin/riddles";
    }

    // 9. イベント開始
    @PostMapping("/start-event")
    public String startEvent(Model model) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/admin/login";
        TeamGroup group = eventAdminService.getEvent(groupId);
        if (group.getStartedAt() != null)
            return "redirect:/admin/dashboard";
        eventAdminService.startEvent(groupId);
        return "redirect:/admin/dashboard";
    }

    // ▼▼▼ スーパーAdmin（統合管理）機能 ▼▼▼

    // S1. 統合ダッシュボード表示
    @GetMapping("/super/dashboard")
    public String superDashboard(Model model) {
        // IDが "admin" でなければログイン画面へ弾く
        if (!"admin".equals(getLoginGroupId())) {
            return "redirect:/admin/login";
        }

        List<TeamGroup> allEvents = eventAdminService.getAllEvents();
        model.addAttribute("events", allEvents);
        return "admin/super_dashboard";
    }

    // S2. ゴッドログイン（パスワードなしで該当イベントの管理画面へ侵入）
    @PostMapping("/super/login/{groupId}")
    public String godLogin(@PathVariable String groupId) {
        if (!"admin".equals(getLoginGroupId())) {
            return "redirect:/admin/login";
        }

        // セッションを該当グループIDで上書き（＝なりすましログイン完了）
        session.setAttribute("loginGroupId", groupId);

        return "redirect:/admin/dashboard";
    }

    // S3. イベント強制削除
    @PostMapping("/super/delete/{groupId}")
    public String forceDeleteEvent(@PathVariable String groupId) {
        if (!"admin".equals(getLoginGroupId())) {
            return "redirect:/admin/login";
        }

        // 自身のID(admin)は消さないようにガード
        if ("admin".equals(groupId)) {
            return "redirect:/admin/super/dashboard?error=admin_cannot_delete";
        }

        eventAdminService.deleteEvent(groupId);
        return "redirect:/admin/super/dashboard";
    }
}
