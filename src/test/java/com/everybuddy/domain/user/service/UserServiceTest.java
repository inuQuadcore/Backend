package com.everybuddy.domain.user.service;

import com.everybuddy.domain.user.dto.UpdateProfileRequest;
import com.everybuddy.domain.user.dto.UpdateTagsRequest;
import com.everybuddy.domain.user.dto.UserLanguageRequest;
import com.everybuddy.domain.user.dto.UserProfileResponse;
import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import com.everybuddy.domain.user.entity.Language;
import com.everybuddy.domain.user.entity.Tag;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.entity.UserLanguage;
import com.everybuddy.domain.user.entity.UserTag;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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
    @Mock private StorageService storageService;

    @InjectMocks
    private UserService userService;

    private User user;
    private User deletedUser;
    private UserLanguage userLanguage;

    @BeforeEach
    void setUp() {
        user = User.createForTest(1L, "user1", "홍길동", "password",
                Country.KOREA, Language.KOREAN, Gender.MALE, LocalDate.of(1990, 1, 1));

        deletedUser = User.createForTest(2L, "deleted", "삭제된유저", "password",
                Country.KOREA, Language.KOREAN, Gender.FEMALE, LocalDate.of(1992, 1, 1));
        deletedUser.softDelete();

        userLanguage = UserLanguage.of(user, Language.ENGLISH, 2);
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
            UpdateProfileRequest request = mock(UpdateProfileRequest.class);
            when(request.getName()).thenReturn("김철수");
            when(request.getBirthday()).thenReturn("1995-06-15");
            when(request.getGender()).thenReturn("FEMALE");
            when(request.getCountry()).thenReturn("USA");
            when(request.getBio()).thenReturn("안녕하세요");

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
            UpdateProfileRequest request = mock(UpdateProfileRequest.class);
            when(request.getName()).thenReturn("김철수");
            // birthday, gender, country, bio → null (Mockito 기본값)

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
            UpdateProfileRequest request = mock(UpdateProfileRequest.class);
            MultipartFile profileImage = mock(MultipartFile.class);
            when(profileImage.isEmpty()).thenReturn(false);
            when(storageService.uploadProfileImage(1L, profileImage)).thenReturn("profiles/user-1/new.jpg");
            when(storageService.getPublicUrl("profiles/user-1/new.jpg")).thenReturn("https://s3.example.com/profiles/user-1/new.jpg");

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

            UpdateProfileRequest request = mock(UpdateProfileRequest.class);
            MultipartFile profileImage = mock(MultipartFile.class);
            when(profileImage.isEmpty()).thenReturn(false);
            when(storageService.uploadProfileImage(1L, profileImage)).thenReturn("profiles/user-1/new.jpg");
            when(storageService.getPublicUrl("profiles/user-1/new.jpg")).thenReturn("https://s3.example.com/profiles/user-1/new.jpg");

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
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateProfile(999L, mock(UpdateProfileRequest.class), null));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(storageService, never()).uploadProfileImage(any(), any());
        }

        @Test
        @DisplayName("TC-2-2. 탈퇴한 유저 → USER_DELETED")
        void userDeleted() {
            // given
            when(userRepository.findById(2L)).thenReturn(Optional.of(deletedUser));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateProfile(2L, mock(UpdateProfileRequest.class), null));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
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
            UpdateProfileRequest request = mock(UpdateProfileRequest.class);
            when(request.getGender()).thenReturn("INVALID_GENDER");

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateProfile(1L, request, null));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
        }

        @Test
        @DisplayName("TC-3-2. 잘못된 country 값 → INVALID_INPUT_VALUE")
        void invalidCountry() {
            // given
            UpdateProfileRequest request = mock(UpdateProfileRequest.class);
            when(request.getCountry()).thenReturn("INVALID_COUNTRY");

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateProfile(1L, request, null));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
        }

        @Test
        @DisplayName("TC-3-3. 잘못된 birthday 형식 → INVALID_INPUT_VALUE")
        void invalidBirthday() {
            // given
            UpdateProfileRequest request = mock(UpdateProfileRequest.class);
            when(request.getBirthday()).thenReturn("2000/01/01");

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
            UpdateProfileRequest request = mock(UpdateProfileRequest.class);
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
        @DisplayName("TC-5-1. 정상 탈퇴 → softDelete 호출 검증")
        void deleteUserSuccess() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // when
            userService.deleteUser(1L);

            // then
            assertTrue(user.isDeleted());
        }
    }

    @Nested
    @DisplayName("6. deleteUser() - 실패 케이스")
    class DeleteUserFailCases {

        @Test
        @DisplayName("TC-6-1. 존재하지 않는 유저 → USER_NOT_FOUND")
        void userNotFound() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.deleteUser(999L));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("TC-6-2. 이미 탈퇴한 유저 → USER_DELETED")
        void userAlreadyDeleted() {
            // given
            when(userRepository.findById(2L)).thenReturn(Optional.of(deletedUser));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.deleteUser(2L));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("7. updateLanguageLevel() - 성공 케이스")
    class UpdateLanguageLevelSuccessCases {

        @Test
        @DisplayName("TC-7-1. 정상 레벨 수정")
        void updateLanguageLevelSuccess() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userLanguageRepository.findByUserUserIdAndLanguage(1L, Language.ENGLISH))
                    .thenReturn(Optional.of(userLanguage));

            UserLanguageRequest request = mock(UserLanguageRequest.class);
            when(request.getLanguage()).thenReturn("ENGLISH");
            when(request.getLevel()).thenReturn(4);

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
        @DisplayName("TC-8-1. 존재하지 않는 유저 → USER_NOT_FOUND")
        void userNotFound() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateLanguageLevel(999L, mock(UserLanguageRequest.class)));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("TC-8-2. 탈퇴한 유저 → USER_DELETED")
        void userDeleted() {
            // given
            when(userRepository.findById(2L)).thenReturn(Optional.of(deletedUser));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateLanguageLevel(2L, mock(UserLanguageRequest.class)));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
        }

        @Test
        @DisplayName("TC-8-3. 잘못된 language 값 → INVALID_INPUT_VALUE")
        void invalidLanguage() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserLanguageRequest request = mock(UserLanguageRequest.class);
            when(request.getLanguage()).thenReturn("INVALID_LANGUAGE");

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateLanguageLevel(1L, request));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
        }

        @Test
        @DisplayName("TC-8-4. 관심 언어 목록에 없는 언어 → USER_LANGUAGE_NOT_FOUND")
        void languageNotInUserList() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userLanguageRepository.findByUserUserIdAndLanguage(1L, Language.JAPANESE))
                    .thenReturn(Optional.empty());

            UserLanguageRequest request = mock(UserLanguageRequest.class);
            when(request.getLanguage()).thenReturn("JAPANESE");

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
            UpdateTagsRequest request = mock(UpdateTagsRequest.class);
            when(request.getTags()).thenReturn(List.of("SPORTS", "INTJ", "MOVIES"));

            // when
            userService.updateTags(1L, request);

            // then: delete → saveAll 순서 보장
            InOrder inOrder = inOrder(userTagRepository);
            inOrder.verify(userTagRepository).deleteAllTagsByUserId(1L);
            inOrder.verify(userTagRepository).saveAll(any());

            // then: 저장된 UserTag 내용 검증
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<UserTag>> captor = ArgumentCaptor.forClass(List.class);
            verify(userTagRepository).saveAll(captor.capture());

            List<UserTag> saved = captor.getValue();
            assertAll(
                    () -> assertEquals(3, saved.size()),
                    () -> assertEquals(Tag.SPORTS, saved.get(0).getTag()),
                    () -> assertEquals(Tag.INTJ, saved.get(1).getTag()),
                    () -> assertEquals(Tag.MOVIES, saved.get(2).getTag()),
                    () -> saved.forEach(ut -> assertEquals(user, ut.getUser()))
            );
        }

        @Test
        @DisplayName("TC-9-2. 빈 리스트 전달 → 기존 태그 전체 삭제, saveAll에 빈 리스트 전달")
        void updateTagsWithEmptyList() {
            // given
            UpdateTagsRequest request = mock(UpdateTagsRequest.class);
            when(request.getTags()).thenReturn(List.of());

            // when
            userService.updateTags(1L, request);

            // then
            verify(userTagRepository).deleteAllTagsByUserId(1L);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<UserTag>> captor = ArgumentCaptor.forClass(List.class);
            verify(userTagRepository).saveAll(captor.capture());
            assertTrue(captor.getValue().isEmpty());
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

            UpdateTagsRequest request = mock(UpdateTagsRequest.class);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateTags(999L, request));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(userTagRepository, never()).deleteAllTagsByUserId(any());
        }

        @Test
        @DisplayName("TC-10-2. 탈퇴한 유저 → USER_DELETED, delete 미호출")
        void userDeleted() {
            // given
            when(userRepository.findById(2L)).thenReturn(Optional.of(deletedUser));

            UpdateTagsRequest request = mock(UpdateTagsRequest.class);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateTags(2L, request));

            assertEquals(ErrorCode.USER_DELETED, ex.getErrorCode());
            verify(userTagRepository, never()).deleteAllTagsByUserId(any());
        }

        @Test
        @DisplayName("TC-10-3. 유효하지 않은 태그 문자열 → INVALID_INPUT_VALUE")
        void invalidTagString() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UpdateTagsRequest request = mock(UpdateTagsRequest.class);
            when(request.getTags()).thenReturn(List.of("SPORTS", "INVALID_TAG"));

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.updateTags(1L, request));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
        }
    }
}
