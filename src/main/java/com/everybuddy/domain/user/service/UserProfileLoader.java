package com.everybuddy.domain.user.service;

import com.everybuddy.domain.user.entity.UserLanguage;
import com.everybuddy.domain.user.entity.UserTag;
import com.everybuddy.domain.user.repository.UserLanguageRepository;
import com.everybuddy.domain.user.repository.UserTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileLoader {

    private final UserLanguageRepository userLanguageRepository;
    private final UserTagRepository userTagRepository;

    public Map<Long, List<UserLanguage>> loadLanguagesByUserId(List<Long> userIds) {
        return userLanguageRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(ul -> ul.getUser().getUserId()));
    }

    public Map<Long, List<UserTag>> loadTagsByUserId(List<Long> userIds) {
        return userTagRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(ut -> ut.getUser().getUserId()));
    }
}
