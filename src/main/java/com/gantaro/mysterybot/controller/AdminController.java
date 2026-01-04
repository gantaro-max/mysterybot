package com.gantaro.mysterybot.controller;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.gantaro.mysterybot.entity.Riddle;
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

    // 4. 謎（問題）の一覧＆登録画面を表示
    @GetMapping("/riddles/{groupId}")
    public String rddleList(@PathVariable String groupId, Model model) {

        List<Riddle> rddles = gameService.getRiddles(groupId);

        model.addAttribute("groupId", groupId);
        model.addAttribute("riddles", rddles);

        return "admin/riddles";
    }

    // 5. 謎（問題）の登録処理
    @PostMapping("/riddles/add")
    public String addRiddle(@RequestParam String groupId, @RequestParam String question,
            @RequestParam String answer, @RequestParam String nextMsg) {

        gameService.registerRiddle(groupId, question, answer, nextMsg);

        return "redirect:/admin/riddles/" + groupId;

    }

    // 6. 編集画面を表示
    @GetMapping("/riddles/edit/{id}")
    public String editRiddle(@PathVariable Integer id, Model model) {
        Riddle riddle = gameService.getRiddle(id);
        model.addAttribute("riddle", riddle);
        return "admin/riddle_edit";
    }

    // 7. 更新処理を実行
    @PostMapping("/riddles/update")
    public String updateRiddle(@RequestParam Integer id, @RequestParam String question,
            @RequestParam String answer, @RequestParam String nextMsg) {
        Riddle original = gameService.getRiddle(id);

        gameService.updateRiddle(id, question, answer, nextMsg);

        return "redirect:/admin/riddles/" + original.getGroupId();
    }

    // 8. 削除処理を実行
    @PostMapping("/riddles/delete/{id}")
    public String deleteRiddle(@PathVariable Integer id) {
        Riddle original = gameService.getRiddle(id);
        gameService.deleteRiddle(id);

        return "redirect:/admin/riddles/" + original.getGroupId();
    }
}
