package com.gantaro.mysterybot.repository;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.gantaro.mysterybot.entity.MasterRiddle;

@Mapper
public interface MasterRiddleRepository {

    List<MasterRiddle> findAll();

    MasterRiddle findById(Integer id);

    void insert(MasterRiddle masterRiddle);

    void update(MasterRiddle masterRiddle);

    void delete(Integer id);

}
