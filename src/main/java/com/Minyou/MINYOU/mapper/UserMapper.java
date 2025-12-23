package com.Minyou.MINYOU.mapper;

import com.Minyou.MINYOU.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    List<User> findAll();
    User findById(Long id);
    User findByEmail(String email);
    boolean existsByEmail(String email);
    User findByUsername(String username);
    boolean existsByUsername(String username);
    User findByResetToken(String resetToken);
    void insert(User user);
    void update(User user);
    void delete(Long id);
}

