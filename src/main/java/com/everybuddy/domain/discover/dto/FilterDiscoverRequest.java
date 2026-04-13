package com.everybuddy.domain.discover.dto;

import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.Tag;

import java.util.List;

// 다른 Request DTO는 외부 생성을 막기 위해 private 생성자를 사용하지만,
// 이 DTO는 @ModelAttribute 쿼리 파라미터 바인딩을 위해 record를 사용하므로 생성자를 제어할 수 없다.
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
