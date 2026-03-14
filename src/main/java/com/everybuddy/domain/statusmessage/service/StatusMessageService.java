package com.everybuddy.domain.statusmessage.service;

import com.everybuddy.domain.statusmessage.dto.CreateStatusMessageRequest;
import com.everybuddy.domain.statusmessage.dto.FriendStatusMessageListResponse;
import com.everybuddy.domain.statusmessage.dto.FriendStatusMessageResponse;
import com.everybuddy.domain.statusmessage.dto.MyStatusMessageResponse;
import com.everybuddy.domain.statusmessage.dto.UpdateStatusMessageRequest;
import com.everybuddy.domain.statusmessage.entity.StatusMessage;
import com.everybuddy.domain.statusmessage.repository.StatusMessageRepository;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.everybuddy.global.s3.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class StatusMessageService {

    private final StatusMessageRepository statusMessageRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public void createStatusMessage(Long userId, CreateStatusMessageRequest request) {
        validateAndCleanupExisting(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        statusMessageRepository.save(StatusMessage.of(user, request.getContent()));
    }

    public void updateStatusMessage(Long userId, UpdateStatusMessageRequest request) {
        StatusMessage statusMessage = statusMessageRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.STATUS_MESSAGE_NOT_FOUND));

        if (isExpired(statusMessage)) {
            throw new CustomException(ErrorCode.STATUS_MESSAGE_EXPIRED);
        }

        statusMessage.updateContent(request.getContent());
    }

    public void deleteStatusMessage(Long userId) {
        StatusMessage statusMessage = statusMessageRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.STATUS_MESSAGE_NOT_FOUND));

        statusMessage.softDelete();
    }

    @Transactional(readOnly = true)
    public MyStatusMessageResponse getMyStatusMessage(Long userId) {
        StatusMessage statusMessage = statusMessageRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.STATUS_MESSAGE_NOT_FOUND));
        return MyStatusMessageResponse.from(statusMessage, getProfileImageUrl(statusMessage.getUser().getProfile()));
    }

    @Transactional(readOnly = true)
    public FriendStatusMessageListResponse getFriendStatusMessages(Long userId, Long cursor, int size) {
        List<StatusMessage> results = fetchFriendStatusMessages(userId, cursor, size);

        return toPagedResponse(results, size);
    }

    private boolean isExpired(StatusMessage statusMessage) {
        return statusMessage.getUpdatedAt().isBefore(LocalDateTime.now().minusHours(24));
    }

    private void validateAndCleanupExisting(Long userId) {
        statusMessageRepository.findByUserId(userId).ifPresent(existing -> {
            if (!isExpired(existing)) {
                throw new CustomException(ErrorCode.STATUS_MESSAGE_ALREADY_EXISTS);
            }
            existing.softDelete();
        });
    }

    private String getProfileImageUrl(String profileKey) {
        return profileKey != null ? storageService.getPublicUrl(profileKey) : null;
    }

    private List<StatusMessage> fetchFriendStatusMessages(Long userId, Long cursor, int size) {
        if (cursor == null) {
            return statusMessageRepository.findFriendStatusMessages(userId, PageRequest.of(0, size + 1));
        }

        StatusMessage cursorMessage = statusMessageRepository.findById(cursor)
                .orElseThrow(() -> new CustomException(ErrorCode.STATUS_MESSAGE_NOT_FOUND));

        return statusMessageRepository.findFriendStatusMessagesAfterCursor(
                userId, cursorMessage.getUpdatedAt(), cursor, PageRequest.of(0, size + 1));
    }

    private FriendStatusMessageListResponse toPagedResponse(List<StatusMessage> results, int size) {
        boolean hasNext = results.size() > size;
        List<StatusMessage> page = hasNext ? results.subList(0, size) : results;
        Long nextCursor = page.isEmpty() ? null : page.getLast().getStatusMessageId();
        List<FriendStatusMessageResponse> responses = toResponses(page);
        return FriendStatusMessageListResponse.of(responses, nextCursor, hasNext);
    }

    private List<FriendStatusMessageResponse> toResponses(List<StatusMessage> statusMessages) {
        return statusMessages.stream()
                .map(sm -> FriendStatusMessageResponse.from(sm, getProfileImageUrl(sm.getUser().getProfile())))
                .toList();
    }
}
