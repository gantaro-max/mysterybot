package com.gantaro.mysterybot.repository;

import java.time.LocalDateTime;
import java.util.List;
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

    void updateNameAndStart(@Param("id") Integer id, @Param("playerName") String playerName,
            @Param("startAt") LocalDateTime startAt);

    void updateCurrentRiddleId(@Param("id") Integer id,
            @Param("nextRiddleId") Integer nextRiddleId);

    void updateFinishedAt(@Param("id") Integer id, @Param("finishedAt") LocalDateTime finishedAt);

    List<Player> findRankingByGroup(@Param("groupId") String groupId);

    // プレイヤー削除
    void deleteById(Integer id);



}
