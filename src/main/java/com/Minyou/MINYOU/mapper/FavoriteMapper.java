package com.Minyou.MINYOU.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoriteMapper {
    void insert(@Param("userId") Long userId, @Param("listingId") Long listingId);
    void delete(@Param("userId") Long userId, @Param("listingId") Long listingId);
    boolean exists(@Param("userId") Long userId, @Param("listingId") Long listingId);
}

