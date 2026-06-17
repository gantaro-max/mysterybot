package com.gantaro.mysterybot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import com.gantaro.mysterybot.entity.Riddle;
import com.gantaro.mysterybot.entity.RiddleImage;
import com.gantaro.mysterybot.repository.RiddleImageRepository;
import com.gantaro.mysterybot.repository.RiddleRepository;
import com.gantaro.mysterybot.repository.SolvedHistoryRepository;

@ExtendWith(MockitoExtension.class)
class RiddleServiceTest {

    @Mock
    private RiddleRepository riddleRepository;

    @Mock
    private RiddleImageRepository riddleImageRepository;

    @Mock
    private SolvedHistoryRepository solvedHistoryRepository;

    @InjectMocks
    private RiddleService riddleService;

    @Test
    void updateRiddleKeepsExistingImageWhenNoFileProvided() throws Exception {
        Riddle existing = riddle(1, "event1", 99);
        when(riddleRepository.findById(1)).thenReturn(Optional.of(existing));

        riddleService.updateRiddle(1, "event1", "question2", "answer2", "next2", "hint2",
                null);

        ArgumentCaptor<Riddle> captor = ArgumentCaptor.forClass(Riddle.class);
        verify(riddleRepository).update(captor.capture());
        Riddle updated = captor.getValue();
        assertEquals("question2", updated.getQuestion());
        assertEquals("answer2", updated.getAnswer());
        assertEquals("next2", updated.getNextMsg());
        assertEquals("hint2", updated.getHintMsg());
        assertEquals(99, updated.getImageId());
    }

    @Test
    void updateRiddleReplacesImageWhenFileProvided() throws Exception {
        Riddle existing = riddle(1, "event1", 99);
        when(riddleRepository.findById(1)).thenReturn(Optional.of(existing));
        org.mockito.Mockito.doAnswer(invocation -> {
            RiddleImage image = invocation.getArgument(0);
            image.setId(123);
            return null;
        }).when(riddleImageRepository).insert(any(RiddleImage.class));

        riddleService.updateRiddle(1, "event1", "question2", "answer2", "next2", "hint2",
                pngFile());

        ArgumentCaptor<Riddle> captor = ArgumentCaptor.forClass(Riddle.class);
        verify(riddleRepository).update(captor.capture());
        assertEquals(123, captor.getValue().getImageId());
    }

    @Test
    void updateRiddleRejectsDifferentOwnerWithoutUpdating() {
        Riddle existing = riddle(1, "event1", 99);
        when(riddleRepository.findById(1)).thenReturn(Optional.of(existing));

        assertThrows(SecurityException.class,
                () -> riddleService.updateRiddle(1, "event2", "question2", "answer2",
                        "next2", "hint2", null));

        verify(riddleRepository, never()).update(any(Riddle.class));
    }

    private Riddle riddle(Integer id, String groupId, Integer imageId) {
        Riddle riddle = new Riddle();
        riddle.setId(id);
        riddle.setGroupId(groupId);
        riddle.setQuestion("question");
        riddle.setAnswer("answer");
        riddle.setNextMsg("next");
        riddle.setHintMsg("hint");
        riddle.setImageId(imageId);
        return riddle;
    }

    private MockMultipartFile pngFile() {
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=");
        return new MockMultipartFile("imageFile", "tiny.png", "image/png", png);
    }
}
