package com.gantaro.mysterybot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.gantaro.mysterybot.dto.MasterRiddleRequest;
import com.gantaro.mysterybot.entity.MasterRiddle;
import com.gantaro.mysterybot.repository.MasterRiddleRepository;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private MasterRiddleRepository masterRiddleRepository;

    @Mock
    private RiddleService riddleService;

    @InjectMocks
    private CatalogService catalogService;

    @Test
    void registerMasterRiddleMapsRequestFields() {
        MasterRiddleRequest request =
                new MasterRiddleRequest("question", "answer", "next", "hint", 10, "category");

        catalogService.registerMasterRiddle(request);

        ArgumentCaptor<MasterRiddle> captor = ArgumentCaptor.forClass(MasterRiddle.class);
        verify(masterRiddleRepository).insert(captor.capture());
        MasterRiddle inserted = captor.getValue();
        assertEquals("question", inserted.getQuestion());
        assertEquals("answer", inserted.getAnswer());
        assertEquals("next", inserted.getNextMsg());
        assertEquals("hint", inserted.getHintMsg());
        assertEquals(10, inserted.getImageId());
        assertEquals("category", inserted.getCategory());
    }

    @Test
    void updateMasterRiddleKeepsExistingImageWhenRequestImageIdIsNull() {
        MasterRiddle existing = masterRiddle(1, 10);
        when(masterRiddleRepository.findById(1)).thenReturn(existing);
        MasterRiddleRequest request =
                new MasterRiddleRequest("question2", "answer2", "next2", "hint2", null,
                        "category2");

        catalogService.updateMasterRiddle(1, request);

        ArgumentCaptor<MasterRiddle> captor = ArgumentCaptor.forClass(MasterRiddle.class);
        verify(masterRiddleRepository).update(captor.capture());
        MasterRiddle updated = captor.getValue();
        assertEquals("question2", updated.getQuestion());
        assertEquals("answer2", updated.getAnswer());
        assertEquals("next2", updated.getNextMsg());
        assertEquals("hint2", updated.getHintMsg());
        assertEquals("category2", updated.getCategory());
        assertEquals(10, updated.getImageId());
    }

    @Test
    void updateMasterRiddleReplacesImageWhenRequestImageIdExists() {
        MasterRiddle existing = masterRiddle(1, 10);
        when(masterRiddleRepository.findById(1)).thenReturn(existing);
        MasterRiddleRequest request =
                new MasterRiddleRequest("question2", "answer2", "next2", "hint2", 20,
                        "category2");

        catalogService.updateMasterRiddle(1, request);

        ArgumentCaptor<MasterRiddle> captor = ArgumentCaptor.forClass(MasterRiddle.class);
        verify(masterRiddleRepository).update(captor.capture());
        assertEquals(20, captor.getValue().getImageId());
    }

    private MasterRiddle masterRiddle(Integer id, Integer imageId) {
        MasterRiddle riddle = new MasterRiddle();
        riddle.setId(id);
        riddle.setQuestion("question");
        riddle.setAnswer("answer");
        riddle.setNextMsg("next");
        riddle.setHintMsg("hint");
        riddle.setCategory("category");
        riddle.setImageId(imageId);
        return riddle;
    }
}
