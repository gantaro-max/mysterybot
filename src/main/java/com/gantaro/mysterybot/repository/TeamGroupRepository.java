package com.gantaro.mysterybot.repository;

import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import com.gantaro.mysterybot.entity.TeamGroup;

@Mapper
public interface TeamGroupRepository {

    Optional<TeamGroup> findByGroupId(String groupId);

}
