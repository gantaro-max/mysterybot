package com.gantaro.mysterybot.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    public void registerMasterRiddle(String q, String a, String n, String h, Integer imgId,
            String cat) {
        MasterRiddle mr = new MasterRiddle();
        mr.setQuestion(q);
        mr.setAnswer(a);
        mr.setNextMsg(n);
        mr.setHintMsg(h);
        mr.setImageId(imgId);
        mr.setCategory(cat);
        masterRiddleRepository.insert(mr);
    }

    public MasterRiddle getMasterRiddle(Integer id) {
        return masterRiddleRepository.findById(id);
    }

    @Transactional
    public void updateMasterRiddle(Integer id, String question, String answer, String nextMsg,
            String hintMsg, Integer imageId, String category) {

        MasterRiddle mr = masterRiddleRepository.findById(id);
        if (mr == null)
            return;

        mr.setQuestion(question);
        mr.setAnswer(answer);
        mr.setNextMsg(nextMsg);
        mr.setHintMsg(hintMsg);
        mr.setCategory(category);

        if (imageId != null) {
            mr.setImageId(imageId);
        }

        masterRiddleRepository.update(mr);
    }

    @Transactional
    public void deleteMasterRiddle(Integer id) {
        masterRiddleRepository.delete(id);
    }
}
