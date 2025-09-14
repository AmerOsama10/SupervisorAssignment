package com.supervisor.assignment.io;

import com.supervisor.assignment.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ExcelReaderWriter {

	public static class ParsedInput {
		public List<SubjectSession> sessions;
		public List<Supervisor> supervisors;
		public Config config;
	}

	public ParsedInput readWorkbook(File file) throws IOException {
		try (InputStream in = new FileInputStream(file); XSSFWorkbook wb = new XSSFWorkbook(in)) {
			ParsedInput pi = new ParsedInput();
			pi.config = new Config();
			pi.sessions = readSubjects(wb, pi.config);
			pi.supervisors = readSupervisors(wb);
			return pi;
		}
	}

	private List<SubjectSession> readSubjects(XSSFWorkbook wb, Config cfg) {
		XSSFSheet sheet = getSheetAny(wb, Arrays.asList("المواد", "Subjects"));
		if (sheet == null) throw new IllegalArgumentException("Missing 'Subjects/المواد' sheet");
		List<SubjectSession> result = new ArrayList<>();
		// Tolerant header detection: accept Arabic/English variants and minor spelling differences
		String[][] groups = new String[][]{
			{"المادة", "Subject"},
			{"اليوم", "Day"},
			{"التاريخ", "Date"},
			{"الفترة", "Period"},
			{"من", "From"},
			{"إلى", "الى", "To"}
		};
		int headerRow = findHeaderRowAny(sheet, groups);
		Map<String, Integer> idx = headerIndex(sheet.getRow(headerRow));
		for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
			Row row = sheet.getRow(r);
			if (row == null) continue;
			String subject = getFirstExisting(row, idx, "المادة", "Subject");
			String building = getFirstExisting(row, idx, "المبنى", "المبني", "Building");
			String periodStr = getFirstExisting(row, idx, "الفترة", "Period");
			String dayStr = getFirstExisting(row, idx, "اليوم", "Day");
			String dateStr = getFirstExisting(row, idx, "التاريخ", "Date");
			String fromStr = getFirstExisting(row, idx, "من", "From");
			String toStr = getFirstExisting(row, idx, "إلى", "الى", "To");
			if (isBlank(subject) && isBlank(dayStr) && isBlank(fromStr) && isBlank(toStr) && isBlank(dateStr)) continue;
			if (isBlank(subject) || isBlank(dayStr) || isBlank(fromStr) || isBlank(toStr) || isBlank(dateStr)) {
				throw new IllegalArgumentException("Subjects row " + (r+1) + ": required fields missing");
			}
			SubjectSession s = new SubjectSession();
			s.setId("S-" + r);
			s.setSubjectName(subject);
			DayOfWeekEnum parsedDay = null;
			try { parsedDay = DayOfWeekEnum.fromString(dayStr); } catch (Exception ignored) {}
			s.setDate(parseDateCell(dateStr));
			DayOfWeekEnum fromDate = mapJavaDayToEnum(s.getDate().getDayOfWeek());
			// Prefer date-derived day, ignore mismatches for robustness
			s.setDay(fromDate != null ? fromDate : (parsedDay != null ? parsedDay : DayOfWeekEnum.SATURDAY));
			PeriodOfDay period = parsePeriod(periodStr);
			LocalTime fromTime = parseTime12h(fromStr, period);
			LocalTime toTime = parseTime12h(toStr, period);
			// Heuristic: if times are not increasing, try opposite period interpretation
			if (!toTime.isAfter(fromTime)) {
				PeriodOfDay alt = (period == PeriodOfDay.MORNING ? PeriodOfDay.EVENING : PeriodOfDay.MORNING);
				LocalTime altFrom = parseTime12h(fromStr, alt);
				LocalTime altTo = parseTime12h(toStr, alt);
				if (altTo.isAfter(altFrom)) {
					period = alt;
					fromTime = altFrom;
					toTime = altTo;
				}
			}
			// Fallback: interpret as 24-hour if still invalid
			if (!toTime.isAfter(fromTime)) {
				try {
					LocalTime f24 = LocalTime.parse(normalizeTime(fromStr));
					LocalTime t24 = LocalTime.parse(normalizeTime(toStr));
					if (t24.isAfter(f24)) {
						fromTime = f24;
						toTime = t24;
						period = f24.getHour() < 12 ? PeriodOfDay.MORNING : PeriodOfDay.EVENING;
					}
				} catch (Exception ignore) {}
			}
			// Last resort: adjust toTime to be at least +1 hour if equal or before
			if (!toTime.isAfter(fromTime)) {
				LocalTime candidate = fromTime.plusHours(1);
				if (candidate.getHour() == 0 && fromTime.getHour() == 23) {
					candidate = fromTime.plusMinutes(30);
				}
				toTime = candidate;
			}
			s.setPeriod(period);
			s.setFrom(fromTime);
			s.setTo(toTime);
			if (!s.getTo().isAfter(s.getFrom())) {
				throw new IllegalArgumentException("Subjects row " + (r+1) + ": To must be > From");
			}
			Integer reqIdx = firstIndex(idx, "عدد_الملاحظين", "عدد_المشرفين", "Supervisors Required");
			int req = getOptionalInt(row, reqIdx, cfg.getDefaultSupervisorsPerSubject());
			s.setSupervisorsRequired(Math.max(1, req));
			s.setNotes(null);
			s.setBuilding(isBlank(building) ? null : building.trim());
			s.setRequiredRole(RoleType.INVIGILATOR);
			
			// Read role type column
			String roleTypeStr = getFirstExisting(row, idx, "نوع", "Type", "Role Type");
			if (!isBlank(roleTypeStr)) {
				s.setRoleType(RoleType.fromString(roleTypeStr));
			}
			
			result.add(s);
		}
		if (result.isEmpty()) throw new IllegalArgumentException("No sessions found in Subjects");
		return result;
	}

	private List<Supervisor> readSupervisors(XSSFWorkbook wb) {
		XSSFSheet sheet = getSheetAny(wb, Arrays.asList("المراقبون", "المشرفون", "Supervisors"));
		if (sheet == null) throw new IllegalArgumentException("Missing 'Supervisors/المشرفون' sheet");
		List<Supervisor> result = new ArrayList<>();
		String[][] groups = new String[][]{
			{"الاسم", "المشرف", "Supervisor"},
			{"الأيام المتاحة", "الايام المتاحة", "Available Days"}
		};
		int headerRow = findHeaderRowAny(sheet, groups);
		Map<String, Integer> idx = headerIndex(sheet.getRow(headerRow));
		Set<String> names = new HashSet<>();
		for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
			Row row = sheet.getRow(r);
			if (row == null) continue;
			String name = getFirstExisting(row, idx, "الاسم", "المشرف", "Supervisor");
			String days = getFirstExisting(row, idx, "الأيام المتاحة", "الايام المتاحة", "Available Days");
			if (isBlank(name) && isBlank(days)) continue;
			if (isBlank(name) || isBlank(days)) {
				throw new IllegalArgumentException("Supervisors row " + (r+1) + ": required fields missing");
			}
			if (!names.add(name)) {
				throw new IllegalArgumentException("Duplicate supervisor name: " + name);
			}
			Supervisor s = new Supervisor();
			s.setName(name);
			Set<DayOfWeekEnum> set = new HashSet<>();
			for (String d : days.split(",")) {
				if (!isBlank(d)) set.add(DayOfWeekEnum.fromString(d.trim()));
			}
			s.setAvailableDays(set);
			Integer pctIdx = firstIndex(idx, "النسبة٪", "النسبة%", "Load%", "LoadPct");
			Double pct = getOptionalDouble(row, pctIdx);
			s.setLoadPercentage(pct == null ? 100.0 : pct);
			String roleStr = getFirstExisting(row, idx, "الوظيفة", "النوع", "Role");
			if (!isBlank(roleStr)) {
				s.setRole(RoleType.fromString(roleStr));
			}
			
			// Read excluded subjects column
			String excludedSubjectsStr = getFirstExisting(row, idx, "المواد المستبعدة", "Excluded Subjects");
			if (!isBlank(excludedSubjectsStr)) {
				Set<String> excludedSubjects = new HashSet<>();
				for (String subject : excludedSubjectsStr.split(",")) {
					if (!isBlank(subject)) {
						excludedSubjects.add(subject.trim());
					}
				}
				s.setExcludedSubjects(excludedSubjects);
			}
			
			result.add(s);
		}
		if (result.isEmpty()) throw new IllegalArgumentException("No supervisors found in Supervisors");
		return result;
	}

	public void writeOutput(File inputFile, File outputFile, AssignmentResult result) throws IOException {
		try (InputStream in = new FileInputStream(inputFile); XSSFWorkbook wb = new XSSFWorkbook(in)) {
			writeAssignmentsSheet(wb, result);
			writeSupervisorTotalsSheet(wb, result);
			writeSupervisorSchedulesSheet(wb, result);
			writeLogsSheet(wb, result);
			writeBackupsSheet(wb, result);
			try (OutputStream out = new FileOutputStream(outputFile)) {
				wb.write(out);
			}
		}
	}

	private void writeAssignmentsSheet(XSSFWorkbook wb, AssignmentResult result) {
		Sheet sheet = Optional.ofNullable(getSheetAny(wb, Arrays.asList("التعيينات", "Assignments"))).orElseGet(() -> wb.createSheet("التعيينات"));
		if (sheet instanceof XSSFSheet) ((XSSFSheet) sheet).setRightToLeft(true);
		int r = 0;
		Row header = getOrCreateRow(sheet, r++);
		writeRow(header, "معرف_المادة", "المادة", "المبنى", "الفترة", "اليوم", "التاريخ", "من", "إلى", "المعينون", "الحالة", "السبب");
		List<AssignmentResult.SessionAssignment> sorted = new ArrayList<>(result.getSessionAssignments());
		sorted.sort((a, b) -> {
			SubjectSession sa = a.getSession();
			SubjectSession sb = b.getSession();
			// 1) date
			int cmp = Comparator.nullsLast(Comparator.<java.time.LocalDate>naturalOrder()).compare(sa.getDate(), sb.getDate());
			if (cmp != 0) return cmp;
			// 2) period: MORNING first
			int pa = sa.getPeriod() == null ? 0 : (sa.getPeriod() == PeriodOfDay.MORNING ? 0 : 1);
			int pb = sb.getPeriod() == null ? 0 : (sb.getPeriod() == PeriodOfDay.MORNING ? 0 : 1);
			cmp = Integer.compare(pa, pb);
			if (cmp != 0) return cmp;
			// 3) building
			String baStr = sa.getBuilding() == null ? "" : sa.getBuilding();
			String bbStr = sb.getBuilding() == null ? "" : sb.getBuilding();
			cmp = baStr.compareTo(bbStr);
			if (cmp != 0) return cmp;
			// 4) role: floor supervisor first
			int ra = sa.getRequiredRole() == RoleType.FLOOR_SUPERVISOR ? 0 : 1;
			int rb = sb.getRequiredRole() == RoleType.FLOOR_SUPERVISOR ? 0 : 1;
			cmp = Integer.compare(ra, rb);
			if (cmp != 0) return cmp;
			// 5) time from
			return sa.getFrom().compareTo(sb.getFrom());
		});
		for (AssignmentResult.SessionAssignment sa : sorted) {
			Row row = getOrCreateRow(sheet, r++);
			SubjectSession s = sa.getSession();
			writeRow(row,
				s.getId(),
				s.getSubjectName(),
				s.getBuilding() == null ? "" : s.getBuilding(),
				s.getPeriod() == null ? "" : (s.getPeriod() == PeriodOfDay.MORNING ? "صباحي" : "مسائي"),
				arabicDay(s.getDay()),
				s.getDate() == null ? "" : s.getDate().toString(),
				s.getFrom().toString(),
				s.getTo().toString(),
				String.join(", ", sa.getAssignedSupervisors()),
				arabicStatus(sa.getStatus()),
				sa.getReason() == null ? "" : sa.getReason()
			);
		}
	}

	private void writeSupervisorTotalsSheet(XSSFWorkbook wb, AssignmentResult result) {
		Sheet sheet = Optional.ofNullable(getSheetAny(wb, Arrays.asList("إجمالي المراقبين", "Supervisor Totals"))).orElseGet(() -> wb.createSheet("إجمالي المراقبين"));
		if (sheet instanceof XSSFSheet) ((XSSFSheet) sheet).setRightToLeft(true);
		int r = 0;
		Row header = getOrCreateRow(sheet, r++);
		List<String> cols = new ArrayList<>(Arrays.asList("الاسم", "الوظيفة", "ساعات_أساسي", "ساعات_احتياطي", "الساعات_الهدف", "الساعات_الفعلية", "نسبة_التحميل%"));
		writeRow(header, cols.toArray(new String[0]));

		List<AssignmentResult.SupervisorTotals> totalsList = new ArrayList<>(result.getSupervisorTotals());
		totalsList.sort(Comparator
			.comparing((AssignmentResult.SupervisorTotals t) -> t.getRole() == RoleType.FLOOR_SUPERVISOR ? 0 : 1)
			.thenComparing(AssignmentResult.SupervisorTotals::getSupervisor)
		);
		for (AssignmentResult.SupervisorTotals t : totalsList) {
			Row row = getOrCreateRow(sheet, r++);
			List<String> data = new ArrayList<>();
			data.add(t.getSupervisor());
			data.add(t.getRole() == RoleType.FLOOR_SUPERVISOR ? "مشرف دور" : "ملاحظ");
			data.add(String.format(java.util.Locale.US, "%.2f", t.getPrimaryHours()));
			data.add(String.format(java.util.Locale.US, "%.2f", t.getBackupHours()));
			data.add(String.format(java.util.Locale.US, "%.2f", t.getExpectedHours()));
			data.add(String.format(java.util.Locale.US, "%.2f", t.getTotalHours()));
			double pct = t.getExpectedHours() <= 0.0 ? 0.0 : (t.getTotalHours() / t.getExpectedHours()) * 100.0;
			if (pct > 100.0) pct = 100.0;
			data.add(String.format(java.util.Locale.US, "%.0f%%", pct));
			writeRow(row, data.toArray(new String[0]));
		}

	}

	public void writeSupervisorSchedulesSheet(XSSFWorkbook wb, AssignmentResult result) {
		Sheet sheet = Optional.ofNullable(getSheetAny(wb, Arrays.asList("جداول المراقبين"))).orElseGet(() -> wb.createSheet("جداول المراقبين"));
		if (sheet instanceof XSSFSheet) ((XSSFSheet) sheet).setRightToLeft(true);
		int r = 0;
		// Build a map from sessionId to session for fast lookup (covers both subjects and floor slots)
		Map<String, SubjectSession> sessionById = new HashMap<>();
		for (AssignmentResult.SessionAssignment sa : result.getSessionAssignments()) {
			SubjectSession s = sa.getSession();
			if (s != null && s.getId() != null) sessionById.put(s.getId(), s);
		}
		for (AssignmentResult.SupervisorTotals t : result.getSupervisorTotals()) {
			Row title = getOrCreateRow(sheet, r);
			String roleLabel = t.getRole() == RoleType.FLOOR_SUPERVISOR ? "(مشرف دور)" : "(ملاحظ)";
			String loadLabel = String.format(java.util.Locale.US, "%.0f%%", t.getLoadPercentage() == null ? 100.0 : t.getLoadPercentage());
			writeRow(title, t.getSupervisor() + " " + roleLabel + " - تحميل: " + loadLabel);
			if (sheet instanceof XSSFSheet) {
				((XSSFSheet) sheet).addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(r, r, 0, 5));
			}
			r++;
			Row header = getOrCreateRow(sheet, r++);
			writeRow(header, "المادة", "المبنى", "التاريخ", "اليوم", "الفترة", "من", "إلى", "نوع");

			// Collect combined events (primary + backups) for this supervisor
			class Event { String subject; String building; java.time.LocalDate date; DayOfWeekEnum day; PeriodOfDay period; java.time.LocalTime from; java.time.LocalTime to; String kind; }
			List<Event> events = new ArrayList<>();
			for (AssignmentResult.SessionAssignment sa : result.getSessionAssignments()) {
				if (!sa.getAssignedSupervisors().contains(t.getSupervisor())) continue;
				SubjectSession s = sa.getSession();
				Event ev = new Event();
				ev.subject = s.getSubjectName();
				ev.building = s.getBuilding() == null ? "" : s.getBuilding();
				ev.date = s.getDate();
				ev.day = s.getDay();
				ev.period = s.getPeriod();
				ev.from = s.getFrom();
				ev.to = s.getTo();
				ev.kind = "أساسي";
				events.add(ev);
			}
			for (AssignmentResult.BackupAssignment ba : result.getBackupAssignments()) {
				if (!t.getSupervisor().equals(ba.getSupervisor())) continue;
				Event ev = new Event();
				SubjectSession s = ba.getSessionId() != null ? sessionById.get(ba.getSessionId()) : null;
				ev.subject = ba.getSubject() == null && s != null ? s.getSubjectName() : (ba.getSubject() == null ? "" : ba.getSubject());
				ev.building = ba.getBuilding() == null ? (s != null && s.getBuilding() != null ? s.getBuilding() : "") : ba.getBuilding();
				ev.date = ba.getDate();
				ev.day = (ba.getDate() == null ? null : mapJavaDayToEnum(ba.getDate().getDayOfWeek()));
				ev.period = ba.getPeriod();
				ev.from = (s != null ? s.getFrom() : null);
				ev.to = (s != null ? s.getTo() : null);
				ev.kind = "احتياطي";
				events.add(ev);
			}
			// Sort by date then from time
			events.sort((e1, e2) -> {
				int cmp = Comparator.nullsLast(Comparator.<java.time.LocalDate>naturalOrder()).compare(e1.date, e2.date);
				if (cmp != 0) return cmp;
				return Comparator.nullsLast(Comparator.<java.time.LocalTime>naturalOrder()).compare(e1.from, e2.from);
			});

			// Write rows
			for (Event ev : events) {
				Row row = getOrCreateRow(sheet, r++);
				writeRow(row,
					ev.subject == null ? "" : ev.subject,
					ev.building == null ? "" : ev.building,
					ev.date == null ? "" : ev.date.toString(),
					ev.day == null ? "" : arabicDay(ev.day),
					ev.period == null ? "" : (ev.period == PeriodOfDay.MORNING ? "صباحي" : "مسائي"),
					ev.from == null ? "" : ev.from.toString(),
					ev.to == null ? "" : ev.to.toString(),
					ev.kind
				);
			}

			Row spacer = getOrCreateRow(sheet, r++);
			writeRow(spacer, "");
			r += 1;
		}
	}

	private void writeLogsSheet(XSSFWorkbook wb, AssignmentResult result) {
		Sheet sheet = Optional.ofNullable(getSheetAny(wb, Arrays.asList("Logs"))).orElseGet(() -> wb.createSheet("Logs"));
		int r = sheet.getLastRowNum() + 1;
		if (r == 0) {
			Row h = sheet.createRow(r++);
			writeRow(h, "Timestamp", "TotalSessions", "Assigned", "Partial", "Unassigned", "TargetHours", "TotalHoursNeeded");
		} else {
			r++;
		}
		long assigned = result.getSessionAssignments().stream().filter(a -> "Assigned".equals(a.getStatus())).count();
		long partial = result.getSessionAssignments().stream().filter(a -> "PartiallyAssigned".equals(a.getStatus())).count();
		long unassigned = result.getSessionAssignments().stream().filter(a -> "Unassigned".equals(a.getStatus())).count();
		Row row = sheet.createRow(r);
		writeRow(row,
			java.time.ZonedDateTime.now().toString(),
			Integer.toString(result.getSessionAssignments().size()),
			Long.toString(assigned),
			Long.toString(partial),
			Long.toString(unassigned),
			String.format(java.util.Locale.US, "%.2f", result.getTargetHoursPerSupervisor()),
			String.format(java.util.Locale.US, "%.2f", result.getTotalHoursNeeded())
		);
	}

	private void writeBackupsSheet(XSSFWorkbook wb, AssignmentResult result) {
		Sheet sheet = Optional.ofNullable(getSheetAny(wb, Arrays.asList("الاحتياط"))).orElseGet(() -> wb.createSheet("الاحتياط"));
		if (sheet instanceof XSSFSheet) ((XSSFSheet) sheet).setRightToLeft(true);
		int r = 0;
		Row header = getOrCreateRow(sheet, r++);
		writeRow(header, "التاريخ", "اليوم", "الفترة", "النوع", "الاسم", "المادة", "المبنى");
		List<AssignmentResult.BackupAssignment> list = new ArrayList<>(result.getBackupAssignments());
		list.sort(Comparator
			.comparing(AssignmentResult.BackupAssignment::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(ba -> ba.getPeriod() == null ? 0 : (ba.getPeriod() == PeriodOfDay.MORNING ? 0 : 1))
			.thenComparing(AssignmentResult.BackupAssignment::getSupervisor)
		);
		for (AssignmentResult.BackupAssignment ba : list) {
			Row row = getOrCreateRow(sheet, r++);
			writeRow(row,
				ba.getDate() == null ? "" : ba.getDate().toString(),
				ba.getDate() == null ? "" : arabicDay(mapJavaDayToEnum(ba.getDate().getDayOfWeek())),
				ba.getPeriod() == null ? "" : (ba.getPeriod() == PeriodOfDay.MORNING ? "صباحي" : "مسائي"),
				ba.getRole() == RoleType.FLOOR_SUPERVISOR ? "مشرف دور" : "ملاحظ",
				ba.getSupervisor(),
				ba.getSubject() == null ? "" : ba.getSubject(),
				ba.getBuilding() == null ? "" : ba.getBuilding()
			);
		}
	}

	// Helpers
	private Map<String, Integer> headerIndex(Row row) {
		if (row == null) throw new IllegalArgumentException("Sheet is missing header row");
		Map<String, Integer> map = new HashMap<>();
		for (int c = 0; c < row.getLastCellNum(); c++) {
			Cell cell = row.getCell(c);
			if (cell == null) continue;
			String name = getCellString(cell);
			if (name != null && !name.trim().isEmpty()) map.put(name.trim(), c);
		}
		return map;
	}

	private int findHeaderRow(Sheet sheet, String[] mustHave) {
		int max = Math.min(sheet.getLastRowNum(), 30);
		for (int r = 0; r <= max; r++) {
			Row row = sheet.getRow(r);
			if (row == null) continue;
			Map<String, Integer> idx = headerIndex(row);
			boolean ok = true;
			for (String col : mustHave) if (!idx.containsKey(col)) { ok = false; break; }
			if (ok) return r;
		}
		throw new IllegalArgumentException("Missing required columns: " + String.join(", ", mustHave));
	}

	private int findHeaderRowAny(Sheet sheet, String[][] groups) {
		int max = Math.min(sheet.getLastRowNum(), 50);
		for (int r = 0; r <= max; r++) {
			Row row = sheet.getRow(r);
			if (row == null) continue;
			Map<String, Integer> idx = headerIndex(row);
			boolean allGroupsPresent = true;
			for (String[] group : groups) {
				boolean groupPresent = false;
				for (String name : group) {
					if (idx.containsKey(name)) { groupPresent = true; break; }
				}
				if (!groupPresent) { allGroupsPresent = false; break; }
			}
			if (allGroupsPresent) return r;
		}
		// Build friendly message listing any one from each group
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < groups.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(String.join("/", groups[i]));
		}
		throw new IllegalArgumentException("Missing required columns: " + sb.toString());
	}

	private void required(Map<String, Integer> idx, String... cols) {
		for (String col : cols) if (!idx.containsKey(col)) throw new IllegalArgumentException("Missing required column '" + col + "'");
	}

	private String normalizeTime(String s) {
		String t = s.trim();
		if (t.length() == 4) t = "0" + t;
		return t;
	}

	private PeriodOfDay parsePeriod(String s) {
		if (s == null) return PeriodOfDay.MORNING; // default
		String v = s.trim();
		if (v.equals("صباحي") || v.equalsIgnoreCase("MORNING")) return PeriodOfDay.MORNING;
		if (v.equals("مسائي") || v.equalsIgnoreCase("EVENING")) return PeriodOfDay.EVENING;
		return PeriodOfDay.MORNING;
	}

	private LocalTime parseTime12h(String s, PeriodOfDay period) {
		if (isBlank(s)) return LocalTime.MIDNIGHT;
		String t = s.trim();
		if (t.contains("AM") || t.contains("PM") || t.contains("am") || t.contains("pm")) {
			// If explicitly marked, use 12-hour format with AM/PM
			DateTimeFormatter fmt = DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH);
			return LocalTime.parse(t.toUpperCase(java.util.Locale.ENGLISH), fmt);
		}
		// Otherwise, interpret based on period
		String[] parts = t.split(":");
		int hour = Integer.parseInt(parts[0].trim());
		int minute = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
		// Fallback: accept 24-hour input (13..23)
		if (hour >= 13 && hour <= 23) {
			return LocalTime.of(hour, minute);
		}
		if (period == PeriodOfDay.MORNING) {
			// Treat 12 as 12:00 noon for morning shifts
			if (hour < 1 || hour > 12) throw new IllegalArgumentException("Invalid 12h hour: " + hour);
			// keep 12 as 12, 1..11 as-is
		} else {
			// Evening: 1..11 -> 13..23; keep 12 as 12:00 (noon) or allow 12 as 12:00
			if (hour < 1 || hour > 12) throw new IllegalArgumentException("Invalid 12h hour: " + hour);
			if (hour != 12) hour += 12;
		}
		return LocalTime.of(hour, minute);
	}

	private String getCellString(Cell cell) {
		if (cell == null) return "";
		if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
		if (cell.getCellType() == CellType.NUMERIC) {
			if (DateUtil.isCellDateFormatted(cell)) {
				java.util.Date d = cell.getDateCellValue();
				return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
			} else {
				double v = cell.getNumericCellValue();
				if (Math.floor(v) == v) return Long.toString((long) v);
				return Double.toString(v);
			}
		}
		if (cell.getCellType() == CellType.BOOLEAN) return Boolean.toString(cell.getBooleanCellValue());
		return "";
	}

	private String getFirstExisting(Row row, Map<String, Integer> idx, String... keys) {
		Integer i = firstIndex(idx, keys);
		return i == null ? "" : getCellString(row.getCell(i));
	}

	private Integer firstIndex(Map<String, Integer> idx, String... keys) {
		for (String k : keys) {
			Integer i = idx.get(k);
			if (i != null) return i;
		}
		return null;
	}

	private int getOptionalInt(Row row, Integer idx, int defaultVal) {
		if (idx == null) return defaultVal;
		String s = getCellString(row.getCell(idx));
		if (isBlank(s)) return defaultVal;
		return Integer.parseInt(s.trim());
	}

	private Double getOptionalDouble(Row row, Integer idx) {
		if (idx == null) return null;
		String s = getCellString(row.getCell(idx));
		if (isBlank(s)) return null;
		return Double.parseDouble(s.trim());
	}

	private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

	private Row getOrCreateRow(Sheet sheet, int r) {
		Row row = sheet.getRow(r);
		if (row == null) row = sheet.createRow(r);
		return row;
	}

	private void writeRow(Row row, String... values) {
		for (int i = 0; i < values.length; i++) {
			Cell cell = row.getCell(i);
			if (cell == null) cell = row.createCell(i);
			cell.setCellValue(values[i]);
		}
	}

	private String capitalize(String s) {
		if (s == null || s.isEmpty()) return s;
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	private XSSFSheet getSheetAny(XSSFWorkbook wb, List<String> names) {
		for (String n : names) {
			XSSFSheet s = wb.getSheet(n);
			if (s != null) return s;
		}
		return null;
	}

	private LocalDate parseDateCell(String s) {
		if (isBlank(s)) return null;
		try { return LocalDate.parse(s.trim()); } catch (Exception ignored) {}
		try { return LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern("d-M-uuuu")); } catch (Exception ignored) {}
		try { return LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern("d/M/uuuu")); } catch (Exception ignored) {}
		throw new IllegalArgumentException("Invalid date: " + s);
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

	private String arabicDay(DayOfWeekEnum d) {
		switch (d) {
			case SATURDAY: return "السبت";
			case SUNDAY: return "الأحد";
			case MONDAY: return "الاثنين";
			case TUESDAY: return "الثلاثاء";
			case WEDNESDAY: return "الأربعاء";
			case THURSDAY: return "الخميس";
			case FRIDAY: return "الجمعة";
			default: return "";
		}
	}

	private String arabicStatus(String status) {
		if (status == null) return "";
		switch (status) {
			case "Assigned": return "مُعين";
			case "PartiallyAssigned": return "معين جزئياً";
			case "Unassigned": return "غير معين";
			default: return status;
		}
	}
}


