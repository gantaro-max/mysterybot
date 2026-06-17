package com.gantaro.mysterybot.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.gantaro.mysterybot.entity.Riddle;
import com.gantaro.mysterybot.entity.RiddleImage;
import com.gantaro.mysterybot.repository.RiddleImageRepository;
import com.gantaro.mysterybot.repository.RiddleRepository;
import com.gantaro.mysterybot.repository.SolvedHistoryRepository;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;

@Service
@RequiredArgsConstructor
public class RiddleService {

    private final RiddleRepository riddleRepository;
    private final RiddleImageRepository riddleImageRepository;
    private final SolvedHistoryRepository solvedHistoryRepository;

    private static final long MAX_IMAGE_BYTES = 10L * 1024L * 1024L;
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;
    private static final int MAX_IMAGE_DIMENSION = 10_000;

    public List<Riddle> getRiddles(String groupId) {
        return riddleRepository.findAllByGroup(groupId);
    }

    public Riddle getRiddle(Integer id) {
        return riddleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("謎が見つかりません:ID" + id));
    }

    public Riddle getRiddleOwnedBy(Integer id, String groupId) {
        Riddle riddle = getRiddle(id);
        if (!groupId.equals(riddle.getGroupId())) {
            throw new SecurityException("この謎問題へのアクセス権がありません");
        }
        return riddle;
    }

    @Transactional
    public void registerRiddle(String groupId, String question, String answer, String nextMsg,
            Integer imageId, String hintMsg) {
        Integer nextStage = riddleRepository.countByGroup(groupId) + 1;
        Riddle newRiddle = new Riddle();
        newRiddle.setGroupId(groupId);
        newRiddle.setStageNo(nextStage);
        newRiddle.setQuestion(question);
        newRiddle.setAnswer(answer);
        newRiddle.setNextMsg(nextMsg);
        newRiddle.setImageId(imageId);
        newRiddle.setHintMsg(hintMsg);
        riddleRepository.insert(newRiddle);
    }

    @Transactional
    public void updateRiddle(Integer id, String groupId, String question, String answer, String nextMsg,
            String hintMsg, Integer imageId) {
        Riddle resultRiddle = getRiddleOwnedBy(id, groupId);
        resultRiddle.setQuestion(question);
        resultRiddle.setAnswer(answer);
        resultRiddle.setNextMsg(nextMsg);
        resultRiddle.setHintMsg(hintMsg);

        if (imageId != null) {
            resultRiddle.setImageId(imageId);
        }

        riddleRepository.update(resultRiddle);
    }

    @Transactional
    public void deleteRiddle(Integer id, String groupId) {
        getRiddleOwnedBy(id, groupId);

        solvedHistoryRepository.deleteByRiddleId(id);
        riddleRepository.delete(id);
    }

    @Transactional
    public Integer uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty())
            return null;
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("画像ファイルは10MB以下にしてください");
        }

        byte[] originalData = file.getBytes();
        if (!isAllowedImageBytes(originalData)) {
            throw new IllegalArgumentException("画像ファイル（JPEG/PNG/GIF）のみアップロードできます");
        }

        byte[] savedData;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(originalData)) {
            BufferedImage image = ImageIO.read(bis);
            validateImageDimensions(image);
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                Thumbnails.of(image).width(800).outputFormat("jpg")
                    .outputQuality(0.8).toOutputStream(bos);
                savedData = bos.toByteArray();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("画像ファイルを処理できませんでした");
        }

        RiddleImage img = new RiddleImage();
        img.setData(savedData);
        img.setMimeType("image/jpeg");
        img.setUuid(UUID.randomUUID().toString());
        riddleImageRepository.insert(img);

        return img.getId();
    }

    private boolean isAllowedImageBytes(byte[] data) {
        if (data.length < 4)
            return false;
        if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF)
            return true;
        if (data[0] == (byte) 0x89 && data[1] == (byte) 0x50
                && data[2] == (byte) 0x4E && data[3] == (byte) 0x47)
            return true;
        return data[0] == (byte) 0x47 && data[1] == (byte) 0x49
                && data[2] == (byte) 0x46 && data[3] == (byte) 0x38;
    }

    private void validateImageDimensions(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("画像ファイルを処理できませんでした");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        long pixels = (long) width * height;
        if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION
                || pixels > MAX_IMAGE_PIXELS) {
            throw new IllegalArgumentException("画像サイズが大きすぎます");
        }
    }
}
