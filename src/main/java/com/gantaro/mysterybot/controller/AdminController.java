package com.gantaro.mysterybot.controller;

import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.gantaro.mysterybot.entity.MasterRiddle;
import com.gantaro.mysterybot.entity.TeamGroup;
import com.gantaro.mysterybot.service.CatalogService;
import com.gantaro.mysterybot.service.EventAdminService;
import com.gantaro.mysterybot.service.RiddleService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final EventAdminService eventAdminService;
    private final CatalogService catalogService;
    private final RiddleService riddleService;
    private final HttpSession session;

    private String getLoginGroupId() {
        return (String) session.getAttribute("loginGroupId");
    }


    // S1. 統合ダッシュボード表示
    @GetMapping("/dashboard")
    public String superDashboard(Model model) {
        // IDが "admin" でなければログイン画面へ弾く
        if (!"admin".equals(getLoginGroupId())) {
            return "redirect:/auth/login";
        }

        List<TeamGroup> allEvents = eventAdminService.getAllEvents();
        model.addAttribute("events", allEvents);
        return "admin/dashboard";
    }

    // S2. ゴッドログイン（パスワードなしで該当イベントの管理画面へ侵入）
    @PostMapping("/impersonate/{groupId}")
    public String impersonate(@PathVariable String groupId) {
        if (!"admin".equals(getLoginGroupId())) {
            return "redirect:/auth/login";
        }

        eventAdminService.getEvent(groupId);
        session.setAttribute("originalAdminId", "admin");
        session.setAttribute("loginGroupId", groupId);

        return "redirect:/user/dashboard";
    }

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

    // S3. イベント強制削除
    @PostMapping("/delete/{groupId}")
    public String forceDeleteEvent(@PathVariable String groupId) {
        if (!"admin".equals(getLoginGroupId())) {
            return "redirect:/auth/login";
        }

        // 自身のID(admin)は消さないようにガード
        if ("admin".equals(groupId)) {
            return "redirect:/admin/dashboard?error=admin_cannot_delete";
        }

        eventAdminService.deleteEvent(groupId);
        return "redirect:/admin/dashboard";
    }

    // ▼▼▼ カタログ管理機能 (スーパーAdmin用) ▼▼▼

    // S4. カタログ一覧・編集画面表示
    @GetMapping("/catalog")
    public String catalog(Model model) {
        if (!"admin".equals(getLoginGroupId()))
            return "redirect:/auth/login";

        model.addAttribute("catalog", catalogService.getCatalog());
        // 管理者権限ON（これで追加フォームが表示されます）
        model.addAttribute("isSuperAdmin", true);
        // 「戻る」ボタンのリンク先
        model.addAttribute("backLink", "/admin/dashboard");

        // テンプレートはUser用のものを共用します
        return "user/catalog";
    }

    // S5. マスターデータ登録
    @PostMapping("/catalog/add-master")
    public String addMasterRiddle(@RequestParam String question, @RequestParam String answer,
            @RequestParam String nextMsg, @RequestParam String hintMsg,
            @RequestParam String category, @RequestParam(required = false) MultipartFile imageFile)
            throws IOException {

        if (!"admin".equals(getLoginGroupId()))
            return "redirect:/auth/login";

        try {
            Integer imgId = riddleService.uploadImage(imageFile);
            catalogService.registerMasterRiddle(question, answer, nextMsg, hintMsg, imgId,
                    category);
        } catch (IllegalArgumentException e) {
            return "redirect:/admin/catalog?error=invalidImage";
        }

        return "redirect:/admin/catalog";
    }

    // S6. イベント時間をリセット（準備中に戻す）
    @PostMapping("/reset-time/{groupId}")
    public String resetEventTime(@PathVariable String groupId) {
        // 管理者権限チェック
        if (!"admin".equals(getLoginGroupId())) {
            return "redirect:/auth/login";
        }

        eventAdminService.resetEventTime(groupId);
        return "redirect:/admin/dashboard";
    }

    // S7. カタログ問題の削除
    @PostMapping("/catalog/delete/{id}")
    public String deleteMasterRiddle(@PathVariable Integer id) {
        if (!"admin".equals(getLoginGroupId())) {
            return "redirect:/auth/login";
        }
        catalogService.deleteMasterRiddle(id);
        return "redirect:/admin/catalog";
    }

    // ▼▼▼ 編集画面表示 ▼▼▼
    @GetMapping("/catalog/edit/{id}")
    public String editMasterRiddlePage(@PathVariable Integer id, Model model) {
        if (!"admin".equals(getLoginGroupId())) {
            return "redirect:/auth/login";
        }

        MasterRiddle riddle = catalogService.getMasterRiddle(id);
        model.addAttribute("riddle", riddle);
        return "admin/master_riddle_edit";
    }

    // ▼▼▼ 更新実行 ▼▼▼
    @PostMapping("/catalog/update")
    public String updateMasterRiddle(@RequestParam Integer id, @RequestParam String question,
            @RequestParam String answer, @RequestParam String nextMsg, @RequestParam String hintMsg,
            @RequestParam String category, @RequestParam(required = false) MultipartFile imageFile)
            throws IOException {

        if (!"admin".equals(getLoginGroupId())) {
            return "redirect:/auth/login";
        }

        try {
            Integer imgId = riddleService.uploadImage(imageFile);
            catalogService.updateMasterRiddle(id, question, answer, nextMsg, hintMsg, imgId,
                    category);
        } catch (IllegalArgumentException e) {
            return "redirect:/admin/catalog?error=invalidImage";
        }

        return "redirect:/admin/catalog";
    }


}
