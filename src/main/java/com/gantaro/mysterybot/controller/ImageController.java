package com.gantaro.mysterybot.controller;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.gantaro.mysterybot.entity.RiddleImage;
import com.gantaro.mysterybot.repository.RiddleImageRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ImageController {
    private final RiddleImageRepository riddleImageRepository;

    @GetMapping("/public/image/{uuid}")
    public ResponseEntity<byte[]> getImage(@PathVariable("uuid") String uuid) {
        Optional<RiddleImage> riddleImage = riddleImageRepository.findByUuid(uuid);
        if (riddleImage.isEmpty())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(riddleImage.get().getMimeType()))
                .body(riddleImage.get().getData());
    }

}
