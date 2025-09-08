package com.supervisor.assignment.model;

import java.util.*;

public class AssignmentResult {
    public static class SessionAssignment {
        private SubjectSession session;
        private List<String> assignedSupervisors = new ArrayList<>();
        private String status; // Assigned / PartiallyAssigned / Unassigned
        private String reason; // if not fully assigned

        public SubjectSession getSession() {
            return session;
        }

        public void setSession(SubjectSession session) {
            this.session = session;
        }

        public List<String> getAssignedSupervisors() {
            return assignedSupervisors;
        }

        public void setAssignedSupervisors(List<String> assignedSupervisors) {
            this.assignedSupervisors = assignedSupervisors;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class BackupAssignment {
        private java.time.LocalDate date;
        private PeriodOfDay period;
        private RoleType role;
        private String supervisor;
        private String building; // optional for floor supervisors grouping
        private String sessionId; // session identifier for which backup is assigned
        private String subject;   // subject name for which backup is assigned

        public java.time.LocalDate getDate() { return date; }
        public void setDate(java.time.LocalDate date) { this.date = date; }
        public PeriodOfDay getPeriod() { return period; }
        public void setPeriod(PeriodOfDay period) { this.period = period; }
        public RoleType getRole() { return role; }
        public void setRole(RoleType role) { this.role = role; }
        public String getSupervisor() { return supervisor; }
        public void setSupervisor(String supervisor) { this.supervisor = supervisor; }
        public String getBuilding() { return building; }
        public void setBuilding(String building) { this.building = building; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
    }

    public static class SupervisorTotals {
        private String supervisor;
        private double totalHours;
        private int sessionsCount;
        private double deviationFromTarget;
        private Map<DayOfWeekEnum, Double> perDayHours = new EnumMap<>(DayOfWeekEnum.class);
        private Double maxHoursConfigured;
        private String notes;
        private double expectedHours;
        private RoleType role;
        private Double loadPercentage;
        private double primaryHours;
        private double backupHours;

        public String getSupervisor() { return supervisor; }
        public void setSupervisor(String supervisor) { this.supervisor = supervisor; }
        public double getTotalHours() { return totalHours; }
        public void setTotalHours(double totalHours) { this.totalHours = totalHours; }
        public int getSessionsCount() { return sessionsCount; }
        public void setSessionsCount(int sessionsCount) { this.sessionsCount = sessionsCount; }
        public double getDeviationFromTarget() { return deviationFromTarget; }
        public void setDeviationFromTarget(double deviationFromTarget) { this.deviationFromTarget = deviationFromTarget; }
        public Map<DayOfWeekEnum, Double> getPerDayHours() { return perDayHours; }
        public void setPerDayHours(Map<DayOfWeekEnum, Double> perDayHours) { this.perDayHours = perDayHours; }
        public Double getMaxHoursConfigured() { return maxHoursConfigured; }
        public void setMaxHoursConfigured(Double maxHoursConfigured) { this.maxHoursConfigured = maxHoursConfigured; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public double getExpectedHours() { return expectedHours; }
        public void setExpectedHours(double expectedHours) { this.expectedHours = expectedHours; }
        public RoleType getRole() { return role; }
        public void setRole(RoleType role) { this.role = role; }
        public Double getLoadPercentage() { return loadPercentage; }
        public void setLoadPercentage(Double loadPercentage) { this.loadPercentage = loadPercentage; }
        public double getPrimaryHours() { return primaryHours; }
        public void setPrimaryHours(double primaryHours) { this.primaryHours = primaryHours; }
        public double getBackupHours() { return backupHours; }
        public void setBackupHours(double backupHours) { this.backupHours = backupHours; }
    }

    private List<SessionAssignment> sessionAssignments = new ArrayList<>();
    private List<SupervisorTotals> supervisorTotals = new ArrayList<>();
    private double targetHoursPerSupervisor;
    private double totalHoursNeeded;
    private List<String> warnings = new ArrayList<>();
    private List<BackupAssignment> backupAssignments = new ArrayList<>();

    public List<SessionAssignment> getSessionAssignments() { return sessionAssignments; }
    public List<SupervisorTotals> getSupervisorTotals() { return supervisorTotals; }
    public double getTargetHoursPerSupervisor() { return targetHoursPerSupervisor; }
    public void setTargetHoursPerSupervisor(double targetHoursPerSupervisor) { this.targetHoursPerSupervisor = targetHoursPerSupervisor; }
    public double getTotalHoursNeeded() { return totalHoursNeeded; }
    public void setTotalHoursNeeded(double totalHoursNeeded) { this.totalHoursNeeded = totalHoursNeeded; }
    public List<String> getWarnings() { return warnings; }
    public List<BackupAssignment> getBackupAssignments() { return backupAssignments; }
}


