package com.gantaro.mysterybot.service;

import java.util.Optional;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gantaro.mysterybot.entity.TeamGroup;
import com.gantaro.mysterybot.repository.TeamGroupRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TeamGroupRepository teamGroupRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> RESERVED_GROUP_IDS =
            Set.of("admin", "system", "root", "superadmin", "test");

    @Transactional
    public boolean login(String groupId, String password) {
        Optional<TeamGroup> group = teamGroupRepository.findByGroupId(groupId);
        if (group.isEmpty())
            return false;
        String savedPass = group.get().getAdminPass();
        if (savedPass == null)
            return false;
        if (isBCryptHash(savedPass)) {
            return passwordEncoder.matches(password, savedPass);
        }
        if (!savedPass.equals(password)) {
            return false;
        }
        teamGroupRepository.updateAdminPass(groupId, passwordEncoder.encode(password));
        return true;
    }

    @Transactional
    public void createEvent(String groupId, String groupName, String password) {
        if (RESERVED_GROUP_IDS.contains(groupId.toLowerCase())) {
            throw new IllegalArgumentException("そのイベントIDは使用できません");
        }
        if (!groupId.matches("^[a-zA-Z0-9_-]{3,30}$")) {
            throw new IllegalArgumentException("イベントIDは半角英数字・ハイフン・アンダーバーのみ、3〜30文字で入力してください");
        }
        if (teamGroupRepository.findByGroupId(groupId).isPresent()) {
            throw new IllegalArgumentException("そのイベントIDは既に使用されています");
        }
        TeamGroup newGroup = new TeamGroup();
        newGroup.setGroupId(groupId);
        newGroup.setGroupName(groupName);
        newGroup.setAdminPass(passwordEncoder.encode(password));
        newGroup.setIsRandomOrder(false);
        teamGroupRepository.insert(newGroup);
    }

    private boolean isBCryptHash(String value) {
        return value.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }
}
