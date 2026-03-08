package com.everybuddy.domain.friendrelation.repository;

import com.everybuddy.domain.friendrelation.entity.FriendRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendRelationRepository extends JpaRepository<FriendRelation, Long> {

    @Query("SELECT CASE WHEN COUNT(fr) > 0 THEN true ELSE false END FROM FriendRelation fr " +
            "WHERE (fr.fromUser.userId = :userAId AND fr.toUser.userId = :userBId) " +
            "OR (fr.fromUser.userId = :userBId AND fr.toUser.userId = :userAId)")
    boolean existsFriendRelationBetween(@Param("userAId") Long userAId, @Param("userBId") Long userBId);
}
