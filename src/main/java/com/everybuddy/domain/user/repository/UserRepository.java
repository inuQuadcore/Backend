package com.everybuddy.domain.user.repository;

import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.Tag;
import com.everybuddy.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.loginId = :loginId")
    Optional<User> findByLoginId(@Param("loginId") String loginId);

    boolean existsByLoginId(String loginId);

    @Query("SELECT u FROM User u WHERE u.userId IN :userIds")
    List<User> findAllByUserIdIn(@Param("userIds") List<Long> userIds);

    @Query(value = """
            SELECT u.* FROM user u
            WHERE u.user_id NOT IN (:excludeIds)
            AND u.deleted_at IS NULL
            ORDER BY RAND()
            LIMIT 6
            """, nativeQuery = true)
    List<User> findRandomUsers(@Param("excludeIds") List<Long> excludeIds);

    @Query("""
            SELECT u FROM User u
            WHERE u.userId NOT IN :excludeIds
            AND u.deletedAt IS NULL
            AND (:gender IS NULL OR u.gender = :gender)
            AND (:country IS NULL OR u.country = :country)
            AND (:maxBirthday IS NULL OR u.birthday <= :maxBirthday)
            AND (:minBirthday IS NULL OR u.birthday >= :minBirthday)
            AND (:hasLanguageFilter = false OR EXISTS (SELECT ul FROM UserLanguage ul WHERE ul.user = u AND ul.language IN :languages))
            AND (:hasTagFilter = false OR EXISTS (SELECT ut FROM UserTag ut WHERE ut.user = u AND ut.tag IN :tags))
            AND (:isOnline = false OR EXISTS (SELECT p FROM UserPresence p WHERE p.user = u AND p.isOnline = true))
            AND (:recentlyActive = false OR EXISTS (SELECT p FROM UserPresence p WHERE p.user = u AND p.lastSeenAt >= :cutoff))
            AND (:lastUserId IS NULL OR u.userId > :lastUserId)
            ORDER BY u.userId ASC
            """)
    List<User> findFilteredUsers(
            @Param("excludeIds") List<Long> excludeIds,
            @Param("gender") Gender gender,
            @Param("country") Country country,
            @Param("maxBirthday") LocalDate maxBirthday,
            @Param("minBirthday") LocalDate minBirthday,
            @Param("hasLanguageFilter") boolean hasLanguageFilter,
            @Param("languages") List<Language> languages,
            @Param("hasTagFilter") boolean hasTagFilter,
            @Param("tags") List<Tag> tags,
            @Param("isOnline") boolean isOnline,
            @Param("recentlyActive") boolean recentlyActive,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("lastUserId") Long lastUserId,
            Pageable pageable
    );
}
