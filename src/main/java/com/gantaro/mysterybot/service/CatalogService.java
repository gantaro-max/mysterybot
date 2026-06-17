package com.gantaro.mysterybot.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gantaro.mysterybot.dto.MasterRiddleRequest;
import com.gantaro.mysterybot.entity.MasterRiddle;
import com.gantaro.mysterybot.repository.MasterRiddleRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final MasterRiddleRepository masterRiddleRepository;
    private final RiddleService riddleService;

    public List<MasterRiddle> getCatalog() {
        return masterRiddleRepository.findAll();
    }

    @Transactional
    public void importFromCatalog(String groupId, Integer masterRiddleId) {
        MasterRiddle m = masterRiddleRepository.findById(masterRiddleId);
        if (m == null)
            return;
        riddleService.registerRiddle(groupId, m.getQuestion(), m.getAnswer(), m.getNextMsg(),
                m.getImageId(), m.getHintMsg());
    }

    @Transactional
    public void registerMasterRiddle(MasterRiddleRequest req) {
        MasterRiddle mr = new MasterRiddle();
        mr.setQuestion(req.question());
        mr.setAnswer(req.answer());
        mr.setNextMsg(req.nextMsg());
        mr.setHintMsg(req.hintMsg());
        mr.setImageId(req.imageId());
        mr.setCategory(req.category());
        masterRiddleRepository.insert(mr);
    }

    public MasterRiddle getMasterRiddle(Integer id) {
        return masterRiddleRepository.findById(id);
    }

    @Transactional
    public void updateMasterRiddle(Integer id, MasterRiddleRequest req) {

        MasterRiddle mr = masterRiddleRepository.findById(id);
        if (mr == null)
            return;

        mr.setQuestion(req.question());
        mr.setAnswer(req.answer());
        mr.setNextMsg(req.nextMsg());
        mr.setHintMsg(req.hintMsg());
        mr.setCategory(req.category());

        if (req.imageId() != null) {
            mr.setImageId(req.imageId());
        }

        masterRiddleRepository.update(mr);
    }

    @Transactional
    public void deleteMasterRiddle(Integer id) {
        masterRiddleRepository.delete(id);
    }
}
