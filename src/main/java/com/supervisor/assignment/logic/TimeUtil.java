package com.supervisor.assignment.logic;

import java.time.LocalTime;

public class TimeUtil {
    public static boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    public static int minutesSinceMidnight(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }
}


