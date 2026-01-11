package com.everybuddy.global.util;

import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;

public class EnumConverter {

    public static <T extends Enum<T>> T stringToEnum(String value, Class<T> enumClass, ErrorCode errorCode) {
        T[] enumConstants = enumClass.getEnumConstants();

        for (T enumConstant : enumConstants) {
            if (enumConstant.name().equals(value)) {
                return enumConstant;
            }
        }

        throw new CustomException(errorCode);
    }
}