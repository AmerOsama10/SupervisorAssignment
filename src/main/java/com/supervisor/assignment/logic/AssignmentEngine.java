package com.supervisor.assignment.logic;

import com.supervisor.assignment.model.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class AssignmentEngine {

	public AssignmentResult assign(List<SubjectSession> sessions, List<Supervisor> supervisors, Config config) {
		AssignmentResult result = new AssignmentResult();
		if (sessions == null || supervisors == null) {
			throw new IllegalArgumentException("Sessions and supervisors must not be null");
		}
		// Precompute total hours and target
		double totalHoursNeeded = sessions.stream()
			.mapToDouble(s -> s.getDurationHours() * Math.max(1, s.getSupervisorsRequired()))
			.sum();
		result.setTotalHoursNeeded(totalHoursNeeded);
		double targetPerSupervisor = supervisors.isEmpty() ? 0.0 : totalHoursNeeded / supervisors.size();
		result.setTargetHoursPerSupervisor(targetPerSupervisor);

		// Build per-supervisor state
		Map<String, Double> supervisorToTotalHours = new HashMap<>();
		Map<String, Map<LocalDate, List<LocalTime[]>>> schedule = new HashMap<>();
		for (Supervisor sup : supervisors) {
			supervisorToTotalHours.put(sup.getName(), 0.0);
			schedule.put(sup.getName(), new HashMap<>());
		}

		// Expand session slots
		List<SubjectSession> slots = new ArrayList<>();
		for (SubjectSession s : sessions) {
			int k = Math.max(1, s.getSupervisorsRequired());
			for (int i = 0; i < k; i++) slots.add(s);
		}

		// Precompute eligibility counts
		Map<SubjectSession, Integer> eligibilityCount = new HashMap<>();
		for (SubjectSession s : sessions) {
			int eligible = (int) supervisors.stream().filter(sup -> isEligible(s, sup, config)).count();
			eligibilityCount.put(s, eligible);
		}

		// Sort by duration desc, then eligibility asc
		slots.sort((a, b) -> {
			int cmp = Double.compare(b.getDurationHours(), a.getDurationHours());
			if (cmp != 0) return cmp;
			return Integer.compare(eligibilityCount.getOrDefault(a, Integer.MAX_VALUE), eligibilityCount.getOrDefault(b, Integer.MAX_VALUE));
		});

		Map<SubjectSession, List<String>> tempAssignments = new HashMap<>();

		for (SubjectSession slot : slots) {
			List<Supervisor> eligible = supervisors.stream()
				.filter(sup -> isEligible(slot, sup, config))
				.filter(sup -> !hasOverlap(schedule.get(sup.getName()), slot))
				.filter(sup -> withinMaxHours(supervisorToTotalHours.get(sup.getName()), slot, sup, config))
				.collect(Collectors.toList());
			if (eligible.isEmpty()) {
				// Fallback: relax availability and max-hours to ensure coverage, but still avoid overlaps
				eligible = supervisors.stream()
					.filter(sup -> !hasOverlap(schedule.get(sup.getName()), slot))
					.collect(Collectors.toList());
				if (eligible.isEmpty()) {
					tempAssignments.computeIfAbsent(slot, k -> new ArrayList<>());
					continue;
				}
			}

			eligible.sort(Comparator
				.comparingDouble((Supervisor s) -> supervisorToTotalHours.get(s.getName()))
				.thenComparing((Supervisor s) -> schedulePreferenceScore(schedule.get(s.getName()), slot, config))
				.thenComparing(Supervisor::getName));

			Supervisor chosen = eligible.get(0);
			tempAssignments.computeIfAbsent(slot, k -> new ArrayList<>()).add(chosen.getName());
			double newHours = supervisorToTotalHours.get(chosen.getName()) + slot.getDurationHours();
			supervisorToTotalHours.put(chosen.getName(), newHours);
			Map<LocalDate, List<LocalTime[]>> dayMap = schedule.get(chosen.getName());
			LocalDate dateKey = slot.getDate() != null ? slot.getDate() : LocalDate.now();
			dayMap.computeIfAbsent(dateKey, d -> new ArrayList<>()).add(new LocalTime[]{ slot.getFrom(), slot.getTo() });
		}

		// Backfill any sessions lacking required supervisors by relaxing availability/max-hours but preserving no-overlap
		for (SubjectSession s : sessions) {
			int required = Math.max(1, s.getSupervisorsRequired());
			List<String> assigned = tempAssignments.entrySet().stream()
				.filter(e -> e.getKey().equals(s))
				.flatMap(e -> e.getValue().stream())
				.collect(Collectors.toList());
			Set<String> assignedSet = new HashSet<>(assigned);
			while (assignedSet.size() < required) {
				List<Supervisor> candidates = supervisors.stream()
					.filter(sup -> !assignedSet.contains(sup.getName()))
					.filter(sup -> !hasOverlap(schedule.get(sup.getName()), s))
					.sorted(Comparator
						.comparingDouble((Supervisor sup) -> supervisorToTotalHours.get(sup.getName()))
						.thenComparing((Supervisor sup) -> schedulePreferenceScore(schedule.get(sup.getName()), s, config))
						.thenComparing(Supervisor::getName))
					.collect(Collectors.toList());
				if (candidates.isEmpty()) break; // cannot fill further without overlaps
				Supervisor chosen = candidates.get(0);
				tempAssignments.computeIfAbsent(s, k -> new ArrayList<>()).add(chosen.getName());
				assignedSet.add(chosen.getName());
				double newHours = supervisorToTotalHours.get(chosen.getName()) + s.getDurationHours();
				supervisorToTotalHours.put(chosen.getName(), newHours);
				Map<LocalDate, List<LocalTime[]>> dayMap = schedule.get(chosen.getName());
				LocalDate dateKey = s.getDate() != null ? s.getDate() : LocalDate.now();
				dayMap.computeIfAbsent(dateKey, d -> new ArrayList<>()).add(new LocalTime[]{ s.getFrom(), s.getTo() });
			}
		}

		// Consolidate per-session results
		for (SubjectSession s : sessions) {
			AssignmentResult.SessionAssignment sa = new AssignmentResult.SessionAssignment();
			sa.setSession(s);
			List<String> assigned = tempAssignments.entrySet().stream()
				.filter(e -> e.getKey().equals(s))
				.flatMap(e -> e.getValue().stream())
				.distinct()
				.collect(Collectors.toList());
			int req = Math.max(1, s.getSupervisorsRequired());
			if (assigned.size() == req) {
				sa.setStatus("Assigned");
			} else if (assigned.size() > 0) {
				sa.setStatus("PartiallyAssigned");
				sa.setReason("Insufficient eligible supervisors or conflicts");
			} else {
				// After backfill, try to force assign one minimal-overlap candidate (still avoiding overlaps may leave none)
				List<Supervisor> candidates = supervisors.stream()
					.filter(sup -> !hasOverlap(schedule.get(sup.getName()), s))
					.sorted(Comparator.comparingDouble((Supervisor sup) -> supervisorToTotalHours.get(sup.getName())))
					.collect(Collectors.toList());
				if (!candidates.isEmpty()) {
					Supervisor chosen = candidates.get(0);
					assigned = new ArrayList<>();
					assigned.add(chosen.getName());
					sa.setAssignedSupervisors(assigned);
					sa.setStatus("PartiallyAssigned");
					sa.setReason("Auto backfill due to constraints");
					double newHours = supervisorToTotalHours.get(chosen.getName()) + s.getDurationHours();
					supervisorToTotalHours.put(chosen.getName(), newHours);
					Map<LocalDate, List<LocalTime[]>> dayMap = schedule.get(chosen.getName());
					LocalDate dateKey = s.getDate() != null ? s.getDate() : LocalDate.now();
					dayMap.computeIfAbsent(dateKey, d -> new ArrayList<>()).add(new LocalTime[]{ s.getFrom(), s.getTo() });
				} else {
					sa.setStatus("Unassigned");
					sa.setReason("No free supervisors (overlap constraint)");
				}
			}
			sa.setAssignedSupervisors(assigned);
			result.getSessionAssignments().add(sa);
		}

		// Totals
		for (Supervisor sup : supervisors) {
			AssignmentResult.SupervisorTotals totals = new AssignmentResult.SupervisorTotals();
			totals.setSupervisor(sup.getName());
			double hours = supervisorToTotalHours.getOrDefault(sup.getName(), 0.0);
			totals.setTotalHours(hours);
			totals.setDeviationFromTarget(hours - targetPerSupervisor);
			Map<DayOfWeekEnum, Double> dayHours = new EnumMap<>(DayOfWeekEnum.class);
			Map<LocalDate, List<LocalTime[]>> dayMap = schedule.get(sup.getName());
			int sessionsCount = 0;
			if (dayMap != null) {
				for (Map.Entry<LocalDate, List<LocalTime[]>> e : dayMap.entrySet()) {
					double h = e.getValue().stream()
						.mapToDouble(arr -> java.time.Duration.between(arr[0], arr[1]).toMinutes() / 60.0)
						.sum();
					DayOfWeekEnum dow = mapJavaDayToEnum(e.getKey().getDayOfWeek());
					dayHours.put(dow, dayHours.getOrDefault(dow, 0.0) + h);
					sessionsCount += e.getValue().size();
				}
			}
			totals.setSessionsCount(sessionsCount);
			totals.setPerDayHours(dayHours);
			totals.setMaxHoursConfigured(sup.getMaxHours());
			result.getSupervisorTotals().add(totals);
		}

		return result;
	}

	private boolean isEligible(SubjectSession s, Supervisor sup, Config config) {
		return sup.getAvailableDays().contains(s.getDay());
	}

	private boolean hasOverlap(Map<LocalDate, List<LocalTime[]>> dayMap, SubjectSession s) {
		if (dayMap == null) return false;
		LocalDate dateKey = s.getDate() != null ? s.getDate() : LocalDate.now();
		List<LocalTime[]> list = dayMap.get(dateKey);
		if (list == null) return false;
		for (LocalTime[] interval : list) {
			if (TimeUtil.overlaps(interval[0], interval[1], s.getFrom(), s.getTo())) return true;
		}
		return false;
	}

	private boolean withinMaxHours(double currentHours, SubjectSession slot, Supervisor sup, Config config) {
		if (sup.getMaxHours() == null) return true;
		double next = currentHours + slot.getDurationHours();
		if (next <= sup.getMaxHours()) return true;
		return config != null && config.isAllowExceedMaxHours();
	}

	private int schedulePreferenceScore(Map<LocalDate, List<LocalTime[]>> dayMap, SubjectSession slot, Config config) {
		if (dayMap == null) return 1;
		LocalDate dateKey = slot.getDate() != null ? slot.getDate() : LocalDate.now();
		List<LocalTime[]> list = dayMap.get(dateKey);
		if (list == null || list.isEmpty()) return 1;
		LocalTime start = slot.getFrom();
		LocalTime end = slot.getTo();
		boolean adjacent = list.stream().anyMatch(arr -> arr[1].equals(start) || arr[0].equals(end));
		if (config != null) {
			switch (config.getSchedulingMode()) {
				case CONSECUTIVE: return adjacent ? 0 : 1;
				case BREAK: return adjacent ? 2 : 0;
				case MIXED: default: return adjacent ? 0 : 1;
			}
		}
		return adjacent ? 0 : 1;
	}

	private DayOfWeekEnum mapJavaDayToEnum(java.time.DayOfWeek dow) {
		switch (dow) {
			case SATURDAY: return DayOfWeekEnum.SATURDAY;
			case SUNDAY: return DayOfWeekEnum.SUNDAY;
			case MONDAY: return DayOfWeekEnum.MONDAY;
			case TUESDAY: return DayOfWeekEnum.TUESDAY;
			case WEDNESDAY: return DayOfWeekEnum.WEDNESDAY;
			case THURSDAY: return DayOfWeekEnum.THURSDAY;
			case FRIDAY: return DayOfWeekEnum.FRIDAY;
			default: return null;
		}
	}
}


