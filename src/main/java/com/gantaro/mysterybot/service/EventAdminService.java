package com.gantaro.mysterybot.service;

import java.sql.Timestamp;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gantaro.mysterybot.entity.Player;
import com.gantaro.mysterybot.entity.TeamGroup;
import com.gantaro.mysterybot.repository.PlayerRepository;
import com.gantaro.mysterybot.repository.RiddleRepository;
import com.gantaro.mysterybot.repository.TeamGroupRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventAdminService {

    private final TeamGroupRepository teamGroupRepository;
    private final PlayerRepository playerRepository;
    private final RiddleRepository riddleRepository;

    public TeamGroup getEvent(String groupId) {
        return teamGroupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new IllegalArgumentException("グループが見つかりません: " + groupId));
    }

    public List<TeamGroup> getAllEvents() {
        return teamGroupRepository.findAll();
    }

    public List<Player> getRanking(String groupId) {
        return playerRepository.findRankingByGroup(groupId);
    }

    @Transactional
    public void startEvent(String groupId) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        teamGroupRepository.updateStartedAt(groupId, now);
    }

    @Transactional
    public void updateEventSettings(String groupId, Boolean isRandom) {
        if (isRandom == null)
            isRandom = false;
        teamGroupRepository.updateRandomMode(groupId, isRandom);
    }

    @Transactional
    public void deleteEvent(String groupId) {
        playerRepository.deleteByGroupId(groupId);
        riddleRepository.deleteByGroupId(groupId);
        teamGroupRepository.delete(groupId);
    }

    @Transactional
    public void resetEventTime(String groupId) {
        teamGroupRepository.updateStartedAt(groupId, null);
    }
}
