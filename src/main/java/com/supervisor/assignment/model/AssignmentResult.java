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

    public static class SupervisorTotals {
        private String supervisor;
        private double totalHours;
        private int sessionsCount;
        private double deviationFromTarget;
        private Map<DayOfWeekEnum, Double> perDayHours = new EnumMap<>(DayOfWeekEnum.class);
        private Double maxHoursConfigured;
        private String notes;

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
    }

    private List<SessionAssignment> sessionAssignments = new ArrayList<>();
    private List<SupervisorTotals> supervisorTotals = new ArrayList<>();
    private double targetHoursPerSupervisor;
    private double totalHoursNeeded;
    private List<String> warnings = new ArrayList<>();

    public List<SessionAssignment> getSessionAssignments() { return sessionAssignments; }
    public List<SupervisorTotals> getSupervisorTotals() { return supervisorTotals; }
    public double getTargetHoursPerSupervisor() { return targetHoursPerSupervisor; }
    public void setTargetHoursPerSupervisor(double targetHoursPerSupervisor) { this.targetHoursPerSupervisor = targetHoursPerSupervisor; }
    public double getTotalHoursNeeded() { return totalHoursNeeded; }
    public void setTotalHoursNeeded(double totalHoursNeeded) { this.totalHoursNeeded = totalHoursNeeded; }
    public List<String> getWarnings() { return warnings; }
}


