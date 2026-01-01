package com.gantaro.mysterybot.repository;

import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.gantaro.mysterybot.entity.Player;

@Mapper
public interface PlayerRepository {
    // LINE ID と グループID でプレイヤーを探す
    Optional<Player> findByLineUserAndGroup(@Param("lineUserId") String lineUserId,
            @Param("groupId") String groupId);

    // 新規プレイヤー登録
    void insert(Player player);

    // ステージを進める（進捗更新）
    void updateProgress(@Param("id") Integer id, @Param("currentStage") Integer currentStage);

    Optional<Player> findByLineUserId(String lineUserId);

    void savePlayer(Player player);



}
