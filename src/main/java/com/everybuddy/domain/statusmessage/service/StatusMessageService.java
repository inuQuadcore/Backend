package com.everybuddy.domain.statusmessage.service;

import com.everybuddy.domain.statusmessage.dto.CreateStatusMessageRequest;
import com.everybuddy.domain.statusmessage.entity.StatusMessage;
import com.everybuddy.domain.statusmessage.repository.StatusMessageRepository;
import com.everybuddy.domain.user.entity.User;
import com.everybuddy.domain.user.repository.UserRepository;
import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class StatusMessageService {

    private final StatusMessageRepository statusMessageRepository;
    private final UserRepository userRepository;

    public void createStatusMessage(Long userId, CreateStatusMessageRequest request) {
        if (statusMessageRepository.existsByUserId(userId)) {
            throw new CustomException(ErrorCode.STATUS_MESSAGE_ALREADY_EXISTS);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        statusMessageRepository.save(StatusMessage.of(user, request.getContent()));
    }
}
