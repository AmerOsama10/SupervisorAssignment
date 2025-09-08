package com.supervisor.assignment.model;

public enum PeriodOfDay {
    MORNING,
    EVENING;

    public static PeriodOfDay fromString(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;
        v = v.toLowerCase();
        // Arabic
        if (v.contains("ص") || v.contains("صباح")) return MORNING;
        if (v.contains("م") || v.contains("مساء")) return EVENING;
        if (v.equals("صباحي") || v.equals("صباحى")) return MORNING;
        if (v.equals("مسائي") || v.equals("مسائى")) return EVENING;
        // English
        if (v.startsWith("morn")) return MORNING;
        if (v.startsWith("even")) return EVENING;
        // Fallback by common tokens
        if (v.equals("am")) return MORNING;
        if (v.equals("pm")) return EVENING;
        throw new IllegalArgumentException("Unknown period: " + s);
    }
}



