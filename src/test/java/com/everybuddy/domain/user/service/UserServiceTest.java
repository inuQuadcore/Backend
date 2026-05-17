package com.everybuddy.domain.user.service;

import com.everybuddy.domain.user.dto.UpdateProfileRequest;
import com.everybuddy.domain.user.dto.UpdateTagsRequest;
import com.everybuddy.domain.user.dto.UserLanguageRequest;
import com.everybuddy.domain.user.dto.UserLanguageResponse;
import com.everybuddy.domain.user.dto.UserLanguagesResponse;
import com.everybuddy.domain.user.dto.UserProfileResponse;
import com.everybuddy.domain.user.dto.UserProfileViewResponse;
import com.everybuddy.domain.user.dto.UserTagResponse;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.Tag;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.entity.UserLanguage;
import com.everybuddy.domain.user.entity.UserTag;
import com.everybuddy.domain.auth.repository.RefreshTokenRepository;
import com.everybuddy.domain.auth.service.FirebaseTokenService;
import com.everybuddy.domain.fcmtoken.repository.FcmTokenRepository;
import com.everybuddy.domain.friendrelation.repository.FriendRelationRepository;
import com.everybuddy.domain.user.repository.UserLanguageRepository;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.domain.user.repository.UserTagRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.s3.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserLanguageRepository userLanguageRepository;
    @Mock private UserTagRepository userTagRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private FcmTokenRepository fcmTokenRepository;
    @Mock private FriendRelationRepository friendRelationRepository;
    @Mock private FirebaseTokenService firebaseTokenService;
    @Mock private StorageService storageService;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<List<UserTag>> userTagsCaptor;

    private User user;
    private User deletedUser;
    private UserLanguage userLanguage;

    @BeforeEach
    void setUp() {
        user = User.createForTest(1L, "user1", "홍길동", "password",
                Country.KOREA, Gender.MALE, LocalDate.of(1990, 1, 1));

        deletedUser = User.createForTest(2L, "deleted", "삭제된유저", "password",
                Country.KOREA, Gender.FEMALE, LocalDate.of(1992, 1, 1));
        deletedUser.softDelete();

        userLanguage = UserLanguage.of(user, Language.ENGLISH, 2, false);
    }

    @Nested
    @DisplayName("1. updateProfile() - 성공 케이스")
    class UpdateProfileSuccessCases {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        }

        @Test
        @DisplayName("TC-1-1. 텍스트 필드만 수정 (이미지 없음)")
        void updateTextFieldsOnly() {
            // given
            UpdateProfileRequest request = UpdateProfileRequest.ofForTest("김철수", "1995-06-15", "FEMALE", "USA", "안녕하세요");

            // when
            UserProfileResponse response = userService.updateProfile(1L, request, null);

            // then
            assertAll(
                    () -> assertEquals(1L, response.getUserId()),
                    () -> assertEquals("김철수", response.getName()),
                    () -> assertEquals(LocalDate.of(1995, 6, 15), response.getBirthday()),
                    () -> assertEquals("FEMALE", response.getGender()),
                    () -> assertEquals("USA", response.getCountry()),
                    () -> assertEquals("안녕하세요", response.getBio()),
                    () -> assertNull(response.getProfileImageUrl())
            );
            verify(storageService, never()).uploadProfileImage(any(), any());
            verify(storageService, never()).deleteFile(any());
        }

        @Test
        @DisplayName("TC-1-2. 일부 필드만 수정 (null 필드는 기존 값 유지)")
        void updatePartialFields() {
            // given
            UpdateProfileRequest request = UpdateProfileRequest.ofForTest("김철수", null, null, null, null);

            // when
            UserProfileResponse response = userService.updateProfile(1L, request, null);

            // then
            assertAll(
                    () -> assertEquals("김철수", response.getName()),
                    () -> assertEquals(LocalDate.of(1990, 1, 1), response.getBirthday()),
                    () -> assertEquals("MALE", response.getGender()),
                    () -> assertEquals("KOREA", response.getCountry())
            );
        }

        @Test
        @DisplayName("TC-1-3. 이미지 포함 수정, 기존 이미지 없음")
        void updateWithNewImageNoExistingImage() {
            // given
            UpdateProfileRequest request = UpdateProfileRequest.ofForTest(null, null, null, null, null);
            MultipartFile profileImage = mock(MultipartFile.class);
            when(profileImage.isEmpty()).thenReturn(false);
            when(storageService.uploadProfileImage(1L, profileImage)).thenReturn("profiles/user-1/new.jpg");
            when(storageService.getPresignedUrl("profiles/user-1/new.jpg")).thenReturn("https://s3.example.com/profiles/user-1/new.jpg");

            // when
            UserProfileResponse response = userService.updateProfile(1L, request, profileImage);

            // then
            assertEquals("https://s3.example.com/profiles/user-1/new.jpg", response.getProfileImageUrl());
            verify(storageService).uploadProfileImage(1L, profileImage);
            verify(storageService, never()).deleteFile(any());
        }

        @Test
        @DisplayName("TC-1-4. 이미지 포함 수정, 기존 이미지 있음 → 기존 이미지 삭제")
        void updateWithNewImageAndExistingImage() {
            // given: 기존 프로필 이미지 세팅
            user.updateProfile(null, null, null, null, null, "profiles/user-1/old.jpg");

            UpdateProfileRequest request = UpdateProfileRequest.ofForTest(null, null, null, null, null);
            MultipartFile profileImage = mock(MultipartFile.class);
            when(profileImage.isEmpty()).thenReturn(false);
            when(storageService.uploadProfileImage(1L, profileImage)).thenReturn("profiles/user-1/new.jpg");
            when(storageService.getPresignedUrl("profiles/user-1/new.jpg")).thenReturn("https://s3.example.com/profiles/user-1/new.jpg");

            // when
            UserProfileResponse response = userService.updateProfile(1L, request, profileImage);

            // then
            assertEquals("https://s3.example.com/profiles/user-1/new.jpg", response.getProfileImageUrl());
            verify(storageService).uploadProfileImage(1L, profileImage);
            verify(storageService).deleteFile("profiles/user-1/old.jpg");
        }
    }

    @Nested
    @DisplayName("2. updateProfile() - 유저 검증 실패")
    class UpdateProfileUserValidationFailCases {

        @Test
        @DisplayName("TC-2-1. 존재하지 않는 유저 → USER_NOT_FOUND")
        void userNotFound() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            UpdateProfileRequest request = UpdateProfileRequest.ofForTest(null, null, null, null, null);
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateProfile(999L, request, null));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(storageService, never()).uploadProfileImage(any(), any());
        }
    }

    @Nested
    @DisplayName("3. updateProfile() - 입력값 변환 실패")
    class UpdateProfileInputParsingFailCases {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        }

        @Test
        @DisplayName("TC-3-1. 잘못된 gender 값 → INVALID_INPUT_VALUE")
        void invalidGender() {
            // given
            UpdateProfileRequest request = UpdateProfileRequest.ofForTest(null, null, "INVALID_GENDER", null, null);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateProfile(1L, request, null));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
        }

        @Test
        @DisplayName("TC-3-2. 잘못된 country 값 → INVALID_INPUT_VALUE")
        void invalidCountry() {
            // given
            UpdateProfileRequest request = UpdateProfileRequest.ofForTest(null, null, null, "INVALID_COUNTRY", null);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateProfile(1L, request, null));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
        }

        @Test
        @DisplayName("TC-3-3. 잘못된 birthday 형식 → INVALID_INPUT_VALUE")
        void invalidBirthday() {
            // given
            UpdateProfileRequest request = UpdateProfileRequest.ofForTest(null, "2000/01/01", null, null, null);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateProfile(1L, request, null));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("4. updateProfile() - S3 업로드 실패")
    class UpdateProfileS3FailCases {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        }

        @Test
        @DisplayName("TC-4-1. S3 업로드 실패 → S3_CONNECTION_ERROR, DB 업데이트 안 됨")
        void s3UploadFails() {
            // given
            UpdateProfileRequest request = UpdateProfileRequest.ofForTest(null, null, null, null, null);
            MultipartFile profileImage = mock(MultipartFile.class);
            when(profileImage.isEmpty()).thenReturn(false);
            when(storageService.uploadProfileImage(1L, profileImage))
                    .thenThrow(new CustomException(ErrorCode.S3_CONNECTION_ERROR));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateProfile(1L, request, profileImage));

            assertAll(
                    () -> assertEquals(ErrorCode.S3_CONNECTION_ERROR, ex.getErrorCode()),
                    () -> assertEquals("홍길동", user.getName())  // DB 업데이트 안 됨
            );
        }
    }

    @Nested
    @DisplayName("5. deleteUser() - 성공 케이스")
    class DeleteUserSuccessCases {

        @Test
        @DisplayName("TC-5-1. 정상 탈퇴 → softDelete + RefreshToken/FcmToken 삭제 + Firebase 토큰 무효화 호출")
        void deleteUserSuccess() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // when
            userService.deleteUser(1L);

            // then
            assertTrue(user.isDeleted());
            verify(refreshTokenRepository).deleteByUser(user);
            verify(fcmTokenRepository).deleteByUser(user);
            verify(firebaseTokenService).revokeTokens(1L);
        }
    }

    @Nested
    @DisplayName("6. deleteUser() - 실패 케이스")
    class DeleteUserFailCases {

        @Test
        @DisplayName("TC-6-1. 존재하지 않는 유저 → USER_NOT_FOUND, 토큰 삭제/무효화 호출 안 됨")
        void userNotFound() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.deleteUser(999L));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(refreshTokenRepository, never()).deleteByUser(any());
            verify(fcmTokenRepository, never()).deleteByUser(any());
            verify(firebaseTokenService, never()).revokeTokens(any());
        }
    }

    @Nested
    @DisplayName("7. updateLanguageLevel() - 성공 케이스")
    class UpdateLanguageLevelSuccessCases {

        @Test
        @DisplayName("TC-7-1. 정상 레벨 수정")
        void updateLanguageLevelSuccess() {
            // given
            when(userLanguageRepository.findByUserUserIdAndLanguage(1L, Language.ENGLISH))
                    .thenReturn(Optional.of(userLanguage));

            UserLanguageRequest request = UserLanguageRequest.ofForTest("ENGLISH", 4);

            // when
            userService.updateLanguageLevel(1L, request);

            // then
            assertEquals(4, userLanguage.getLevel());
        }
    }

    @Nested
    @DisplayName("8. updateLanguageLevel() - 실패 케이스")
    class UpdateLanguageLevelFailCases {

        @Test
        @DisplayName("TC-8-1. 잘못된 language 값 → INVALID_INPUT_VALUE")
        void invalidLanguage() {
            // given
            UserLanguageRequest request = UserLanguageRequest.ofForTest("INVALID_LANGUAGE", null);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateLanguageLevel(1L, request));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
        }

        @Test
        @DisplayName("TC-8-2. 관심 언어 목록에 없는 언어 → USER_LANGUAGE_NOT_FOUND")
        void languageNotInUserList() {
            // given
            when(userLanguageRepository.findByUserUserIdAndLanguage(1L, Language.JAPANESE))
                    .thenReturn(Optional.empty());

            UserLanguageRequest request = UserLanguageRequest.ofForTest("JAPANESE", null);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateLanguageLevel(1L, request));

            assertEquals(ErrorCode.USER_LANGUAGE_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("9. updateTags() - 성공 케이스")
    class UpdateTagsSuccessCases {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        }

        @Test
        @DisplayName("TC-9-1. 유효한 태그 목록 전달 → delete 먼저 호출 후 saveAll, 태그 올바르게 변환됨")
        void updateTagsSuccess() {
            // given
            UpdateTagsRequest request = UpdateTagsRequest.ofForTest(List.of("WORKOUT", "INTJ", "MOVIE"));

            // when
            userService.updateTags(1L, request);

            // then: delete → saveAll 순서 보장 + 저장된 UserTag 내용 검증
            InOrder inOrder = inOrder(userTagRepository);
            inOrder.verify(userTagRepository).deleteAllTagsByUserId(1L);
            inOrder.verify(userTagRepository).saveAll(userTagsCaptor.capture());

            List<UserTag> saved = userTagsCaptor.getValue();
            assertAll(
                    () -> assertEquals(3, saved.size()),
                    () -> assertEquals(Tag.WORKOUT, saved.get(0).getTag()),
                    () -> assertEquals(Tag.INTJ, saved.get(1).getTag()),
                    () -> assertEquals(Tag.MOVIE, saved.get(2).getTag()),
                    () -> saved.forEach(ut -> assertEquals(user, ut.getUser()))
            );
        }

        @Test
        @DisplayName("TC-9-2. 빈 리스트 전달 → 기존 태그 전체 삭제, saveAll에 빈 리스트 전달")
        void updateTagsWithEmptyList() {
            // given
            UpdateTagsRequest request = UpdateTagsRequest.ofForTest(List.of());

            // when
            userService.updateTags(1L, request);

            // then
            verify(userTagRepository).deleteAllTagsByUserId(1L);

            verify(userTagRepository).saveAll(userTagsCaptor.capture());
            assertTrue(userTagsCaptor.getValue().isEmpty());
        }
    }

    @Nested
    @DisplayName("10. updateTags() - 실패 케이스")
    class UpdateTagsFailCases {

        @Test
        @DisplayName("TC-10-1. 존재하지 않는 유저 → USER_NOT_FOUND, delete 미호출")
        void userNotFound() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            UpdateTagsRequest request = UpdateTagsRequest.ofForTest(List.of());

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateTags(999L, request));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(userTagRepository, never()).deleteAllTagsByUserId(any());
        }

        @Test
        @DisplayName("TC-10-3. 유효하지 않은 태그 문자열 → INVALID_INPUT_VALUE")
        void invalidTagString() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UpdateTagsRequest request = UpdateTagsRequest.ofForTest(List.of("WORKOUT", "INVALID_TAG"));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateTags(1L, request));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
            verify(userTagRepository).deleteAllTagsByUserId(1L);
        }
    }

    @Nested
    @DisplayName("11. getUserTags() - 성공 케이스")
    class GetUserTagsSuccessCases {

        @Test
        @DisplayName("TC-11-1. 태그가 있는 유저 조회 → tag, category 포함 목록 반환")
        void getUserTagsSuccess() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            List<UserTag> userTags = List.of(
                    UserTag.of(user, Tag.TRAVEL),
                    UserTag.of(user, Tag.KOREAN_FOOD)
            );
            when(userTagRepository.findAllByUserId(1L)).thenReturn(userTags);

            // when
            List<UserTagResponse> result = userService.getUserTags(1L);

            // then
            assertAll(
                    () -> assertEquals(2, result.size()),
                    () -> assertEquals("TRAVEL", result.get(0).getTag()),
                    () -> assertEquals("HOBBY", result.get(0).getCategory()),
                    () -> assertEquals("KOREAN_FOOD", result.get(1).getTag()),
                    () -> assertEquals("FOOD", result.get(1).getCategory())
            );
        }

        @Test
        @DisplayName("TC-11-2. 태그가 없는 유저 조회 → 빈 목록 반환")
        void getUserTagsEmpty() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // when
            List<UserTagResponse> result = userService.getUserTags(1L);

            // then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("12. getUserTags() - 실패 케이스")
    class GetUserTagsFailCases {

        @Test
        @DisplayName("TC-12-1. 존재하지 않는 유저 → USER_NOT_FOUND")
        void userNotFound() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.getUserTags(999L));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(userTagRepository, never()).findAllByUserId(any());
        }

        @Test
        @DisplayName("TC-12-2. 탈퇴한 유저 → USER_DELETED")
        void userDeleted() {
            // given
            when(userRepository.findById(2L)).thenReturn(Optional.of(deletedUser));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.getUserTags(2L));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
            verify(userTagRepository, never()).findAllByUserId(any());
        }
    }

    @Nested
    @DisplayName("13. getUserProfile() - 성공 케이스")
    class GetUserProfileSuccessCases {

        private User targetUser;

        @BeforeEach
        void setUp() {
            targetUser = User.createForTest(3L, "target", "대상유저", "password",
                    Country.USA, Gender.FEMALE, LocalDate.of(1995, 5, 5));
        }

        @Test
        @DisplayName("TC-13-1. 본인 조회 → birthday 채워짐, isFriend=null")
        void getOwnProfile() {
            // given
            when(userRepository.findById(3L)).thenReturn(Optional.of(targetUser));

            // when
            UserProfileViewResponse response = userService.getUserProfile(3L, 3L);

            // then
            assertAll(
                    () -> assertNull(response.getProfileImageUrl()),
                    () -> assertEquals("USA", response.getCountry()),
                    () -> assertEquals("대상유저", response.getName()),
                    () -> assertEquals(Period.between(LocalDate.of(1995, 5, 5), LocalDate.now()).getYears(), response.getAge()),
                    () -> assertEquals(LocalDate.of(1995, 5, 5), response.getBirthday()),
                    () -> assertEquals("FEMALE", response.getGender()),
                    () -> assertNull(response.getBio()),
                    () -> assertEquals(0, response.getConsecutiveDays()),
                    () -> assertNull(response.getIsFriend())
            );
            verify(friendRelationRepository, never()).existsFriendRelationBetween(any(), any());
        }

        @Test
        @DisplayName("TC-13-2. 타인 조회 (친구 아님) → birthday null, isFriend=false")
        void getOtherUserProfileNotFriend() {
            // given
            when(userRepository.findById(3L)).thenReturn(Optional.of(targetUser));
            when(friendRelationRepository.existsFriendRelationBetween(1L, 3L)).thenReturn(false);

            // when
            UserProfileViewResponse response = userService.getUserProfile(3L, 1L);

            // then
            assertAll(
                    () -> assertNull(response.getProfileImageUrl()),
                    () -> assertEquals("USA", response.getCountry()),
                    () -> assertEquals("대상유저", response.getName()),
                    () -> assertEquals(Period.between(LocalDate.of(1995, 5, 5), LocalDate.now()).getYears(), response.getAge()),
                    () -> assertNull(response.getBirthday()),
                    () -> assertEquals("FEMALE", response.getGender()),
                    () -> assertNull(response.getBio()),
                    () -> assertEquals(0, response.getConsecutiveDays()),
                    () -> assertEquals(Boolean.FALSE, response.getIsFriend())
            );
        }

        @Test
        @DisplayName("TC-13-3. 타인 조회 (친구) → isFriend=true")
        void getOtherUserProfileFriend() {
            // given
            when(userRepository.findById(3L)).thenReturn(Optional.of(targetUser));
            when(friendRelationRepository.existsFriendRelationBetween(1L, 3L)).thenReturn(true);

            // when
            UserProfileViewResponse response = userService.getUserProfile(3L, 1L);

            // then
            assertEquals(Boolean.TRUE, response.getIsFriend());
        }

        @Test
        @DisplayName("TC-13-4. 본인/타인 모두 consecutiveDays 반환 (오늘 출석 → 저장된 값)")
        void consecutiveDaysReturnedRegardlessOfOwner() {
            // given
            targetUser.recordAttendance(LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
            when(userRepository.findById(3L)).thenReturn(Optional.of(targetUser));

            // when
            UserProfileViewResponse ownerResponse = userService.getUserProfile(3L, 3L);
            UserProfileViewResponse otherResponse = userService.getUserProfile(3L, 1L);

            // then
            assertAll(
                    () -> assertEquals(1, ownerResponse.getConsecutiveDays()),
                    () -> assertEquals(1, otherResponse.getConsecutiveDays())
            );
        }

    }

    @Nested
    @DisplayName("14. getUserProfile() - 실패 케이스")
    class GetUserProfileFailCases {

        @Test
        @DisplayName("TC-14-1. 존재하지 않는 유저 → USER_NOT_FOUND")
        void userNotFound() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.getUserProfile(999L, 1L));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("TC-14-2. 탈퇴한 유저 → USER_DELETED")
        void userDeleted() {
            // given
            when(userRepository.findById(2L)).thenReturn(Optional.of(deletedUser));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.getUserProfile(2L, 1L));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("15. getUserLanguages() - 성공 케이스")
    class GetUserLanguagesSuccessCases {

        @Test
        @DisplayName("TC-15-1. 언어가 있는 유저, 타인 조회 → languages 목록 + isOwner=false")
        void getUserLanguagesSuccess() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            List<UserLanguage> userLanguages = List.of(
                    UserLanguage.of(user, Language.ENGLISH, 3, false),
                    UserLanguage.of(user, Language.JAPANESE, 2, false)
            );
            when(userLanguageRepository.findAllByUserId(1L)).thenReturn(userLanguages);

            // when
            UserLanguagesResponse result = userService.getUserLanguages(1L, 2L);

            // then
            assertAll(
                    () -> assertFalse(result.isOwner()),
                    () -> assertEquals(2, result.getLanguages().size()),
                    () -> assertEquals("ENGLISH", result.getLanguages().get(0).getLanguage()),
                    () -> assertEquals(3, result.getLanguages().get(0).getLevel()),
                    () -> assertEquals("JAPANESE", result.getLanguages().get(1).getLanguage()),
                    () -> assertEquals(2, result.getLanguages().get(1).getLevel())
            );
        }

        @Test
        @DisplayName("TC-15-2. 본인 조회 → isOwner=true")
        void getOwnLanguages() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // when
            UserLanguagesResponse result = userService.getUserLanguages(1L, 1L);

            // then
            assertTrue(result.isOwner());
        }

        @Test
        @DisplayName("TC-15-3. 언어가 없는 유저 → 빈 목록 + isOwner=false")
        void getUserLanguagesEmpty() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // when
            UserLanguagesResponse result = userService.getUserLanguages(1L, 2L);

            // then
            assertAll(
                    () -> assertFalse(result.isOwner()),
                    () -> assertTrue(result.getLanguages().isEmpty())
            );
        }
    }

    @Nested
    @DisplayName("16. getUserLanguages() - 실패 케이스")
    class GetUserLanguagesFailCases {

        @Test
        @DisplayName("TC-16-1. 존재하지 않는 유저 → USER_NOT_FOUND")
        void userNotFound() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.getUserLanguages(999L, 1L));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(userLanguageRepository, never()).findAllByUserId(any());
        }

        @Test
        @DisplayName("TC-16-2. 탈퇴한 유저 → USER_DELETED")
        void userDeleted() {
            // given
            when(userRepository.findById(2L)).thenReturn(Optional.of(deletedUser));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.getUserLanguages(2L, 1L));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
            verify(userLanguageRepository, never()).findAllByUserId(any());
        }
    }

    @Nested
    @DisplayName("17. recordAttendance()")
    class RecordAttendanceCases {

        @Test
        @DisplayName("TC-17-1. 첫 출석 → User.consecutiveDays=1, lastAttendanceDate=오늘(KST)")
        void firstAttendance() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // when
            userService.recordAttendance(1L);

            // then
            LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
            assertAll(
                    () -> assertEquals(1, user.getConsecutiveDays()),
                    () -> assertEquals(today, user.getLastAttendanceDate())
            );
        }

        @Test
        @DisplayName("TC-17-2. 존재하지 않는 유저 → USER_NOT_FOUND")
        void userNotFound() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.recordAttendance(999L));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("TC-17-3. 탈퇴한 유저 → USER_DELETED")
        void userDeleted() {
            // given
            when(userRepository.findById(2L)).thenReturn(Optional.of(deletedUser));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.recordAttendance(2L));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
        }
    }
}
