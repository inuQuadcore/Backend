package com.everybuddy.domain.friendrelation.entity;

import com.everybuddy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "block_relation")
public class BlockRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long blockRelationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_user_id")
    private User blockerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_user_id")
    private User blockedUser;

    public static BlockRelation of(User blockerUser, User blockedUser) {
        BlockRelation relation = new BlockRelation();
        relation.blockerUser = blockerUser;
        relation.blockedUser = blockedUser;
        return relation;
    }
}
