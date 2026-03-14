package com.everybuddy.global.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class TimeAgoUtil {

    private TimeAgoUtil() {}

    public static String format(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);

        if (minutes < 1) return "방금 전";
        if (minutes < 60) return minutes + "분 전";

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) return hours + "시간 전";

        long days = ChronoUnit.DAYS.between(dateTime, now);
        return days + "일 전";
    }
}
