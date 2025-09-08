package com.supervisor.assignment.model;

public class Config {
    private int timeGranularityMinutes = 30;
    private double fairnessToleranceHours = 1.0;
    private int defaultSupervisorsPerSubject = 2;
    private boolean allowExceedMaxHours = false;
    private boolean allowCrossDay = false;
    private boolean preserveManualOverrides = false;
    private SchedulingMode schedulingMode = SchedulingMode.MIXED;

    public int getTimeGranularityMinutes() {
        return timeGranularityMinutes;
    }

    public void setTimeGranularityMinutes(int timeGranularityMinutes) {
        this.timeGranularityMinutes = timeGranularityMinutes;
    }

    public double getFairnessToleranceHours() {
        return fairnessToleranceHours;
    }

    public void setFairnessToleranceHours(double fairnessToleranceHours) {
        this.fairnessToleranceHours = fairnessToleranceHours;
    }

    public int getDefaultSupervisorsPerSubject() {
        return defaultSupervisorsPerSubject;
    }

    public void setDefaultSupervisorsPerSubject(int defaultSupervisorsPerSubject) {
        this.defaultSupervisorsPerSubject = defaultSupervisorsPerSubject;
    }

    public boolean isAllowExceedMaxHours() {
        return allowExceedMaxHours;
    }

    public void setAllowExceedMaxHours(boolean allowExceedMaxHours) {
        this.allowExceedMaxHours = allowExceedMaxHours;
    }

    public boolean isAllowCrossDay() {
        return allowCrossDay;
    }

    public void setAllowCrossDay(boolean allowCrossDay) {
        this.allowCrossDay = allowCrossDay;
    }

    public boolean isPreserveManualOverrides() {
        return preserveManualOverrides;
    }

    public void setPreserveManualOverrides(boolean preserveManualOverrides) {
        this.preserveManualOverrides = preserveManualOverrides;
    }

    public SchedulingMode getSchedulingMode() {
        return schedulingMode;
    }

    public void setSchedulingMode(SchedulingMode schedulingMode) {
        this.schedulingMode = schedulingMode;
    }
}


