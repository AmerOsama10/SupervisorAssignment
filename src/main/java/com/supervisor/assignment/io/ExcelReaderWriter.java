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
			{"من", "From"},
			{"إلى", "الى", "To"}
		};
		int headerRow = findHeaderRowAny(sheet, groups);
		Map<String, Integer> idx = headerIndex(sheet.getRow(headerRow));
		for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
			Row row = sheet.getRow(r);
			if (row == null) continue;
			String subject = getFirstExisting(row, idx, "المادة", "Subject");
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
			s.setFrom(LocalTime.parse(normalizeTime(fromStr)));
			s.setTo(LocalTime.parse(normalizeTime(toStr)));
			if (!s.getTo().isAfter(s.getFrom())) {
				throw new IllegalArgumentException("Subjects row " + (r+1) + ": To must be > From");
			}
			Integer reqIdx = firstIndex(idx, "عدد_المشرفين", "Supervisors Required");
			int req = getOptionalInt(row, reqIdx, cfg.getDefaultSupervisorsPerSubject());
			s.setSupervisorsRequired(Math.max(1, req));
			s.setNotes(null);
			result.add(s);
		}
		if (result.isEmpty()) throw new IllegalArgumentException("No sessions found in Subjects");
		return result;
	}

	private List<Supervisor> readSupervisors(XSSFWorkbook wb) {
		XSSFSheet sheet = getSheetAny(wb, Arrays.asList("المشرفون", "Supervisors"));
		if (sheet == null) throw new IllegalArgumentException("Missing 'Supervisors/المشرفون' sheet");
		List<Supervisor> result = new ArrayList<>();
		String[][] groups = new String[][]{
			{"المشرف", "Supervisor"},
			{"الأيام المتاحة", "الايام المتاحة", "Available Days"}
		};
		int headerRow = findHeaderRowAny(sheet, groups);
		Map<String, Integer> idx = headerIndex(sheet.getRow(headerRow));
		Set<String> names = new HashSet<>();
		for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
			Row row = sheet.getRow(r);
			if (row == null) continue;
			String name = getFirstExisting(row, idx, "المشرف", "Supervisor");
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
			Integer maxIdx = firstIndex(idx, "الحد الأقصى للساعات", "Max Hours");
			s.setMaxHours(getOptionalDouble(row, maxIdx));
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
			try (OutputStream out = new FileOutputStream(outputFile)) {
				wb.write(out);
			}
		}
	}

	private void writeAssignmentsSheet(XSSFWorkbook wb, AssignmentResult result) {
		Sheet sheet = Optional.ofNullable(getSheetAny(wb, Arrays.asList("التعيينات", "Assignments"))).orElseGet(() -> wb.createSheet("التعيينات"));
		int r = 0;
		Row header = getOrCreateRow(sheet, r++);
		writeRow(header, "معرف_المادة", "المادة", "اليوم", "التاريخ", "من", "إلى", "المشرفون المعينون", "الحالة", "السبب");
		List<AssignmentResult.SessionAssignment> sorted = new ArrayList<>(result.getSessionAssignments());
		sorted.sort(
			Comparator
				.comparing((AssignmentResult.SessionAssignment sa) -> sa.getSession().getDate(), Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(sa -> sa.getSession().getFrom())
		);
		for (AssignmentResult.SessionAssignment sa : sorted) {
			Row row = getOrCreateRow(sheet, r++);
			SubjectSession s = sa.getSession();
			writeRow(row,
				s.getId(),
				s.getSubjectName(),
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
		Sheet sheet = Optional.ofNullable(getSheetAny(wb, Arrays.asList("إجمالي المشرفين", "Supervisor Totals"))).orElseGet(() -> wb.createSheet("إجمالي المشرفين"));
		int r = 0;
		Row header = getOrCreateRow(sheet, r++);
		List<String> dayCols = new ArrayList<>();
		for (DayOfWeekEnum d : DayOfWeekEnum.values()) dayCols.add("ساعات_" + arabicDay(d));
		List<String> cols = new ArrayList<>(Arrays.asList("المشرف", "إجمالي_الساعات", "عدد_الجلسات", "الانحراف_عن_الهدف", "الساعات_الفعلية"));
		cols.addAll(dayCols);
		cols.addAll(Arrays.asList("الحد_الأقصى_المُعد"));
		writeRow(header, cols.toArray(new String[0]));

		for (AssignmentResult.SupervisorTotals t : result.getSupervisorTotals()) {
			Row row = getOrCreateRow(sheet, r++);
			List<String> data = new ArrayList<>();
			data.add(t.getSupervisor());
			data.add(String.format(java.util.Locale.US, "%.2f", t.getTotalHours()));
			data.add(Integer.toString(t.getSessionsCount()));
			data.add(String.format(java.util.Locale.US, "%.2f", t.getDeviationFromTarget()));
			data.add(String.format(java.util.Locale.US, "%.2f", t.getTotalHours()));
			for (DayOfWeekEnum d : DayOfWeekEnum.values()) {
				double v = t.getPerDayHours().getOrDefault(d, 0.0);
				data.add(String.format(java.util.Locale.US, "%.2f", v));
			}
			data.add(t.getMaxHoursConfigured() == null ? "" : String.format(java.util.Locale.US, "%.2f", t.getMaxHoursConfigured()));
			writeRow(row, data.toArray(new String[0]));
		}

		SheetConditionalFormatting scf = sheet.getSheetConditionalFormatting();
		ConditionalFormattingRule rule = scf.createConditionalFormattingRule(ComparisonOperator.GT, "1");
		PatternFormatting fill = rule.createPatternFormatting();
		fill.setFillBackgroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
		CellRangeAddress[] regions = { new CellRangeAddress(1, r, 3, 3) }; // الانحراف_عن_الهدف column
		scf.addConditionalFormatting(regions, rule);
	}

	public void writeSupervisorSchedulesSheet(XSSFWorkbook wb, AssignmentResult result) {
		Sheet sheet = Optional.ofNullable(getSheetAny(wb, Arrays.asList("جداول المشرفين"))).orElseGet(() -> wb.createSheet("جداول المشرفين"));
		int r = 0;
		for (AssignmentResult.SupervisorTotals t : result.getSupervisorTotals()) {
			Row title = getOrCreateRow(sheet, r++);
			writeRow(title, "المشرف:", t.getSupervisor());
			Row header = getOrCreateRow(sheet, r++);
			writeRow(header, "التاريخ", "اليوم", "من", "إلى", "المادة");
			List<AssignmentResult.SessionAssignment> list = new ArrayList<>();
			for (AssignmentResult.SessionAssignment sa : result.getSessionAssignments()) {
				if (sa.getAssignedSupervisors().contains(t.getSupervisor())) list.add(sa);
			}
			list.sort(
				Comparator
					.comparing((AssignmentResult.SessionAssignment sa) -> sa.getSession().getDate(), Comparator.nullsLast(Comparator.naturalOrder()))
					.thenComparing(sa -> sa.getSession().getFrom())
			);
			for (AssignmentResult.SessionAssignment sa : list) {
				SubjectSession s = sa.getSession();
				Row row = getOrCreateRow(sheet, r++);
				writeRow(row,
					s.getDate() == null ? "" : s.getDate().toString(),
					arabicDay(s.getDay()),
					s.getFrom().toString(),
					s.getTo().toString(),
					s.getSubjectName()
				);
			}
			r += 2;
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


