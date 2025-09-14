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
		// Build floor-supervisor slots per (date, period, building)
		// المشرف الدور مسؤول عن المباني/الأدوار
		List<SubjectSession> floorSlots = new ArrayList<>();
		Map<String, List<SubjectSession>> byKey = sessions.stream()
			.filter(s -> s.getDate() != null && s.getPeriod() != null && s.getBuilding() != null)
			.collect(Collectors.groupingBy(s -> s.getDate() + "|" + s.getPeriod().name() + "|" + s.getBuilding()));
		int floorCounter = 1;
		for (Map.Entry<String, List<SubjectSession>> e : byKey.entrySet()) {
			List<SubjectSession> list = e.getValue();
			if (list.isEmpty()) continue;
			LocalDate date = list.get(0).getDate();
			DayOfWeekEnum day = list.get(0).getDay();
			LocalTime start = list.stream().map(SubjectSession::getFrom).min(LocalTime::compareTo).orElse(LocalTime.of(8, 0));
			LocalTime end = list.stream().map(SubjectSession::getTo).max(LocalTime::compareTo).orElse(LocalTime.of(16, 0));
			SubjectSession fs = new SubjectSession();
			fs.setId("F-" + (floorCounter++));
			fs.setSubjectName("مشرف دور - " + list.get(0).getBuilding());
			fs.setDay(day);
			fs.setDate(date);
			fs.setFrom(start);
			fs.setTo(end);
			fs.setSupervisorsRequired(1);
			fs.setBuilding(list.get(0).getBuilding());
			fs.setPeriod(list.get(0).getPeriod());
			fs.setRequiredRole(RoleType.FLOOR_SUPERVISOR);
			floorSlots.add(fs);
		}

		// Build maintenance staff daily assignments
		List<SubjectSession> maintenanceSlots = createMaintenanceSlots(sessions, supervisors);
		
		List<SubjectSession> allSessions = new ArrayList<>();
		allSessions.addAll(sessions);
		allSessions.addAll(floorSlots);
		allSessions.addAll(maintenanceSlots);

		double invigilatorHoursNeeded = sessions.stream()
			.mapToDouble(s -> s.getDurationHours() * Math.max(1, s.getSupervisorsRequired()))
			.sum();
		// Backups policy: for every invigilator hour of primaries, add same hours for backups (2 backups for each slot equals same total hours)
		double invigilatorHoursIncludingBackups = invigilatorHoursNeeded * 2.0;
		double floorHoursNeeded = floorSlots.stream()
			.mapToDouble(SubjectSession::getDurationHours)
			.sum();
		double maintenanceHoursNeeded = maintenanceSlots.stream()
			.mapToDouble(SubjectSession::getDurationHours)
			.sum();
		double totalHoursNeeded = invigilatorHoursNeeded + floorHoursNeeded + maintenanceHoursNeeded;
		result.setTotalHoursNeeded(totalHoursNeeded);
		double invLoadUnits = supervisors.stream()
			.filter(sup -> sup.getRole() == RoleType.INVIGILATOR)
			.mapToDouble(sup -> (sup.getLoadPercentage() == null ? 100.0 : sup.getLoadPercentage()) / 100.0)
			.sum();
		double floorLoadUnits = supervisors.stream()
			.filter(sup -> sup.getRole() == RoleType.FLOOR_SUPERVISOR)
			.mapToDouble(sup -> (sup.getLoadPercentage() == null ? 100.0 : sup.getLoadPercentage()) / 100.0)
			.sum();
		double maintenanceLoadUnits = supervisors.stream()
			.filter(sup -> sup.getRole() == RoleType.MAINTENANCE)
			.mapToDouble(sup -> (sup.getLoadPercentage() == null ? 100.0 : sup.getLoadPercentage()) / 100.0)
			.sum();
		double basePerInvigilatorUnit = invLoadUnits == 0.0 ? 0.0 : invigilatorHoursIncludingBackups / invLoadUnits;
		double basePerFloorUnit = floorLoadUnits == 0.0 ? 0.0 : (floorHoursNeeded * 2.0) / floorLoadUnits;
		double basePerMaintenanceUnit = maintenanceLoadUnits == 0.0 ? 0.0 : (maintenanceHoursNeeded * 2.0) / maintenanceLoadUnits;
		// keep a single target for logging; use invigilator target
		result.setTargetHoursPerSupervisor(basePerInvigilatorUnit);

		// Build per-supervisor state
		Map<String, Double> supervisorToTotalHours = new HashMap<>();
		Map<String, Map<LocalDate, List<LocalTime[]>>> schedule = new HashMap<>();
		Map<String, Double> supervisorExpectedHours = new HashMap<>();
		for (Supervisor sup : supervisors) {
			supervisorToTotalHours.put(sup.getName(), 0.0);
			schedule.put(sup.getName(), new HashMap<>());
			double units = (sup.getLoadPercentage() == null ? 1.0 : sup.getLoadPercentage() / 100.0);
			double expected;
			if (sup.getRole() == RoleType.FLOOR_SUPERVISOR) {
				expected = basePerFloorUnit * units;
			} else if (sup.getRole() == RoleType.MAINTENANCE) {
				expected = basePerMaintenanceUnit * units;
			} else {
				expected = basePerInvigilatorUnit * units;
			}
			supervisorExpectedHours.put(sup.getName(), expected);
		}

		final double FAIRNESS_CAP = 1.0; // prefer under or at 100% of expected
		java.util.function.Function<String, Double> loadRatio = name -> {
			double exp = Math.max(0.1, supervisorExpectedHours.getOrDefault(name, 0.0));
			return supervisorToTotalHours.getOrDefault(name, 0.0) / exp;
		};

		// Expand session slots
		List<SubjectSession> slots = new ArrayList<>();
		for (SubjectSession s : allSessions) {
			int k = Math.max(1, s.getSupervisorsRequired());
			for (int i = 0; i < k; i++) slots.add(s);
		}

		// Precompute eligibility counts
		Map<SubjectSession, Integer> eligibilityCount = new HashMap<>();
		for (SubjectSession s : allSessions) {
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
				.filter(sup -> withinConsecutiveDaysLimit(schedule.get(sup.getName()), slot))
				.collect(Collectors.toList());
			if (eligible.isEmpty()) {
				// Fallback: relax availability and max-hours to ensure coverage, but still avoid overlaps
				eligible = supervisors.stream()
					.filter(sup -> sup.getRole() == slot.getRequiredRole())
					.filter(sup -> !hasOverlap(schedule.get(sup.getName()), slot))
					.filter(sup -> withinConsecutiveDaysLimit(schedule.get(sup.getName()), slot))
					.collect(Collectors.toList());
				if (eligible.isEmpty()) {
					tempAssignments.computeIfAbsent(slot, k -> new ArrayList<>());
					continue;
				}
			}

			List<Supervisor> underCap = eligible.stream().filter(s -> loadRatio.apply(s.getName()) <= FAIRNESS_CAP).collect(Collectors.toList());
			List<Supervisor> pool = underCap.isEmpty() ? eligible : underCap;
			pool.sort((a, b) -> {
				double ra = loadRatio.apply(a.getName());
				double rb = loadRatio.apply(b.getName());
				int cmp = Double.compare(ra, rb);
				if (cmp != 0) return cmp;
				cmp = Integer.compare(schedulePreferenceScore(schedule.get(a.getName()), slot, config), schedulePreferenceScore(schedule.get(b.getName()), slot, config));
				if (cmp != 0) return cmp;
				return a.getName().compareTo(b.getName());
			});

			Supervisor chosen = pool.get(0);
			tempAssignments.computeIfAbsent(slot, k -> new ArrayList<>()).add(chosen.getName());
			double newHours = supervisorToTotalHours.get(chosen.getName()) + slot.getDurationHours();
			supervisorToTotalHours.put(chosen.getName(), newHours);
			Map<LocalDate, List<LocalTime[]>> dayMap = schedule.get(chosen.getName());
			LocalDate dateKey = slot.getDate() != null ? slot.getDate() : LocalDate.now();
			dayMap.computeIfAbsent(dateKey, d -> new ArrayList<>()).add(new LocalTime[]{ slot.getFrom(), slot.getTo() });
		}

		// Backfill any sessions lacking required supervisors by relaxing availability/max-hours but preserving no-overlap
		for (SubjectSession s : allSessions) {
			int required = Math.max(1, s.getSupervisorsRequired());
			List<String> assigned = tempAssignments.entrySet().stream()
				.filter(e -> e.getKey().equals(s))
				.flatMap(e -> e.getValue().stream())
				.collect(Collectors.toList());
			Set<String> assignedSet = new HashSet<>(assigned);
			while (assignedSet.size() < required) {
				List<Supervisor> candidates = supervisors.stream()
					.filter(sup -> !assignedSet.contains(sup.getName()))
					.filter(sup -> sup.getRole() == s.getRequiredRole())
					.filter(sup -> !hasOverlap(schedule.get(sup.getName()), s))
					.filter(sup -> withinConsecutiveDaysLimit(schedule.get(sup.getName()), s))
					.collect(Collectors.toList());
				List<Supervisor> underCap = candidates.stream().filter(x -> loadRatio.apply(x.getName()) <= FAIRNESS_CAP).collect(Collectors.toList());
				List<Supervisor> pool = underCap.isEmpty() ? candidates : underCap;
				pool = pool.stream()
					.sorted(Comparator
							.comparingDouble((Supervisor sup) -> loadRatio.apply(sup.getName()))
							.thenComparing((Supervisor sup) -> schedulePreferenceScore(schedule.get(sup.getName()), s, config))
							.thenComparing(Supervisor::getName))
					.collect(Collectors.toList());
				if (pool.isEmpty()) break; // cannot fill further without overlaps
				Supervisor chosen = pool.get(0);
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
		for (SubjectSession s : allSessions) {
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
					.filter(sup -> sup.getRole() == s.getRequiredRole())
					.filter(sup -> !hasOverlap(schedule.get(sup.getName()), s))
					.filter(sup -> withinConsecutiveDaysLimit(schedule.get(sup.getName()), s))
					.collect(Collectors.toList());
				List<Supervisor> underCap = candidates.stream().filter(x -> loadRatio.apply(x.getName()) <= FAIRNESS_CAP).collect(Collectors.toList());
				List<Supervisor> pool = underCap.isEmpty() ? candidates : underCap;
				pool = pool.stream()
					.sorted(Comparator.comparingDouble((Supervisor sup) -> loadRatio.apply(sup.getName())))
					.collect(Collectors.toList());
				if (!pool.isEmpty()) {
					Supervisor chosen = pool.get(0);
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

		// Generate backups (احتياطي): for each session, assign backups equal to required count (distinct and not assigned as primary on same session/date)
		for (SubjectSession s : sessions) {
			LocalDate date = s.getDate();
			PeriodOfDay p = s.getPeriod();
			if (date == null || p == null) continue;
			Set<String> primaries = result.getSessionAssignments().stream()
				.filter(sa -> sa.getSession().equals(s))
				.flatMap(sa -> sa.getAssignedSupervisors().stream())
				.collect(java.util.stream.Collectors.toSet());
			List<Supervisor> candidates = supervisors.stream()
				.filter(sup -> sup.getRole() == RoleType.INVIGILATOR)
				.filter(sup -> !primaries.contains(sup.getName()))
				.filter(sup -> sup.getAvailableDays().contains(mapJavaDayToEnum(date.getDayOfWeek())))
				.filter(sup -> !hasOverlap(schedule.get(sup.getName()), s))
				.filter(sup -> withinConsecutiveDaysLimit(schedule.get(sup.getName()), date))
				.collect(Collectors.toList());
			List<Supervisor> underCap = candidates.stream().filter(x -> loadRatio.apply(x.getName()) <= FAIRNESS_CAP).collect(Collectors.toList());
			List<Supervisor> pool = underCap.isEmpty() ? candidates : underCap;
			pool = pool.stream()
				.sorted(Comparator.comparingDouble((Supervisor sup) -> loadRatio.apply(sup.getName())))
				.collect(Collectors.toList());
			int neededBackups = Math.max(0, s.getSupervisorsRequired());
			int added = 0;
			for (Supervisor sup : pool) {
				AssignmentResult.BackupAssignment ba = new AssignmentResult.BackupAssignment();
				ba.setDate(date);
				ba.setPeriod(p);
				ba.setRole(RoleType.INVIGILATOR);
				ba.setSupervisor(sup.getName());
				ba.setBuilding(s.getBuilding());
				ba.setSessionId(s.getId());
				ba.setSubject(s.getSubjectName());
				result.getBackupAssignments().add(ba);
				Map<LocalDate, List<LocalTime[]>> dayMap = schedule.get(sup.getName());
				dayMap.computeIfAbsent(date, d -> new ArrayList<>()).add(new LocalTime[]{ s.getFrom(), s.getTo() });
				supervisorToTotalHours.put(sup.getName(), supervisorToTotalHours.getOrDefault(sup.getName(), 0.0) + s.getDurationHours());
				added++;
				if (added >= neededBackups) break;
			}
		}

		// Backups for floor supervisors: exactly 1
		for (SubjectSession s : floorSlots) {
			LocalDate date = s.getDate();
			PeriodOfDay p = s.getPeriod();
			if (date == null || p == null) continue;
			Set<String> primaries = result.getSessionAssignments().stream()
				.filter(sa -> sa.getSession().equals(s))
				.flatMap(sa -> sa.getAssignedSupervisors().stream())
				.collect(java.util.stream.Collectors.toSet());
			List<Supervisor> candidates = supervisors.stream()
				.filter(sup -> sup.getRole() == RoleType.FLOOR_SUPERVISOR)
				.filter(sup -> !primaries.contains(sup.getName()))
				.filter(sup -> sup.getAvailableDays().contains(mapJavaDayToEnum(date.getDayOfWeek())))
				.filter(sup -> !hasOverlap(schedule.get(sup.getName()), s))
				.filter(sup -> withinConsecutiveDaysLimit(schedule.get(sup.getName()), date))
				.collect(Collectors.toList());
			List<Supervisor> underCap = candidates.stream().filter(x -> loadRatio.apply(x.getName()) <= FAIRNESS_CAP).collect(Collectors.toList());
			List<Supervisor> pool = underCap.isEmpty() ? candidates : underCap;
			pool = pool.stream()
				.sorted(Comparator.comparingDouble((Supervisor sup) -> loadRatio.apply(sup.getName())))
				.collect(Collectors.toList());
			int added = 0;
			for (Supervisor sup : pool) {
				AssignmentResult.BackupAssignment ba = new AssignmentResult.BackupAssignment();
				ba.setDate(date);
				ba.setPeriod(p);
				ba.setRole(RoleType.FLOOR_SUPERVISOR);
				ba.setSupervisor(sup.getName());
				ba.setBuilding(s.getBuilding());
				ba.setSessionId(s.getId());
				ba.setSubject(s.getSubjectName());
				result.getBackupAssignments().add(ba);
				Map<LocalDate, List<LocalTime[]>> dayMap = schedule.get(sup.getName());
				dayMap.computeIfAbsent(date, d -> new ArrayList<>()).add(new LocalTime[]{ s.getFrom(), s.getTo() });
				supervisorToTotalHours.put(sup.getName(), supervisorToTotalHours.getOrDefault(sup.getName(), 0.0) + s.getDurationHours());
				added++;
				if (added >= 1) break;
			}
		}

		// Totals (after backups so backup hours are counted and split)
		for (Supervisor sup : supervisors) {
			AssignmentResult.SupervisorTotals totals = new AssignmentResult.SupervisorTotals();
			totals.setSupervisor(sup.getName());
			double primaryHours = 0.0;
			double backupHours = 0.0;
			for (AssignmentResult.SessionAssignment sa : result.getSessionAssignments()) {
				if (sa.getAssignedSupervisors().contains(sup.getName())) {
					primaryHours += sa.getSession().getDurationHours();
				}
			}
			for (AssignmentResult.BackupAssignment ba : result.getBackupAssignments()) {
				if (sup.getName().equals(ba.getSupervisor())) {
					SubjectSession ss = sessions.stream().filter(x -> x.getId().equals(ba.getSessionId())).findFirst().orElse(null);
					if (ss == null) ss = floorSlots.stream().filter(x -> x.getId().equals(ba.getSessionId())).findFirst().orElse(null);
					if (ss != null) backupHours += ss.getDurationHours();
				}
			}
			double totalH = primaryHours + backupHours;
			totals.setPrimaryHours(primaryHours);
			totals.setBackupHours(backupHours);
			totals.setTotalHours(totalH);
			double expected = supervisorExpectedHours.getOrDefault(sup.getName(), 0.0);
			totals.setExpectedHours(expected);
			totals.setDeviationFromTarget(totalH - expected);
			totals.setRole(sup.getRole());
			totals.setLoadPercentage(sup.getLoadPercentage() == null ? 100.0 : sup.getLoadPercentage());
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
			totals.setMaxHoursConfigured(null);
			result.getSupervisorTotals().add(totals);
		}

		return result;
	}

	private boolean isEligible(SubjectSession s, Supervisor sup, Config config) {
		if (sup.getRole() != s.getRequiredRole()) return false;
		
		// Check if supervisor is excluded from this subject
		if (sup.getExcludedSubjects().contains(s.getSubjectName())) {
			return false;
		}
		
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

	private boolean withinConsecutiveDaysLimit(Map<LocalDate, List<LocalTime[]>> dayMap, SubjectSession slot) {
		if (dayMap == null) return true;
		LocalDate d = slot.getDate();
		if (d == null) return true;
		boolean prev1 = dayMap.containsKey(d.minusDays(1)) && !dayMap.get(d.minusDays(1)).isEmpty();
		boolean prev2 = dayMap.containsKey(d.minusDays(2)) && !dayMap.get(d.minusDays(2)).isEmpty();
		if (prev1 && prev2) return false;
		boolean next1 = dayMap.containsKey(d.plusDays(1)) && !dayMap.get(d.plusDays(1)).isEmpty();
		if (prev1 && next1) return false;
		return true;
	}

	private boolean withinConsecutiveDaysLimit(Map<LocalDate, List<LocalTime[]>> dayMap, LocalDate d) {
		if (dayMap == null) return true;
		boolean prev1 = dayMap.containsKey(d.minusDays(1)) && !dayMap.get(d.minusDays(1)).isEmpty();
		boolean prev2 = dayMap.containsKey(d.minusDays(2)) && !dayMap.get(d.minusDays(2)).isEmpty();
		if (prev1 && prev2) return false;
		boolean next1 = dayMap.containsKey(d.plusDays(1)) && !dayMap.get(d.plusDays(1)).isEmpty();
		if (prev1 && next1) return false;
		return true;
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

	private List<SubjectSession> createMaintenanceSlots(List<SubjectSession> sessions, List<Supervisor> supervisors) {
		List<SubjectSession> maintenanceSlots = new ArrayList<>();
		List<Supervisor> maintenanceStaff = supervisors.stream()
			.filter(sup -> sup.getRole() == RoleType.MAINTENANCE)
			.collect(Collectors.toList());
		
		if (maintenanceStaff.isEmpty()) {
			return maintenanceSlots;
		}
		
		// العامل عامل تنظيمي في الحوش - واحد كل يوم للمدرسة كلها
		Set<LocalDate> workingDates = sessions.stream()
			.map(SubjectSession::getDate)
			.filter(date -> date != null && date.getDayOfWeek() != java.time.DayOfWeek.FRIDAY)
			.collect(Collectors.toSet());
		
		int maintenanceCounter = 1;
		for (LocalDate date : workingDates) {
			// Create one maintenance worker per day for the whole school
			SubjectSession maintenanceSlot = new SubjectSession();
			maintenanceSlot.setId("M-" + (maintenanceCounter++));
			maintenanceSlot.setSubjectName("عامل تنظيمي - " + date.toString());
			maintenanceSlot.setDay(mapJavaDayToEnum(date.getDayOfWeek()));
			maintenanceSlot.setDate(date);
			maintenanceSlot.setFrom(LocalTime.of(8, 0)); // 8 AM to 4 PM
			maintenanceSlot.setTo(LocalTime.of(16, 0));
			maintenanceSlot.setSupervisorsRequired(1);
			maintenanceSlot.setBuilding("الحوش/المدرسة كلها");
			maintenanceSlot.setPeriod(PeriodOfDay.MORNING);
			maintenanceSlot.setRequiredRole(RoleType.MAINTENANCE);
			maintenanceSlots.add(maintenanceSlot);
		}
		
		return maintenanceSlots;
	}
}


