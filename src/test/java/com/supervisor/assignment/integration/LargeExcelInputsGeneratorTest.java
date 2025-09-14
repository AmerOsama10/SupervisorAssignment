package com.supervisor.assignment.integration;

import com.supervisor.assignment.model.Config;
import com.supervisor.assignment.template.TemplateGenerator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LargeExcelInputsGeneratorTest {

	@Test
	public void generateTwoLargeInputs() throws Exception {
		File outDir = new File("target/test-excels");
		if (!outDir.exists()) outDir.mkdirs();

		// Input A: exactly 300 sessions, ~80 supervisors
		File inputA = new File(outDir, "input_large_A.xlsx");
		generateLargeInputFixed(inputA, /*numSessions*/ 300, /*numSupervisors*/ 80, Arrays.asList(
			"المبنى الرابع - الدور الثاني",
			"المبنى الخامس - الدور الثالث",
			"المبنى الجديد - الدور الأول"
		));
		assertTrue(inputA.exists());

		// Input B: ~500 sessions, ~150 supervisors (unchanged generator)
		File inputB = new File(outDir, "input_large_B.xlsx");
		generateLargeInput(inputB, /*numSessions*/ 500, /*numSupervisors*/ 150, Arrays.asList(
			"المبنى الرابع - الدور الثاني",
			"المبنى الخامس - الدور الثالث",
			"المبنى الجديد - الدور الأول",
			"كلية الهندسة - مبنى أ",
			"كلية العلوم - مبنى ب"
		));
		assertTrue(inputB.exists());
	}

	private void generateLargeInputFixed(File file, int numSessions, int numSupervisors, List<String> buildings) throws Exception {
		new TemplateGenerator().generateTemplate(file, new Config());

		List<String> subjects = Arrays.asList(
			"رياضيات عامة 101", "كيمياء عامة 101", "فيزياء 1", "لغة عربية 101", "لغة إنجليزية 101",
			"أحياء عامة", "إحصاء واحتمالات", "حاسب آلي", "برمجة Java", "تحليل عددي",
			"معادلات تفاضلية", "هندسة برمجيات", "قواعد بيانات", "شبكات الحاسوب", "ذكاء اصطناعي",
			"هندسة كهربائية", "ميكانيكا عامة", "محاسبة مالية", "إدارة أعمال", "قانون تجاري",
			"تحليل نظم", "عمارة حاسوب", "نظم تشغيل", "تراكيب بيانات", "هندسة مدنية"
		);
		List<String> arabicNames = Arrays.asList(
			"أحمد علي", "محمد حسن", "خالد يوسف", "سلمان العتيبي", "عبدالله القحطاني",
			"فهد المطيري", "سعيد الغامدي", "حسين السلمي", "عبدالرحمن الشهراني", "ماجد الزهراني",
			"تركي الدوسري", "سعود الشمري", "عبدالعزيز التركي", "يوسف الحربي", "فيصل الجهني",
			"إبراهيم الرشيدي", "ناصر الشهري", "طارق المطيري", "عبدالمجيد الحارثي", "بندر السبيعي",
			"عمر العبدالله", "راشد القحطاني", "خالد الدوسري", "حسن البقمي", "وليد الحربي",
			"مساعد المطيري", "هيثم الزهراني", "زياد الشمراني", "سامي القاسم", "حمد الدوسري",
			"مشعل الحربي", "رمزي القرني", "باسل العتيبي", "قصي السويلم", "ثامر الشهراني",
			"ليث الحارثي", "أمين الزايدي", "فهد الزايدي", "خليل الحربي", "محمد العوفي",
			"عبدالله السالم", "تركي العبدالكريم", "بسام الحربي", "نواف العتيبي", "يزيد المقبل",
			"نايف الروقي", "عبدالإله الشمري", "حمد السبيعي", "ريان الحربي", "سلطان الدوسري",
			"يزن الغامدي", "طلال الحارثي", "رامي المطيري", "بدر الزهراني", "حسام الشهري",
			"وليد القحطاني", "مهند الحربي", "عبدالملك التركي", "منصور العتيبي", "قصي الشمري",
			"جمال الحربي", "اياد السبيعي", "محسن الشهراني", "طارق العمري", "حسن الشريف",
			"عبدالفتاح الغامدي", "هيثم السواط", "ماهر العوفي", "نادر الحربي", "فراس الزبن",
			"ضياء المطيري", "حاتم الزهراني", "سامي البلوي", "مؤيد الثقفي", "أصيل الحربي",
			"عبدالرحيم القرني", "محمد القاضي", "مروان النفيسي", "ياسر العبدلي", "أنس باوزير",
			"راشد العبدالقادر", "زيد الخالدي", "رائد السلمي", "معاذ الشهري", "خليل القحطاني",
			"عمرو العتيبي", "عبدالهادي الحربي", "مهدي الزهراني", "عبدالمحسن القاسم", "حمزة الفيفي"
		);

		try (FileInputStream fis = new FileInputStream(file); XSSFWorkbook wb = new XSSFWorkbook(fis)) {
			Sheet subjectsSheet = wb.getSheet("المواد");
			Sheet supervisorsSheet = wb.getSheet("المراقبون");

			// Two weeks starting upcoming Saturday
			LocalDate start = next(DayOfWeek.SATURDAY);
			LocalDate end = start.plusDays(13);
			int r = 2;
			int count = 0;
			int subjIdx = 0;
			int perBuildingPerPeriod = 5; // up to 5 sessions per building per period
			for (LocalDate d = start; d.isBefore(end) && count < numSessions; d = d.plusDays(1)) {
				if (d.getDayOfWeek() == DayOfWeek.FRIDAY) continue; // Friday off
				String arabicDay = arabicDay(d.getDayOfWeek());
				// Morning period 10:00-12:00 (12h)
				for (int rep = 0; rep < perBuildingPerPeriod && count < numSessions; rep++) {
					for (String building : buildings) {
						if (count >= numSessions) break;
						String id = "S" + (count + 1);
						String name = subjects.get(subjIdx % subjects.size()); subjIdx++;
						addSubjectRow(subjectsSheet, r++, id, name, building, "صباحي", arabicDay, d.toString(), "10:00", "12:00", 2);
						count++;
					}
				}
				// Evening period 01:00-03:00 (12h)
				for (int rep = 0; rep < perBuildingPerPeriod && count < numSessions; rep++) {
					for (String building : buildings) {
						if (count >= numSessions) break;
						String id = "S" + (count + 1);
						String name = subjects.get(subjIdx % subjects.size()); subjIdx++;
						addSubjectRow(subjectsSheet, r++, id, name, building, "مسائي", arabicDay, d.toString(), "01:00", "03:00", 2);
						count++;
					}
				}
			}

			// Supervisors: 85 names => first 10 floors (100%), 5 maintenance (100%), remaining 70 invigilators (100%)
			int s = 2;
			String availableDays = String.join(", ", Arrays.asList("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس"));
			for (int i = 0; i < 85; i++) {
				String base = arabicNames.get(i % arabicNames.size());
				String name = base + (i >= arabicNames.size() ? (" " + (i / arabicNames.size() + 1)) : "");
				String role;
				String excludedSubjects = "";
				if (i < 10) {
					role = "مشرف دور";
				} else if (i < 15) {
					role = "عامل";
				} else {
					role = "ملاحظ";
					// Some invigilators have excluded subjects
					if (i % 10 == 0) {
						excludedSubjects = subjects.get(i % subjects.size());
					}
				}
				int pct = 100;
				addSupervisorRow(supervisorsSheet, s++, name, availableDays, pct, role, excludedSubjects);
			}

			try (FileOutputStream fos = new FileOutputStream(file)) { wb.write(fos); }
		}
	}

	private void generateLargeInput(File file, int numSessions, int numSupervisors, List<String> buildings) throws Exception {
		new TemplateGenerator().generateTemplate(file, new Config());

		// Realistic subjects and Arabic names
		List<String> subjects = Arrays.asList(
			"رياضيات عامة 101", "كيمياء عامة 101", "فيزياء 1", "لغة عربية 101", "لغة إنجليزية 101",
			"أحياء عامة", "إحصاء واحتمالات", "حاسب آلي", "برمجة Java", "تحليل عددي",
			"معادلات تفاضلية", "هندسة برمجيات", "قواعد بيانات", "شبكات الحاسوب", "ذكاء اصطناعي",
			"هندسة كهربائية", "ميكانيكا عامة", "محاسبة مالية", "إدارة أعمال", "قانون تجاري",
			"تحليل نظم", "عمارة حاسوب", "نظم تشغيل", "تراكيب بيانات", "هندسة مدنية"
		);
		List<String> arabicNames = Arrays.asList(
			"أحمد علي", "محمد حسن", "خالد يوسف", "سلمان العتيبي", "عبدالله القحطاني",
			"فهد المطيري", "سعيد الغامدي", "حسين السلمي", "عبدالرحمن الشهراني", "ماجد الزهراني",
			"تركي الدوسري", "سعود الشمري", "عبدالعزيز التركي", "يوسف الحربي", "فيصل الجهني",
			"إبراهيم الرشيدي", "ناصر الشهري", "طارق المطيري", "عبدالمجيد الحارثي", "بندر السبيعي",
			"عمر العبدالله", "راشد القحطاني", "خالد الدوسري", "حسن البقمي", "وليد الحربي",
			"مساعد المطيري", "هيثم الزهراني", "زياد الشمراني", "سامي القاسم", "حمد الدوسري",
			"مشعل الحربي", "رمزي القرني", "باسل العتيبي", "قصي السويلم", "ثامر الشهراني",
			"ليث الحارثي", "أمين الزايدي", "فهد الزايدي", "خليل الحربي", "محمد العوفي"
		);

		try (FileInputStream fis = new FileInputStream(file); XSSFWorkbook wb = new XSSFWorkbook(fis)) {
			Sheet subjectsSheet = wb.getSheet("المواد");
			Sheet supervisorsSheet = wb.getSheet("المراقبون");

			// Fill subjects with real names and set required supervisors = 2
			int r = 2; // start after header row and example row
			LocalDate start = next(DayOfWeek.SATURDAY);
			for (int i = 0; i < numSessions; i++) {
				String id = "S" + (i + 1);
				String name = subjects.get(i % subjects.size());
				String building = buildings.get(i % buildings.size());
				String period = (i % 2 == 0) ? "صباحي" : "مسائي";
				DayOfWeek dow = DayOfWeek.of(((i % 7) + 6) % 7 + 1); // start from Saturday
				LocalDate date = nextOnOrAfter(start, dow).plusDays((i / 7) * 7);
				String arabicDay = arabicDay(dow);
				String from = (i % 2 == 0) ? "10:00" : "01:00";
				String to = (i % 2 == 0) ? "12:00" : "03:00";
				int required = 2;
				addSubjectRow(subjectsSheet, r++, id, name, building, period, arabicDay, date.toString(), from, to, required);
			}

			// Supervisors: 85 names => first 10 floors (100%), 5 maintenance (100%), remaining 70 invigilators (100%).
			int s = 2;
			String availableDays = String.join(", ", Arrays.asList("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس"));
			for (int i = 0; i < 85; i++) {
				String base = arabicNames.get(i % arabicNames.size());
				String name = base + (i >= arabicNames.size() ? (" " + (i / arabicNames.size() + 1)) : "");
				String role;
				String excludedSubjects = "";
				if (i < 10) {
					role = "مشرف دور";
				} else if (i < 15) {
					role = "عامل";
				} else {
					role = "ملاحظ";
					// Some invigilators have excluded subjects
					if (i % 10 == 0) {
						excludedSubjects = subjects.get(i % subjects.size());
					}
				}
				int pct = 100;
				addSupervisorRow(supervisorsSheet, s++, name, availableDays, pct, role, excludedSubjects);
			}

			try (FileOutputStream fos = new FileOutputStream(file)) { wb.write(fos); }
		}
	}

	private static void addSubjectRow(Sheet sheet, int rowIdx, String id, String subject, String building, String period, String day, String date, String from, String to, int req) {
		Row row = sheet.createRow(rowIdx);
		row.createCell(0).setCellValue(id);
		row.createCell(1).setCellValue(subject);
		row.createCell(2).setCellValue(building);
		row.createCell(3).setCellValue(period);
		row.createCell(4).setCellValue(day);
		row.createCell(5).setCellValue(date);
		row.createCell(6).setCellValue(from);
		row.createCell(7).setCellValue(to);
		row.createCell(8).setCellValue(req);
	}

	private static void addSupervisorRow(Sheet sheet, int rowIdx, String name, String days, int pct, String role, String excludedSubjects) {
		Row row = sheet.createRow(rowIdx);
		row.createCell(0).setCellValue(name);
		row.createCell(1).setCellValue(days);
		row.createCell(2).setCellValue(pct);
		row.createCell(3).setCellValue(role);
		row.createCell(4).setCellValue(excludedSubjects);
	}

	private static LocalDate next(DayOfWeek target) {
		LocalDate d = LocalDate.now();
		while (d.getDayOfWeek() != target) d = d.plusDays(1);
		return d;
	}

	private static LocalDate nextOnOrAfter(LocalDate start, DayOfWeek target) {
		LocalDate d = start;
		while (d.getDayOfWeek() != target) d = d.plusDays(1);
		return d;
	}

	private static String arabicDay(DayOfWeek dow) {
		switch (dow) {
			case SATURDAY: return "السبت";
			case SUNDAY: return "الأحد";
			case MONDAY: return "الاثنين";
			case TUESDAY: return "الثلاثاء";
			case WEDNESDAY: return "الأربعاء";
			case THURSDAY: return "الخميس";
			case FRIDAY: return "الجمعة";
			default: return "السبت";
		}
	}
}
