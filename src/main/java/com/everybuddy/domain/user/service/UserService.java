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
import com.everybuddy.domain.user.repository.UserLanguageRepository;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.domain.user.repository.UserTagRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.s3.service.StorageService;
import com.everybuddy.global.util.EnumConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserLanguageRepository userLanguageRepository;
    private final UserTagRepository userTagRepository;
    private final StorageService storageService;

    public void updateLanguageLevel(Long userId, UserLanguageRequest request) {
        Language language = EnumConverter.stringToEnum(
                request.getLanguage(), Language.class, ErrorCode.INVALID_INPUT_VALUE);

        UserLanguage userLanguage = userLanguageRepository
                .findByUserUserIdAndLanguage(userId, language)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_LANGUAGE_NOT_FOUND));

        userLanguage.updateLevel(request.getLevel());
    }

    public void updateTags(Long userId, UpdateTagsRequest request) {
        User user = findUser(userId);

        userTagRepository.deleteAllTagsByUserId(userId);

        List<UserTag> userTags = getRequestUserTags(request, user);

        userTagRepository.saveAll(userTags);
    }


    @Transactional(readOnly = true)
    public UserProfileViewResponse getUserProfile(Long userId) {
        User user = findActiveUser(userId);
        return UserProfileViewResponse.from(user, getProfileImageUrl(user.getProfile()));
    }

    @Transactional(readOnly = true)
    public List<UserTagResponse> getUserTags(Long userId) {
        findActiveUser(userId);

        return getUserTagResponses(userId);
    }

    @Transactional(readOnly = true)
    public UserLanguagesResponse getUserLanguages(Long userId, Long requesterId) {
        findActiveUser(userId);

        List<UserLanguageResponse> languages = getUserLanguages(userId);

        return UserLanguagesResponse.of(requesterId.equals(userId), languages);
    }


    public void deleteUser(Long userId) {
        User user = findUser(userId);
        user.softDelete();
    }

    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request, MultipartFile profileImage) {
        User user = findUser(userId);

        String newProfileKey = uploadNewProfileImage(userId, profileImage);
        String oldProfileKey = user.getProfile();

        user.updateProfile(
                request.getName(),
                parseBirthday(request.getBirthday()),
                parseGender(request.getGender()),
                parseCountry(request.getCountry()),
                request.getBio(),
                newProfileKey
        );

        deleteOldProfileImage(oldProfileKey, newProfileKey);

        return UserProfileResponse.from(user, getProfileImageUrl(user.getProfile()));
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

    private String uploadNewProfileImage(Long userId, MultipartFile profileImage) {
        if (profileImage == null || profileImage.isEmpty()) {
            return null;
        }
        return storageService.uploadProfileImage(userId, profileImage);
    }

    private void deleteOldProfileImage(String oldProfileKey, String newProfileKey) {
        if (newProfileKey != null && oldProfileKey != null) {
            storageService.deleteFile(oldProfileKey);
        }
    }

    private String getProfileImageUrl(String profileKey) {
        return profileKey != null ? storageService.getPublicUrl(profileKey) : null;
    }

    private Gender parseGender(String gender) {
        if (gender == null) return null;
        return EnumConverter.stringToEnum(gender, Gender.class, ErrorCode.INVALID_INPUT_VALUE);
    }

    private Country parseCountry(String country) {
        if (country == null) return null;
        return EnumConverter.stringToEnum(country, Country.class, ErrorCode.INVALID_INPUT_VALUE);
    }

    private LocalDate parseBirthday(String birthday) {
        if (birthday == null) return null;
        try {
            return LocalDate.parse(birthday);
        } catch (DateTimeParseException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private List<UserTag> getRequestUserTags(UpdateTagsRequest request, User user) {
        return request.getTags().stream()
                .distinct()
                .map(tagStr -> EnumConverter.stringToEnum(tagStr, Tag.class, ErrorCode.INVALID_INPUT_VALUE))
                .map(tag -> UserTag.of(user, tag))
                .toList();
    }

    private List<UserLanguageResponse> getUserLanguages(Long userId) {
        return userLanguageRepository.findAllByUserId(userId).stream()
                .map(UserLanguageResponse::from)
                .toList();
    }

    private List<UserTagResponse> getUserTagResponses(Long userId) {
        return userTagRepository.findAllByUserId(userId).stream()
                .map(UserTagResponse::from)
                .toList();
    }
}
