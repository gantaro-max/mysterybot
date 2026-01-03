package com.gantaro.mysterybot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.gantaro.mysterybot.service.GameService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final GameService gameService;

    // 1. 管理画面トップ（一覧表示）
    @GetMapping
    public String index(Model model) {
        // DBから全イベントを取得して画面に渡す
        model.addAttribute("events", gameService.getAllEvents());
        return "admin/index";
    }

    // 2. 作成フォーム表示
    @GetMapping("/create")
    public String createForm() {
        return "admin/create";
    }

    // 3. 登録処理
    @PostMapping("/create")
    public String create(@RequestParam String eventId, @RequestParam String eventName) {
        try {
            gameService.createEvent(eventId, eventName);
        } catch (Exception e) {
            // エラーがあったらログに出す（本来は画面にエラーメッセージを出すべきですが簡易的に）
            System.out.println("登録エラー: " + e.getMessage());
            return "redirect:/admin/create?error";
        }
        return "redirect:/admin";
    }
}
