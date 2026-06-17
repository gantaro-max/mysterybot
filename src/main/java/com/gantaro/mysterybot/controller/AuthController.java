package com.gantaro.mysterybot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.gantaro.mysterybot.service.EventAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EventAdminService eventAdminService;
    private final HttpSession session;

    // ログイン画面
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    // ログイン処理
    @PostMapping("/login")
    public String login(@RequestParam String groupId, @RequestParam String password, Model model,
            HttpServletRequest request) {
        if (eventAdminService.login(groupId, password)) {
            request.changeSessionId();
            session.removeAttribute("originalAdminId");
            session.setAttribute("loginGroupId", groupId);

            if ("admin".equals(groupId)) {
                return "redirect:/admin/dashboard"; // アプリ管理者へ
            } else {
                return "redirect:/user/dashboard"; // イベント主催者へ
            }
        } else {
            model.addAttribute("error", "IDまたはパスワードが違います");
            return "auth/login";
        }
    }

    // ログアウト処理
    @PostMapping("/logout")
    public String logout() {
        session.invalidate();
        return "redirect:/auth/login";
    }

    // 新規登録画面
    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    // 新規登録処理
    @PostMapping("/register")
    public String register(@RequestParam String groupId, @RequestParam String groupName,
            @RequestParam String password, Model model, HttpServletRequest request) {
        try {
            eventAdminService.createEvent(groupId, groupName, password);
            // 登録成功したらそのままログイン
            request.changeSessionId();
            session.removeAttribute("originalAdminId");
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
}
