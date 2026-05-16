package com.everybuddy.domain.friendrelation.service;

import com.everybuddy.domain.friendrelation.dto.FriendListResponse;
import com.everybuddy.domain.friendrelation.dto.FriendResponse;
import com.everybuddy.domain.friendrelation.entity.FriendRelation;
import com.everybuddy.domain.friendrelation.event.FriendAddedEvent;
import com.everybuddy.domain.friendrelation.repository.BlockRelationRepository;
import com.everybuddy.domain.friendrelation.repository.FriendRelationRepository;
import com.everybuddy.domain.user.dto.UserLanguageResponse;
import com.everybuddy.domain.user.dto.UserTagResponse;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.entity.UserLanguage;
import com.everybuddy.domain.user.entity.UserTag;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.domain.user.service.UserProfileLoader;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.s3.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class FriendRelationService {

    private final FriendRelationRepository friendRelationRepository;
    private final BlockRelationRepository blockRelationRepository;
    private final UserRepository userRepository;
    private final UserProfileLoader userProfileLoader;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

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
        eventPublisher.publishEvent(FriendAddedEvent.of(fromUser, toUser));
    }

    @Transactional(readOnly = true)
    public FriendListResponse getFriends(Long userId) {
        findUser(userId);

        List<FriendRelation> relations = friendRelationRepository.findAllActiveFriends(userId);

        return FriendListResponse.of(toFriendResponses(relations, userId));
    }

    private List<FriendResponse> toFriendResponses(List<FriendRelation> page, Long userId) {
        List<User> friends = extractFriends(page, userId);
        List<Long> friendIds = toUserIds(friends);

        Map<Long, List<UserLanguage>> languagesByUserId = userProfileLoader.loadLanguagesByUserId(friendIds);
        Map<Long, List<UserTag>> tagsByUserId = userProfileLoader.loadTagsByUserId(friendIds);

        return friends.stream()
                .map(friend -> FriendResponse.of(
                        friend,
                        resolveProfileImageUrl(friend.getProfile()),
                        languagesByUserId.getOrDefault(friend.getUserId(), List.of()).stream().map(UserLanguageResponse::from).toList(),
                        tagsByUserId.getOrDefault(friend.getUserId(), List.of()).stream().map(UserTagResponse::from).toList()
                ))
                .toList();
    }

    private List<User> extractFriends(List<FriendRelation> relations, Long userId) {
        return relations.stream()
                .map(fr -> fr.getFromUser().getUserId().equals(userId) ? fr.getToUser() : fr.getFromUser())
                .toList();
    }

    private List<Long> toUserIds(List<User> users) {
        return users.stream().map(User::getUserId).toList();
    }

    private String resolveProfileImageUrl(String profileKey) {
        return profileKey != null ? storageService.getPublicUrl(profileKey) : null;
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
