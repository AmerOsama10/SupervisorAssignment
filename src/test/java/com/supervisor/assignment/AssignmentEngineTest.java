package com.supervisor.assignment;

import com.supervisor.assignment.logic.AssignmentEngine;
import com.supervisor.assignment.model.*;
import org.junit.jupiter.api.Test;

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

    private Supervisor makeSupervisor(String name, DayOfWeekEnum... days) {
        Supervisor s = new Supervisor();
        s.setName(name);
        Set<DayOfWeekEnum> set = new HashSet<>(Arrays.asList(days));
        s.setAvailableDays(set);
        return s;
    }
}


