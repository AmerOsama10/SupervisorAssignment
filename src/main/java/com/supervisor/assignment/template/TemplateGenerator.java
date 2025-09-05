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
			createListsSheetArabic(wb);
			createSubjectsSheetArabic(wb);
			createSupervisorsSheetArabic(wb);
			try (FileOutputStream fos = new FileOutputStream(outFile)) {
				wb.write(fos);
			}
		}
	}

	private void createListsSheetArabic(XSSFWorkbook wb) {
		XSSFSheet sheet = wb.createSheet("القوائم");
		Row header = sheet.createRow(0);
		header.createCell(0).setCellValue("أيام الأسبوع");
		header.createCell(1).setCellValue("الأوقات");

		String[] days = new String[]{"السبت","الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة"};
		for (int i = 0; i < days.length; i++) {
			Row row = sheet.getRow(i+1);
			if (row == null) row = sheet.createRow(i+1);
			row.createCell(0).setCellValue(days[i]);
		}
		int r = 1;
		for (int h = 0; h < 24; h++) {
			Row row = sheet.getRow(r);
			if (row == null) row = sheet.createRow(r);
			row.createCell(1).setCellValue(String.format("%02d:00", h));
			r++;
			Row row2 = sheet.getRow(r);
			if (row2 == null) row2 = sheet.createRow(r);
			row2.createCell(1).setCellValue(String.format("%02d:30", h));
			r++;
		}
		// Trim any extra rows after Friday (keep only 7 day rows in col A)
		int lastDayRow = 7; // A2..A8 are days
		for (int rr = lastDayRow + 1; rr <= sheet.getLastRowNum(); rr++) {
			Row extra = sheet.getRow(rr);
			if (extra != null) {
				Cell c0 = extra.getCell(0);
				if (c0 != null) c0.setBlank();
			}
		}
		wb.setSheetHidden(wb.getSheetIndex(sheet), true);
	}

	private void createSubjectsSheetArabic(XSSFWorkbook wb) {
		Sheet sheet = wb.createSheet("المواد");
		int r = 0;
		Row note = sheet.createRow(r++);
		note.createCell(0).setCellValue("أدخل الجلسات أدناه. اليوم/الوقت بقوائم منسدلة. التاريخ مطلوب.");

		Row header = sheet.createRow(r++);
		String[] cols = new String[]{"المعرف","المادة","اليوم","التاريخ","من","إلى","عدد_المشرفين"};
		for (int c = 0; c < cols.length; c++) header.createCell(c).setCellValue(cols[c]);
		sheet.createFreezePane(0, r);

		DataValidationHelper dvHelper = sheet.getDataValidationHelper();
		DataValidationConstraint dayConstraint = dvHelper.createFormulaListConstraint("القوائم!$A$2:$A$8");
		CellRangeAddressList dayAddr = new CellRangeAddressList(2, 2000, 2, 2);
		sheet.addValidationData(dvHelper.createValidation(dayConstraint, dayAddr));

		int lastTimeRow = wb.getSheet("القوائم").getLastRowNum() + 1;
		String timeRange = String.format("القوائم!$B$2:$B$%d", lastTimeRow);
		DataValidationConstraint timeConstraint = dvHelper.createFormulaListConstraint(timeRange);
		CellRangeAddressList fromAddr = new CellRangeAddressList(2, 2000, 4, 4);
		CellRangeAddressList toAddr = new CellRangeAddressList(2, 2000, 5, 5);
		sheet.addValidationData(dvHelper.createValidation(timeConstraint, fromAddr));
		sheet.addValidationData(dvHelper.createValidation(timeConstraint, toAddr));

		sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, cols.length-1));
	}

	private void createSupervisorsSheetArabic(XSSFWorkbook wb) {
		Sheet sheet = wb.createSheet("المشرفون");
		int r = 0;
		Row note = sheet.createRow(r++);
		note.createCell(0).setCellValue("الأيام المتاحة: قيم مفصولة بفواصل من القوائم. مثال: السبت, الأحد");
		Row header = sheet.createRow(r++);
		String[] cols = new String[]{"المشرف","الأيام المتاحة","الحد الأقصى للساعات"};
		for (int c = 0; c < cols.length; c++) header.createCell(c).setCellValue(cols[c]);
		sheet.createFreezePane(0, r);
	}
}


