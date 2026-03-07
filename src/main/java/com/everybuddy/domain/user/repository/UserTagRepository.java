package com.everybuddy.domain.user.repository;

import com.everybuddy.domain.user.entity.UserTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserTagRepository extends JpaRepository<UserTag, Long> {

    @Modifying
    @Query("DELETE FROM UserTag ut WHERE ut.user.userId = :userId")
    void deleteAllTagsByUserId(@Param("userId") Long userId);

    @Query("SELECT ut FROM UserTag ut WHERE ut.user.userId = :userId")
    List<UserTag> findAllByUserId(@Param("userId") Long userId);
}
