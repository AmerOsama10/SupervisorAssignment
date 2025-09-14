package com.supervisor.assignment.model;

public enum RoleType {
    FLOOR_SUPERVISOR, // مشرف دور
    INVIGILATOR,      // ملاحظ
    MAINTENANCE;      // عامل

    public static RoleType fromString(String s) {
        if (s == null) return INVIGILATOR;
        String v = s.trim().toLowerCase();
        if (v.isEmpty()) return INVIGILATOR;
        // Arabic
        if (v.contains("مشرف") && v.contains("دور")) return FLOOR_SUPERVISOR;
        if (v.contains("ملاحظ")) return INVIGILATOR;
        if (v.contains("عامل")) return MAINTENANCE;
        // English
        if (v.contains("floor")) return FLOOR_SUPERVISOR;
        if (v.contains("invigil")) return INVIGILATOR;
        if (v.contains("maintenance")) return MAINTENANCE;
        return INVIGILATOR;
    }
}



