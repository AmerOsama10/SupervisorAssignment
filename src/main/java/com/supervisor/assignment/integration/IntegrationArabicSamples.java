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

public class IntegrationArabicSamples {
	public static void main(String[] args) throws Exception {
		File out = new File("عينة_مدخلات.xlsx");
		new TemplateGenerator().generateTemplate(out, new Config());
		try (FileInputStream fis = new FileInputStream(out); XSSFWorkbook wb = new XSSFWorkbook(fis)) {
			Sheet subjects = wb.getSheet("المواد");
			Sheet supervisors = wb.getSheet("المشرفون");

			// Subjects with dates
			Row s1 = subjects.createRow(2);
			s1.createCell(0).setCellValue("م1");
			s1.createCell(1).setCellValue("رياضيات");
			s1.createCell(2).setCellValue("السبت");
			s1.createCell(3).setCellValue(nextDateFor(DayOfWeek.SATURDAY).toString());
			s1.createCell(4).setCellValue("10:00");
			s1.createCell(5).setCellValue("12:00");
			s1.createCell(6).setCellValue(2);

			Row s2 = subjects.createRow(3);
			s2.createCell(0).setCellValue("م2");
			s2.createCell(1).setCellValue("فيزياء");
			s2.createCell(2).setCellValue("الأحد");
			s2.createCell(3).setCellValue(nextDateFor(DayOfWeek.SUNDAY).toString());
			s2.createCell(4).setCellValue("12:00");
			s2.createCell(5).setCellValue("15:00");
			s2.createCell(6).setCellValue(2);

			Row s3 = subjects.createRow(4);
			s3.createCell(0).setCellValue("م3");
			s3.createCell(1).setCellValue("كيمياء");
			s3.createCell(2).setCellValue("الاثنين");
			s3.createCell(3).setCellValue(nextDateFor(DayOfWeek.MONDAY).toString());
			s3.createCell(4).setCellValue("09:30");
			s3.createCell(5).setCellValue("11:30");
			s3.createCell(6).setCellValue(1);

			// Supervisors (Arabic names)
			Row p1 = supervisors.createRow(2);
			p1.createCell(0).setCellValue("أحمد");
			p1.createCell(1).setCellValue("السبت, الأحد, الاثنين, الثلاثاء, الأربعاء, الخميس");
			p1.createCell(2).setCellValue(12);

			Row p2 = supervisors.createRow(3);
			p2.createCell(0).setCellValue("سارة");
			p2.createCell(1).setCellValue("السبت, الأحد, الاثنين");
			p2.createCell(2).setCellValue(8);

			Row p3 = supervisors.createRow(4);
			p3.createCell(0).setCellValue("عمر");
			p3.createCell(1).setCellValue("السبت, الثلاثاء, الأربعاء, الخميس");
			p3.createCell(2).setCellValue(10);

			try (FileOutputStream fos = new FileOutputStream(out)) {
				wb.write(fos);
			}
		}

		System.out.println("تم إنشاء ملف العينة: " + out.getAbsolutePath());
	}

	private static LocalDate nextDateFor(DayOfWeek dow) {
		LocalDate d = LocalDate.now();
		while (d.getDayOfWeek() != dow) d = d.plusDays(1);
		return d;
	}
}
