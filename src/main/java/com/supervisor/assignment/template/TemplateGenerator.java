package com.supervisor.assignment.template;

import com.supervisor.assignment.model.Config;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TemplateGenerator {
	public void generateTemplate(File outFile, Config defaults) throws IOException {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			createDaysSheetArabic(wb);
			createSubjectsSheetArabic(wb);
			createSupervisorsSheetArabic(wb);
			try (FileOutputStream fos = new FileOutputStream(outFile)) {
				wb.write(fos);
			}
		}
	}

	private void createDaysSheetArabic(XSSFWorkbook wb) {
		XSSFSheet sheet = wb.createSheet("الأيام");
		sheet.setRightToLeft(true);
		Row header = sheet.createRow(0);
		header.createCell(0).setCellValue("أيام الأسبوع");

		String[] days = new String[]{"السبت","الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة"};
		for (int i = 0; i < days.length; i++) {
			Row row = sheet.getRow(i+1);
			if (row == null) row = sheet.createRow(i+1);
			row.createCell(0).setCellValue(days[i]);
		}
	}

	private void createSubjectsSheetArabic(XSSFWorkbook wb) {
		XSSFSheet sheet = wb.createSheet("المواد");
		sheet.setRightToLeft(true);
		int r = 0;
		Row note = sheet.createRow(r++);
		note.createCell(0).setCellValue("أدخل الجلسات أدناه. اختر الفترة (صباحي/مسائي)، اليوم من قائمة الأيام. الوقت بصيغة 12 ساعة (مثال 01:00).");

		Row header = sheet.createRow(r++);
		String[] cols = new String[]{"المعرف","المادة","المبنى","الفترة","اليوم","التاريخ","من","إلى","عدد_الملاحظين"};
		for (int c = 0; c < cols.length; c++) header.createCell(c).setCellValue(cols[c]);
		sheet.createFreezePane(0, r);

		DataValidationHelper dvHelper = sheet.getDataValidationHelper();
		DataValidationConstraint dayConstraint = dvHelper.createFormulaListConstraint("الأيام!$A$2:$A$8");
		CellRangeAddressList dayAddr = new CellRangeAddressList(2, 2000, 4, 4);
		sheet.addValidationData(dvHelper.createValidation(dayConstraint, dayAddr));

		DataValidationConstraint periodConstraint = dvHelper.createExplicitListConstraint(new String[]{"صباحي","مسائي"});
		CellRangeAddressList periodAddr = new CellRangeAddressList(2, 2000, 3, 3);
		sheet.addValidationData(dvHelper.createValidation(periodConstraint, periodAddr));

		sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, cols.length-1));

		// Prefill example rows (like a sample) to guide users
		int start = r;
		java.time.LocalDate sat = next(java.time.DayOfWeek.SATURDAY);
		addSubjectRow(sheet.createRow(start++), "S1", "رياضيات عامة 101", "المبنى الرابع - الدور الثاني", "صباحي", "السبت", sat.toString(), "10:00", "12:00", 2);
		addSubjectRow(sheet.createRow(start++), "S2", "كيمياء عامة 101", "المبنى الخامس - الدور الثالث", "مسائي", "السبت", sat.toString(), "01:00", "03:00", 2);
		addSubjectRow(sheet.createRow(start++), "S3", "لغة عربية 101", "المبنى الرابع - الدور الثاني", "صباحي", "الأحد", nextFor("الأحد").toString(), "10:00", "12:00", 2);
		addSubjectRow(sheet.createRow(start++), "S4", "هندسة برمجيات", "المبنى الجديد - الدور الأول", "مسائي", "الاثنين", nextFor("الاثنين").toString(), "01:00", "03:00", 2);
	}

	private void createSupervisorsSheetArabic(XSSFWorkbook wb) {
		XSSFSheet sheet = wb.createSheet("المراقبون");
		sheet.setRightToLeft(true);
		int r = 0;
		Row note = sheet.createRow(r++);
		note.createCell(0).setCellValue("الأيام المتاحة: قيم مفصولة بفواصل من القوائم. مثال: السبت, الأحد. الوظيفة: ملاحظ أو مشرف دور. النسبة٪: 100 تعني كامل الحمل.");
		Row header = sheet.createRow(r++);
		String[] cols = new String[]{"الاسم","الأيام المتاحة","النسبة٪","الوظيفة"};
		for (int c = 0; c < cols.length; c++) header.createCell(c).setCellValue(cols[c]);
		sheet.createFreezePane(0, r);

		DataValidationHelper dvHelper = sheet.getDataValidationHelper();
		DataValidationConstraint roleConstraint = dvHelper.createExplicitListConstraint(new String[]{"ملاحظ","مشرف دور"});
		CellRangeAddressList roleAddr = new CellRangeAddressList(2, 2000, 3, 3);
		sheet.addValidationData(dvHelper.createValidation(roleConstraint, roleAddr));

		// Prefill example supervisors (90 ملاحظ، 10 مشرف دور - هنا فقط أمثلة قليلة)
		int start = r;
		addSupRow(sheet.createRow(start++), "أحمد علي", "السبت, الأحد, الاثنين, الثلاثاء, الأربعاء, الخميس", 100, "ملاحظ");
		addSupRow(sheet.createRow(start++), "محمد حسن", "السبت, الأحد, الاثنين, الثلاثاء, الأربعاء, الخميس", 100, "ملاحظ");
		addSupRow(sheet.createRow(start++), "خالد يوسف", "السبت, الأحد, الاثنين, الثلاثاء, الأربعاء, الخميس", 100, "ملاحظ");
		addSupRow(sheet.createRow(start++), "سلمان العتيبي", "السبت, الأحد, الاثنين, الثلاثاء, الأربعاء, الخميس", 100, "ملاحظ");
		addSupRow(sheet.createRow(start++), "حسين سالم", "السبت, الأحد, الاثنين, الثلاثاء, الأربعاء, الخميس", 100, "مشرف دور");
	}

	private void addSubjectRow(Row row, String id, String subject, String building, String period, String day, String date, String from, String to, int req) {
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

	private void addSupRow(Row row, String name, String days, int pct, String role) {
		row.createCell(0).setCellValue(name);
		row.createCell(1).setCellValue(days);
		row.createCell(2).setCellValue(pct);
		row.createCell(3).setCellValue(role);
	}

	private java.time.LocalDate next(java.time.DayOfWeek dow) {
		java.time.LocalDate d = java.time.LocalDate.now();
		while (d.getDayOfWeek() != dow) d = d.plusDays(1);
		return d;
	}

	private java.time.LocalDate nextFor(String arabicDay) {
		switch (arabicDay) {
			case "السبت": return next(java.time.DayOfWeek.SATURDAY);
			case "الأحد": return next(java.time.DayOfWeek.SUNDAY);
			case "الاثنين": return next(java.time.DayOfWeek.MONDAY);
			case "الثلاثاء": return next(java.time.DayOfWeek.TUESDAY);
			case "الأربعاء": return next(java.time.DayOfWeek.WEDNESDAY);
			case "الخميس": return next(java.time.DayOfWeek.THURSDAY);
			case "الجمعة": return next(java.time.DayOfWeek.FRIDAY);
			default: return next(java.time.DayOfWeek.SATURDAY);
		}
	}
}


