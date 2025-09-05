package com.supervisor.assignment.model;

import java.util.*;

public class Supervisor {
    private String name;
    private Set<DayOfWeekEnum> availableDays = new HashSet<>();
    private Double maxHours; // optional

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
}


