package com.supervisor.assignment.integration;

import com.supervisor.assignment.model.Config;
import com.supervisor.assignment.template.TemplateGenerator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class IntegrationLargeSample {
	public static void main(String[] args) throws Exception {
		// Ensure test-excels directory exists
		File outDir = new File("test-excels");
		if (!outDir.exists()) outDir.mkdirs();
		// Create template in test-excels folder (Arabic filename)
		File out = new File(outDir, "عينة_مدخل_٣أسابيع.xlsx");
		new TemplateGenerator().generateTemplate(out, new Config());

		try (FileInputStream fis = new FileInputStream(out); XSSFWorkbook wb = new XSSFWorkbook(fis)) {
			Sheet subjects = wb.getSheet("المواد");
			Sheet supervisors = wb.getSheet("المشرفون");

			DayOfWeek[] allowed = new DayOfWeek[]{DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY};
			String[] arabicDays = new String[]{"السبت","الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس"};

			// Realistic subject bases and topics
			String[] baseSubjects = new String[]{
				"الرياضيات","الفيزياء","الكيمياء","الأحياء","اللغة العربية","اللغة الإنجليزية","التاريخ","الجغرافيا",
				"علوم الحاسوب","برمجة","هندسة برمجيات","قواعد البيانات","الشبكات","نظم التشغيل","الفنون","التربية الدينية",
				"التربية الوطنية","الاقتصاد","المحاسبة","الإحصاء","الأدب العربي","النحو","البلاغة","التربية الرياضية",
				"الفيزياء الحديثة","علم النفس","علم الاجتماع","الفلسفة","المنطق","أمن المعلومات"
			};
			String[] topics = new String[]{
				"الجبر","الهندسة","التفاضل","الميكانيكا","الكهرباء","البصريات","العضوية","اللاعضوية","الوراثة",
				"النصوص","القواعد","الاستماع","القراءة","الكتابة","برمجة كائنية","الخوارزميات","هياكل البيانات",
				"الشبكات المحلية","نظم موزعة","قواعد SQL","تحليل البيانات","تحليل نصوص","تاريخ حديث","تاريخ قديم",
				"جغرافيا بشرية","جغرافيا طبيعية","التربية","التحليل الإحصائي","الحساب","تحليل نظم"
			};
			java.util.Set<String> usedSubjects = new java.util.HashSet<>();

			// Generate 200 sessions across 3 weeks (skip Friday), durations 1..5 hours
			int dataStartRow = 2; // header at row 1; data begins at row index 2
			for (int i = 0; i < 200; i++) {
				Row row = subjects.createRow(dataStartRow + i);
				String id = String.format("S%03d", i + 1);
				// ثلاث فئات: الصف الأول/الثاني/الثالث
				String grade = (i % 3 == 0) ? "الصف الأول" : (i % 3 == 1) ? "الصف الثاني" : "الصف الثالث";
				String base = baseSubjects[(i * 7) % baseSubjects.length];
				String topic = topics[(i * 11) % topics.length];
				String subject = grade + " - " + base + ": " + topic;
				// ensure unique subject names
				int dedup = 1;
				while (usedSubjects.contains(subject)) {
					subject = grade + " - " + base + ": " + topic + " (مستوى " + (++dedup) + ")";
				}
				usedSubjects.add(subject);
				int dayIdx = i % allowed.length;
				String day = arabicDays[dayIdx];
				LocalDate date = nextDateFor(allowed[dayIdx]).plusDays(i / allowed.length);
				int slot = i % 9; // 9 slots per day (08:00 - 16:00 inclusive start)
				int fromHour = 8 + slot; // 08:00 -> 16:00
				int durationHours = (i % 5) + 1; // 1..5 hours
				int toHour = Math.min(fromHour + durationHours, 20);
				String from = String.format("%02d:00", fromHour);
				String to = String.format("%02d:00", toHour);
				int supervisorsRequired = (i % 10 == 0) ? 3 : ((i % 4 == 0) ? 2 : 1);

				row.createCell(0).setCellValue(id);
				row.createCell(1).setCellValue(subject);
				row.createCell(2).setCellValue(day);
				row.createCell(3).setCellValue(date.toString());
				row.createCell(4).setCellValue(from);
				row.createCell(5).setCellValue(to);
				row.createCell(6).setCellValue(supervisorsRequired);
			}

			// Generate 50 realistic Arabic supervisor names available on all non-Friday days
			int supStartRow = 2;
			String[] first = new String[]{"محمد","أحمد","محمود","خالد","عبدالله","إبراهيم","مصطفى","حسن","حسين","يوسف","عمر","كريم","طارق","سعيد","إسلام","ياسر","وليد","إيهاب","رامي","شريف","عمرو","ماهر","أيمن","علاء","سامي","ناصر","باسم","رامز","هيثم","رائد","عبدالرحمن","حمزة","سالم","أمين","جمال","مدحت"};
			String[] middle = new String[]{"إبراهيم","خالد","محمد","أحمد","عبدالرحمن","مصطفى","حسن","حسين","يوسف","كامل","سعيد","محمود","طلال","فتحي","عبدالله","جلال","ماهر","علي","سامي","سالم"};
			String[] last = new String[]{"المليجي","عبدالعزيز","حجازي","المنسي","البدري","المصري","الحمادي","التميمي","العوضي","النجار","الشريف","المرسي","المغربي","الأنصاري","زيدان","الطهطاوي","العشري","البنا","البيومي","النعيمي","الخطيب","الخالدي","العتيبي","القرشي","المدني"};
			java.util.Set<String> usedNames = new java.util.HashSet<>();
			int created = 0;
			outer: for (String f : first) {
				for (String m : middle) {
					for (String l : last) {
						String name = f + " " + m + " " + l;
						if (usedNames.add(name)) {
							Row row = supervisors.createRow(supStartRow + created);
							row.createCell(0).setCellValue(name);
							row.createCell(1).setCellValue("السبت, الأحد, الاثنين, الثلاثاء, الأربعاء, الخميس");
							row.createCell(2).setCellValue(50);
							created++;
							if (created >= 50) break outer;
						}
					}
				}
			}

			try (FileOutputStream fos = new FileOutputStream(out)) {
				wb.write(fos);
			}
		}

		System.out.println("Large sample input written: " + out.getAbsolutePath());
	}

	private static LocalDate nextDateFor(DayOfWeek dow) {
		LocalDate d = LocalDate.now();
		while (d.getDayOfWeek() != dow) d = d.plusDays(1);
		return d;
	}
}


