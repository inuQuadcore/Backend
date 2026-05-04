package com.everybuddy.domain.friendrelation.event;

import com.everybuddy.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FriendAddedEvent {

    private final User fromUser;
    private final User toUser;

    @Builder
    private FriendAddedEvent(User fromUser, User toUser) {
        this.fromUser = fromUser;
        this.toUser = toUser;
    }

    public static FriendAddedEvent of(User fromUser, User toUser) {
        return FriendAddedEvent.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .build();
    }
}
