package com.supervisor.assignment.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class SubjectSession {
    private String id;
    private String subjectName;
    private DayOfWeekEnum day;
    private LocalDate date; // new explicit date
    private LocalTime from;
    private LocalTime to;
    private int supervisorsRequired;
    private String notes;
    private String building; // المبنى
    private PeriodOfDay period; // الفترة: صباحي/مسائي
    private RoleType requiredRole = RoleType.INVIGILATOR; // الدور المطلوب لهذه الجلسة

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public DayOfWeekEnum getDay() {
        return day;
    }

    public void setDay(DayOfWeekEnum day) {
        this.day = day;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getFrom() {
        return from;
    }

    public void setFrom(LocalTime from) {
        this.from = from;
    }

    public LocalTime getTo() {
        return to;
    }

    public void setTo(LocalTime to) {
        this.to = to;
    }

    public int getSupervisorsRequired() {
        return supervisorsRequired;
    }

    public void setSupervisorsRequired(int supervisorsRequired) {
        this.supervisorsRequired = supervisorsRequired;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public double getDurationHours() {
        long minutes = java.time.Duration.between(from, to).toMinutes();
        return minutes / 60.0;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public PeriodOfDay getPeriod() {
        return period;
    }

    public void setPeriod(PeriodOfDay period) {
        this.period = period;
    }

    public RoleType getRequiredRole() {
        return requiredRole;
    }

    public void setRequiredRole(RoleType requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubjectSession)) return false;
        SubjectSession that = (SubjectSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}


