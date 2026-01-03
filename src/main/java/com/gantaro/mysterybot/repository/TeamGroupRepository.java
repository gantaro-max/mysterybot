package com.gantaro.mysterybot.repository;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import com.gantaro.mysterybot.entity.TeamGroup;

@Mapper
public interface TeamGroupRepository {

    Optional<TeamGroup> findByGroupId(String groupId);

    List<TeamGroup> findAll();

    void insert(TeamGroup teamGroup);

}
