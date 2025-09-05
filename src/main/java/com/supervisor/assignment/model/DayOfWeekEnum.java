package com.supervisor.assignment.model;

public enum DayOfWeekEnum {
    SATURDAY,
    SUNDAY,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY;

    public static DayOfWeekEnum fromString(String s) {
        if (s == null) return null;
        String normalized = s.trim();
        String upper = normalized.toUpperCase();
        switch (upper) {
            case "SAT": case "SATURDAY": return SATURDAY;
            case "SUN": case "SUNDAY": return SUNDAY;
            case "MON": case "MONDAY": return MONDAY;
            case "TUE": case "TUESDAY": return TUESDAY;
            case "WED": case "WEDNESDAY": return WEDNESDAY;
            case "THU": case "THURSDAY": return THURSDAY;
            case "FRI": case "FRIDAY": return FRIDAY;
        }
        String ar = normalized.replace("يوم", "").trim();
        switch (ar) {
            case "السبت": return SATURDAY;
            case "الأحد": case "الاحد": return SUNDAY;
            case "الإثنين": case "الاثنين": return MONDAY;
            case "الثلاثاء": return TUESDAY;
            case "الأربعاء": case "الاربعاء": return WEDNESDAY;
            case "الخميس": return THURSDAY;
            case "الجمعة": return FRIDAY;
            default: throw new IllegalArgumentException("Unknown day: " + s);
        }
    }
}


