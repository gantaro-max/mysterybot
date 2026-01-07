package com.gantaro.mysterybot.repository;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SolvedHistoryRepository {
    // プレイヤーがクリア済みの問題IDリストを取得
    List<Integer> findSolvedRiddleIds(Integer playerId);

    // 履歴を追加
    void insert(@Param("playerId") Integer playerId, @Param("riddleId") Integer riddleId);

    // Playerのクリア履歴を全削除
    void deleteByPlayerId(Integer playerId);
}
