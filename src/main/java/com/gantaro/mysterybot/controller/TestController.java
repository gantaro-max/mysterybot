package com.gantaro.mysterybot.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.gantaro.mysterybot.entity.Riddle;
import com.gantaro.mysterybot.repository.RiddleRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final RiddleRepository riddleRepository;

    // ブラウザで http://localhost:8080/test/riddles/demo にアクセスすると動く
    @GetMapping("/test/riddles/{groupId}")
    public List<Riddle> getRiddles(@PathVariable String groupId) {
        return riddleRepository.findAllByGroup(groupId);
    }
}
