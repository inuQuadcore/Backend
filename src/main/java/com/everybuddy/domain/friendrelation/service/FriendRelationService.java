package com.everybuddy.domain.friendrelation.service;

import com.everybuddy.domain.friendrelation.entity.FriendRelation;
import com.everybuddy.domain.friendrelation.repository.BlockRelationRepository;
import com.everybuddy.domain.friendrelation.repository.FriendRelationRepository;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class FriendRelationService {

    private final FriendRelationRepository friendRelationRepository;
    private final BlockRelationRepository blockRelationRepository;
    private final UserRepository userRepository;

    public void addFriend(Long fromUserId, Long toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new CustomException(ErrorCode.CANNOT_ADD_SELF);
        }

        User fromUser = findUser(fromUserId);
        User toUser = findActiveUser(toUserId);

        if (blockRelationRepository.existsBlockRelationBetween(fromUserId, toUserId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        if (friendRelationRepository.existsFriendRelationBetween(fromUserId, toUserId)) {
            throw new CustomException(ErrorCode.ALREADY_FRIEND);
        }

        friendRelationRepository.save(FriendRelation.of(fromUser, toUser));
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.USER_DELETED);
        }
        return user;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
