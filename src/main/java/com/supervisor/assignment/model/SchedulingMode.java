package com.supervisor.assignment.model;

public enum SchedulingMode {
    CONSECUTIVE, // prefer back-to-back on same day
    BREAK,       // avoid back-to-back on same day
    MIXED        // neutral
}


