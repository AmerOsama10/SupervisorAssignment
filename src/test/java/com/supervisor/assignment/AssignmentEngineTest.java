package com.supervisor.assignment;

import com.supervisor.assignment.logic.AssignmentEngine;
import com.supervisor.assignment.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AssignmentEngineTest {
    @Test
    public void smallBalancedCase() {
        List<SubjectSession> sessions = new ArrayList<>();
        sessions.add(makeSession("S1", "Math", DayOfWeekEnum.SATURDAY, "10:00", "12:00", 1));
        sessions.add(makeSession("S2", "Physics", DayOfWeekEnum.SATURDAY, "12:00", "13:30", 1));
        sessions.add(makeSession("S3", "Chem", DayOfWeekEnum.SATURDAY, "13:30", "14:30", 1));

        List<Supervisor> supervisors = new ArrayList<>();
        supervisors.add(makeSupervisor("A", DayOfWeekEnum.SATURDAY));
        supervisors.add(makeSupervisor("B", DayOfWeekEnum.SATURDAY));
        supervisors.add(makeSupervisor("C", DayOfWeekEnum.SATURDAY));

        AssignmentEngine engine = new AssignmentEngine();
        AssignmentResult result = engine.assign(sessions, supervisors, new Config());

        long assigned = result.getSessionAssignments().stream().filter(a -> "Assigned".equals(a.getStatus())).count();
        assertEquals(3, assigned);
        assertEquals(3, result.getSupervisorTotals().size());
    }

    @Test
    public void roleFilteringAssignsFloorSupervisorOnly() {
        List<SubjectSession> sessions = new ArrayList<>();
        SubjectSession fs = makeSessionWithDate("F1", "Floor", DayOfWeekEnum.SATURDAY, LocalDate.now(), "10:00", "12:00", 1);
        fs.setRequiredRole(RoleType.FLOOR_SUPERVISOR);
        sessions.add(fs);

        List<Supervisor> supervisors = new ArrayList<>();
        Supervisor inv = makeSupervisor("Inv1", DayOfWeekEnum.SATURDAY);
        inv.setRole(RoleType.INVIGILATOR);
        supervisors.add(inv);
        Supervisor floor = makeSupervisor("Floor1", DayOfWeekEnum.SATURDAY);
        floor.setRole(RoleType.FLOOR_SUPERVISOR);
        supervisors.add(floor);

        AssignmentEngine engine = new AssignmentEngine();
        AssignmentResult result = engine.assign(sessions, supervisors, new Config());

        AssignmentResult.SessionAssignment sa = result.getSessionAssignments().get(0);
        assertEquals("Assigned", sa.getStatus());
        assertEquals(1, sa.getAssignedSupervisors().size());
        assertEquals("Floor1", sa.getAssignedSupervisors().get(0));
    }

    @Test
    public void maxTwoConsecutiveDaysRespected() {
        List<SubjectSession> sessions = new ArrayList<>();
        LocalDate sat = next(java.time.DayOfWeek.SATURDAY);
        LocalDate sun = sat.plusDays(1);
        LocalDate mon = sat.plusDays(2);
        sessions.add(makeSessionWithDate("S1", "Sub1", DayOfWeekEnum.SATURDAY, sat, "08:00", "10:00", 1));
        sessions.add(makeSessionWithDate("S2", "Sub2", DayOfWeekEnum.SUNDAY, sun, "08:00", "10:00", 1));
        sessions.add(makeSessionWithDate("S3", "Sub3", DayOfWeekEnum.MONDAY, mon, "08:00", "10:00", 1));

        List<Supervisor> supervisors = new ArrayList<>();
        supervisors.add(makeSupervisor("A", DayOfWeekEnum.SATURDAY, DayOfWeekEnum.SUNDAY, DayOfWeekEnum.MONDAY));
        supervisors.add(makeSupervisor("B", DayOfWeekEnum.SATURDAY, DayOfWeekEnum.SUNDAY, DayOfWeekEnum.MONDAY));

        AssignmentEngine engine = new AssignmentEngine();
        AssignmentResult result = engine.assign(sessions, supervisors, new Config());

        // All sessions should be assigned
        long assigned = result.getSessionAssignments().stream().filter(a -> "Assigned".equals(a.getStatus())).count();
        assertEquals(3, assigned);

        // No supervisor should be assigned on all 3 consecutive days
        Map<String, Integer> counts = new HashMap<>();
        for (AssignmentResult.SessionAssignment sa : result.getSessionAssignments()) {
            for (String name : sa.getAssignedSupervisors()) {
                counts.put(name, counts.getOrDefault(name, 0) + 1);
            }
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            assertTrue(e.getValue() <= 2, "Supervisor assigned 3 consecutive days: " + e.getKey());
        }
    }

    private SubjectSession makeSession(String id, String name, DayOfWeekEnum day, String from, String to, int req) {
        SubjectSession s = new SubjectSession();
        s.setId(id);
        s.setSubjectName(name);
        s.setDay(day);
        s.setFrom(LocalTime.parse(from));
        s.setTo(LocalTime.parse(to));
        s.setSupervisorsRequired(req);
        return s;
    }

    private SubjectSession makeSessionWithDate(String id, String name, DayOfWeekEnum day, LocalDate date, String from, String to, int req) {
        SubjectSession s = makeSession(id, name, day, from, to, req);
        s.setDate(date);
        return s;
    }

    private Supervisor makeSupervisor(String name, DayOfWeekEnum... days) {
        Supervisor s = new Supervisor();
        s.setName(name);
        Set<DayOfWeekEnum> set = new HashSet<>(Arrays.asList(days));
        s.setAvailableDays(set);
        return s;
    }

    private static LocalDate next(java.time.DayOfWeek dow) {
        LocalDate d = LocalDate.now();
        while (d.getDayOfWeek() != dow) d = d.plusDays(1);
        return d;
    }
}


