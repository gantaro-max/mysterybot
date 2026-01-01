package com.gantaro.mysterybot.repository;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.gantaro.mysterybot.entity.Riddle;

@Mapper
public interface RiddleRepository {
    // 特定のグループの、指定したステージの問題を取得する
    Optional<Riddle> findByGroupIdAndStageNo(@Param("groupId") String groupId,
            @Param("stageNo") Integer stageNo);

    // そのグループに全部で何問あるか数える（クリア判定用）
    int countByGroup(String groupId);

    // 全件取得（管理者用など）
    List<Riddle> findAllByGroup(String groupId);

}
