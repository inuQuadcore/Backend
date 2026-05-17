package com.everybuddy.domain.discover.service;

import com.everybuddy.domain.discover.dto.DiscoveredUserResponse;
import com.everybuddy.domain.discover.dto.FilterDiscoverRequest;
import com.everybuddy.domain.discover.dto.FilterDiscoverResponse;
import com.everybuddy.domain.discover.dto.RandomDiscoverResponse;
import com.everybuddy.domain.friendrelation.repository.BlockRelationRepository;
import com.everybuddy.domain.friendrelation.repository.FriendRelationRepository;
import com.everybuddy.domain.user.dto.UserLanguageResponse;
import com.everybuddy.domain.user.dto.UserTagResponse;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.entity.UserLanguage;
import com.everybuddy.domain.user.entity.UserPresence;
import com.everybuddy.domain.user.entity.UserTag;
import com.everybuddy.domain.user.repository.UserPresenceRepository;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.domain.user.service.UserProfileLoader;
import com.everybuddy.global.s3.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DiscoverService {

    private final UserRepository userRepository;
    private final UserPresenceRepository userPresenceRepository;
    private final FriendRelationRepository friendRelationRepository;
    private final BlockRelationRepository blockRelationRepository;
    private final UserProfileLoader userProfileLoader;
    private final StorageService storageService;

    public RandomDiscoverResponse getRandomUsers(Long userId) {
        List<User> users = userRepository.findRandomUsers(buildExcludeIds(userId));
        return RandomDiscoverResponse.of(toDiscoveredUserResponses(users));
    }

    public FilterDiscoverResponse getFilteredUsers(Long userId, FilterDiscoverRequest request) {
        List<User> users = userRepository.findFilteredUsers(
                buildExcludeIds(userId),
                request.gender(),
                request.country(),
                maxBirthday(request.minAge()),
                minBirthday(request.maxAge()),
                hasFilter(request.languages()),
                request.languages(),
                hasFilter(request.tags()),
                request.tags(),
                request.isOnline(),
                request.recentlyActive(),
                cutoff(request.recentlyActive()),
                request.lastUserId(),
                PageRequest.of(0, request.size() + 1)
        );

        return toPagedResponse(users, request.size());
    }

    private List<Long> buildExcludeIds(Long userId) {
        List<Long> excludeIds = new ArrayList<>();
        excludeIds.add(userId);
        excludeIds.addAll(friendRelationRepository.findFriendIds(userId));
        excludeIds.addAll(blockRelationRepository.findBlockRelatedUserIds(userId));
        return excludeIds;
    }

    private List<DiscoveredUserResponse> toDiscoveredUserResponses(List<User> users) {
        List<Long> userIds = users.stream().map(User::getUserId).toList();

        Map<Long, List<UserLanguage>> languagesByUserId = userProfileLoader.loadLanguagesByUserId(userIds);
        Map<Long, List<UserTag>> tagsByUserId = userProfileLoader.loadTagsByUserId(userIds);
        Map<Long, UserPresence> presenceByUserId = userPresenceRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserPresence::getUserId, p -> p));

        return users.stream()
                .map(user -> DiscoveredUserResponse.of(
                        user,
                        resolveProfileImageUrl(user.getProfile()),
                        languagesByUserId.getOrDefault(user.getUserId(), List.of()).stream()
                                .map(UserLanguageResponse::from).toList(),
                        tagsByUserId.getOrDefault(user.getUserId(), List.of()).stream()
                                .map(UserTagResponse::from).toList(),
                        presenceByUserId.containsKey(user.getUserId())
                                ? presenceByUserId.get(user.getUserId()).getLastSeenAt()
                                : null
                ))
                .toList();
    }

    private FilterDiscoverResponse toPagedResponse(List<User> users, int size) {
        boolean hasNext = users.size() > size;
        List<User> content = hasNext ? users.subList(0, size) : users;
        Long nextCursor = content.isEmpty() ? null : content.getLast().getUserId();
        return FilterDiscoverResponse.of(toDiscoveredUserResponses(content), hasNext, nextCursor);
    }

    private <T> boolean hasFilter(List<T> list) {
        return !list.isEmpty();
    }

    private LocalDate maxBirthday(Integer minAge) {
        return minAge != null ? LocalDate.now().minusYears(minAge) : null;
    }

    private LocalDate minBirthday(Integer maxAge) {
        return maxAge != null ? LocalDate.now().minusYears(maxAge + 1L).plusDays(1) : null;
    }

    private LocalDateTime cutoff(boolean recentlyActive) {
        return recentlyActive ? LocalDateTime.now().minusHours(24) : null;
    }

    private String resolveProfileImageUrl(String profileKey) {
        return profileKey != null ? storageService.getPresignedUrl(profileKey) : null;
    }
}
