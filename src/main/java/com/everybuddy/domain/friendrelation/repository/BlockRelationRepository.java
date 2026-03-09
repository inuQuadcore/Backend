package com.everybuddy.domain.friendrelation.repository;

import com.everybuddy.domain.friendrelation.entity.BlockRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlockRelationRepository extends JpaRepository<BlockRelation, Long> {

    @Query("SELECT CASE WHEN COUNT(br) > 0 THEN true ELSE false END FROM BlockRelation br " +
            "WHERE br.blockerUser.userId = :blockerUserId AND br.blockedUser.userId = " +
            ":blockedUserId")
    boolean existsBlock(@Param("blockerUserId") Long blockerUserId, @Param("blockedUserId") Long blockedUserId);

    @Modifying
    @Query("DELETE FROM BlockRelation br " +
            "WHERE br.blockerUser.userId = :blockerUserId AND br.blockedUser.userId = :blockedUserId")
    void deleteBlock(@Param("blockerUserId") Long blockerUserId, @Param("blockedUserId") Long blockedUserId);

    @Query("SELECT CASE WHEN COUNT(br) > 0 THEN true ELSE false END FROM BlockRelation br " +
            "WHERE (br.blockerUser.userId = :userAId AND br.blockedUser.userId = :userBId) " +
            "OR (br.blockerUser.userId = :userBId AND br.blockedUser.userId = :userAId)")
    boolean existsBlockRelationBetween(@Param("userAId") Long userAId, @Param("userBId") Long userBId);
}
