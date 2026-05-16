package com.everybuddy.domain.user.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("User 엔티티 단위 테스트")
class UserTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 16);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final LocalDate TWO_DAYS_AGO = TODAY.minusDays(2);

    private User user;

    @BeforeEach
    void setUp() {
        user = User.createForTest(1L, "user1", "홍길동", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));
    }

    @Nested
    @DisplayName("1. recordAttendance()")
    class RecordAttendanceCases {

        @Test
        @DisplayName("TC-1-1. 첫 출석 (lastAttendanceDate=null) → consecutiveDays=1, lastAttendanceDate=today")
        void firstAttendance() {
            // when
            user.recordAttendance(TODAY);

            // then
            assertAll(
                    () -> assertEquals(1, user.getConsecutiveDays()),
                    () -> assertEquals(TODAY, user.getLastAttendanceDate())
            );
        }

        @Test
        @DisplayName("TC-1-2. 어제 출석 → consecutiveDays+1, lastAttendanceDate=today")
        void attendedYesterday() {
            // given
            user.recordAttendance(YESTERDAY);

            // when
            user.recordAttendance(TODAY);

            // then
            assertAll(
                    () -> assertEquals(2, user.getConsecutiveDays()),
                    () -> assertEquals(TODAY, user.getLastAttendanceDate())
            );
        }

        @Test
        @DisplayName("TC-1-3. 오늘 이미 출석 → no-op (값 변경 없음)")
        void alreadyAttendedToday() {
            // given
            user.recordAttendance(YESTERDAY);
            user.recordAttendance(TODAY);

            // when
            user.recordAttendance(TODAY);

            // then
            assertAll(
                    () -> assertEquals(2, user.getConsecutiveDays()),
                    () -> assertEquals(TODAY, user.getLastAttendanceDate())
            );
        }

        @Test
        @DisplayName("TC-1-4. 이틀 이상 끊김 → consecutiveDays=1로 리셋")
        void streakBroken() {
            // given
            user.recordAttendance(TWO_DAYS_AGO);

            // when
            user.recordAttendance(TODAY);

            // then
            assertAll(
                    () -> assertEquals(1, user.getConsecutiveDays()),
                    () -> assertEquals(TODAY, user.getLastAttendanceDate())
            );
        }
    }

    @Nested
    @DisplayName("2. getCurrentConsecutiveDays()")
    class GetCurrentConsecutiveDaysCases {

        @Test
        @DisplayName("TC-2-1. lastAttendanceDate=null → 0")
        void noAttendance() {
            assertEquals(0, user.getCurrentConsecutiveDays(TODAY));
        }

        @Test
        @DisplayName("TC-2-2. 오늘 출석 → 저장된 consecutiveDays 반환")
        void attendedToday() {
            // given
            user.recordAttendance(YESTERDAY);
            user.recordAttendance(TODAY);

            // when & then
            assertEquals(2, user.getCurrentConsecutiveDays(TODAY));
        }

        @Test
        @DisplayName("TC-2-3. 어제 출석, 오늘 아직 → 저장된 consecutiveDays 반환 (끊긴 게 아님)")
        void attendedYesterdayNotYetToday() {
            // given
            user.recordAttendance(YESTERDAY);

            // when & then
            assertEquals(1, user.getCurrentConsecutiveDays(TODAY));
        }

        @Test
        @DisplayName("TC-2-4. 이틀 이상 전 출석 → 0 (끊김)")
        void streakBroken() {
            // given
            user.recordAttendance(TWO_DAYS_AGO);

            // when & then
            assertEquals(0, user.getCurrentConsecutiveDays(TODAY));
        }
    }
}
