package com.everybuddy.domain.auth.dto;

import com.everybuddy.domain.user.entity.Country;
import com.everybuddy.domain.user.entity.Gender;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class RegisterRequestDto {
    private String id;

    private String password;

    private String name;

    private Country country;

    private LocalDate birthday;

    private Gender gender;

    // 서비스 약관 동의여부, 검증을 위해 둔 필드
    private boolean checked;
}
