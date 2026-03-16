package com.everybuddy.domain.friendrelation.service;

import com.everybuddy.domain.friendrelation.dto.FriendListResponse;
import com.everybuddy.domain.friendrelation.dto.FriendResponse;
import com.everybuddy.domain.friendrelation.entity.FriendRelation;
import com.everybuddy.domain.friendrelation.repository.BlockRelationRepository;
import com.everybuddy.domain.friendrelation.repository.FriendRelationRepository;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.entity.UserLanguage;
import com.everybuddy.domain.user.entity.UserTag;
import com.everybuddy.domain.user.repository.UserLanguageRepository;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.domain.user.repository.UserTagRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.s3.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FriendRelationService {

    private final FriendRelationRepository friendRelationRepository;
    private final BlockRelationRepository blockRelationRepository;
    private final UserRepository userRepository;
    private final UserLanguageRepository userLanguageRepository;
    private final UserTagRepository userTagRepository;
    private final StorageService storageService;

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

    @Transactional(readOnly = true)
    public FriendListResponse getFriends(Long userId) {
        findUser(userId);

        List<FriendRelation> relations = friendRelationRepository.findAllFriends(userId);

        return FriendListResponse.of(toFriendResponses(relations, userId));
    }

    private List<FriendResponse> toFriendResponses(List<FriendRelation> page, Long userId) {
        List<User> friends = extractFriends(page, userId);
        List<Long> friendIds = toUserIds(friends);

        List<UserLanguage> allLanguages = userLanguageRepository.findAllByUserIdIn(friendIds);
        List<UserTag> allTags = userTagRepository.findAllByUserIdIn(friendIds);

        Map<Long, List<UserLanguage>> languagesByUserId = groupByUserId(allLanguages, ul -> ul.getUser().getUserId());
        Map<Long, List<UserTag>> tagsByUserId = groupByUserId(allTags, ut -> ut.getUser().getUserId());

        return friends.stream()
                .map(friend -> FriendResponse.of(
                        friend,
                        resolveProfileImageUrl(friend.getProfile()),
                        languagesByUserId.getOrDefault(friend.getUserId(), List.of()),
                        tagsByUserId.getOrDefault(friend.getUserId(), List.of())
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

    private <T> Map<Long, List<T>> groupByUserId(List<T> items, Function<T, Long> keyExtractor) {
        return items.stream().collect(Collectors.groupingBy(keyExtractor));
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
