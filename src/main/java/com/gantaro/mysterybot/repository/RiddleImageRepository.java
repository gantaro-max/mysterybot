package com.gantaro.mysterybot.repository;

import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import com.gantaro.mysterybot.entity.RiddleImage;

@Mapper
public interface RiddleImageRepository {

    void insert(RiddleImage image);

    Optional<RiddleImage> findById(Integer id);

    Optional<RiddleImage> findByUuid(String uuid);

}
