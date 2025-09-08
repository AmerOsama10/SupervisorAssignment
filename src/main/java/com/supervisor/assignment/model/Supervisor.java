package com.supervisor.assignment.model;

import java.util.*;

public class Supervisor {
    private String name;
    private Set<DayOfWeekEnum> availableDays = new HashSet<>();
    private Double maxHours; // optional (kept for backward-compat)
    private Double loadPercentage; // النسبة: 0..100
    private RoleType role = RoleType.INVIGILATOR; // النوع: مشرف دور أو ملاحظ

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<DayOfWeekEnum> getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(Set<DayOfWeekEnum> availableDays) {
        this.availableDays = availableDays;
    }

    public Double getMaxHours() {
        return maxHours;
    }

    public void setMaxHours(Double maxHours) {
        this.maxHours = maxHours;
    }

    public Double getLoadPercentage() {
        return loadPercentage;
    }

    public void setLoadPercentage(Double loadPercentage) {
        this.loadPercentage = loadPercentage;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }
}


