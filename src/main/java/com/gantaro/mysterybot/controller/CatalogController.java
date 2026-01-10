package com.gantaro.mysterybot.controller;

import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.gantaro.mysterybot.entity.MasterRiddle;
import com.gantaro.mysterybot.service.EventAdminService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/catalog") // 基本URLを /admin/catalog に統一
@RequiredArgsConstructor
public class CatalogController {

    private final EventAdminService eventAdminService;
    private final HttpSession session;

    // 共通のログインチェック用ヘルパー
    private String getLoginGroupId() {
        return (String) session.getAttribute("loginGroupId");
    }

    // 1. カタログ画面表示
    @GetMapping
    public String index(Model model) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/admin/login";

        List<MasterRiddle> catalog = eventAdminService.getCatalog();
        model.addAttribute("catalog", catalog);

        // IDが "admin" の場合のみスーパー管理者権限を与える
        boolean isSuperAdmin = "admin".equals(groupId);
        model.addAttribute("isSuperAdmin", isSuperAdmin);

        return "admin/catalog";
    }

    // 2. カタログからインポート
    @PostMapping("/import")
    public String importRiddle(@RequestParam Integer id) {
        String groupId = getLoginGroupId();
        if (groupId != null) {
            eventAdminService.importFromCatalog(groupId, id);
        }
        return "redirect:/admin/riddles";
    }

    // 3. マスターデータ登録 (管理者のみ)
    @PostMapping("/add-master")
    public String addMasterRiddle(@RequestParam String question, @RequestParam String answer,
            @RequestParam String nextMsg, @RequestParam String hintMsg,
            @RequestParam String category, @RequestParam(required = false) MultipartFile imageFile)
            throws IOException {

        // スーパーAdminチェック
        if (!"admin".equals(getLoginGroupId())) {
            return "redirect:/admin/dashboard";
        }

        Integer imgId = eventAdminService.uploadImage(imageFile);
        eventAdminService.registerMasterRiddle(question, answer, nextMsg, hintMsg, imgId, category);

        return "redirect:/admin/catalog";
    }
}
