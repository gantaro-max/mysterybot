package com.gantaro.mysterybot.controller;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    // ★GameServiceではなくEventAdminServiceを使います
    private final EventAdminService eventAdminService;
    private final HttpSession session;

    // セッションチェック用のヘルパーメソッド
    private String getLoginGroupId() {
        return (String) session.getAttribute("loginGroupId");
    }

    // 1. ダッシュボード表示 (旧indexから変更)
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/admin/login";

        TeamGroup group = eventAdminService.getEvent(groupId);
        model.addAttribute("group", group);
        return "admin/dashboard";
    }

    // 2. ランキング画面 (新規)
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

    // 3. 謎（問題）の一覧表示 (シナリオ編集)
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

    // 4. 謎（問題）の登録処理
    @PostMapping("/riddles/add")
    public String addRiddle(@RequestParam String question, @RequestParam String answer,
            @RequestParam String nextMsg) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/admin/login";

        eventAdminService.registerRiddle(groupId, question, answer, nextMsg);
        return "redirect:/admin/riddles";
    }

    // 5. 編集画面を表示
    @GetMapping("/riddles/edit/{id}")
    public String editRiddle(@PathVariable Integer id, Model model) {
        if (getLoginGroupId() == null)
            return "redirect:/admin/login";

        Riddle riddle = eventAdminService.getRiddle(id);
        model.addAttribute("riddle", riddle);
        return "admin/riddle_edit";
    }

    // 6. 更新処理
    @PostMapping("/riddles/update")
    public String updateRiddle(@RequestParam Integer id, @RequestParam String question,
            @RequestParam String answer, @RequestParam String nextMsg) {
        if (getLoginGroupId() == null)
            return "redirect:/admin/login";

        eventAdminService.updateRiddle(id, question, answer, nextMsg);
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

    // 8. 設定を保存するPOST処理
    @PostMapping("/riddles/settings")
    public String updateSettings(@RequestParam(required = false) Boolean isRandom) {
        String groupId = getLoginGroupId();
        if (groupId == null)
            return "redirect:/admin/login";

        eventAdminService.updateEventSettings(groupId, isRandom != null);
        return "redirect:/admin/riddles";
    }
}
