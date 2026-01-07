package com.gantaro.mysterybot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.gantaro.mysterybot.service.EventAdminService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AuthController {

    private final EventAdminService eventAdminService;
    private final HttpSession session;

    // ログイン画面
    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    // ログイン処理
    @PostMapping("/login")
    public String login(@RequestParam String groupId, @RequestParam String password, Model model) {
        if (eventAdminService.login(groupId, password)) {
            session.setAttribute("loginGroupId", groupId);
            return "redirect:/admin/dashboard";
        } else {
            model.addAttribute("error", "IDまたはパスワードが違います");
            return "admin/login";
        }
    }

    // ログアウト処理
    @GetMapping("/logout")
    public String logout() {
        session.invalidate();
        return "redirect:/admin/login";
    }

    // 新規登録画面
    @GetMapping("/register")
    public String registerPage() {
        return "admin/register";
    }

    // 新規登録処理
    @PostMapping("/register")
    public String register(@RequestParam String groupId, @RequestParam String groupName,
            @RequestParam String password, Model model) {
        try {
            eventAdminService.createEvent(groupId, groupName, password);
            // 登録成功したらそのままログイン
            session.setAttribute("loginGroupId", groupId);
            return "redirect:/admin/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "登録失敗: IDが重複している可能性があります");
            return "admin/register";
        }
    }
}
