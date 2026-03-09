package com.everybuddy.domain.friendrelation.service;

import com.everybuddy.domain.friendrelation.entity.BlockRelation;
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
public class BlockRelationService {

    private final BlockRelationRepository blockRelationRepository;
    private final FriendRelationRepository friendRelationRepository;
    private final UserRepository userRepository;

    public void block(Long blockerUserId, Long blockedUserId) {
        if (blockerUserId.equals(blockedUserId)) {
            throw new CustomException(ErrorCode.CANNOT_BLOCK_SELF);
        }

        User blockerUser = findUser(blockerUserId);
        User blockedUser = findActiveUser(blockedUserId);

        if (blockRelationRepository.existsBlock(blockerUserId, blockedUserId)) {
            throw new CustomException(ErrorCode.ALREADY_BLOCKED);
        }

        friendRelationRepository.deleteFriendRelationBetween(blockerUserId, blockedUserId);
        blockRelationRepository.save(BlockRelation.of(blockerUser, blockedUser));
    }

    public void unblock(Long blockerUserId, Long blockedUserId) {
        if (!blockRelationRepository.existsBlock(blockerUserId, blockedUserId)) {
            throw new CustomException(ErrorCode.BLOCK_NOT_FOUND);
        }

        blockRelationRepository.deleteBlock(blockerUserId, blockedUserId);
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
