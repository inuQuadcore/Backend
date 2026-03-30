package com.everybuddy.domain.discover.dto;

import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.Tag;

import java.util.List;

public record FilterDiscoverRequest(
        Gender gender,
        Country country,
        Integer minAge,
        Integer maxAge,
        List<Language> languages,
        List<Tag> tags,
        boolean isOnline,
        boolean recentlyActive,
        Long lastUserId,
        int size
) {
    public FilterDiscoverRequest {
        if (size == 0) size = 20;
        if (languages == null) languages = List.of();
        if (tags == null) tags = List.of();
    }
}
