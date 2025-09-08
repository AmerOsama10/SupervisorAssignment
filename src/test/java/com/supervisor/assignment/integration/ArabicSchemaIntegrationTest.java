package com.supervisor.assignment.integration;

import com.supervisor.assignment.io.ExcelReaderWriter;
import com.supervisor.assignment.logic.AssignmentEngine;
import com.supervisor.assignment.model.AssignmentResult;
import com.supervisor.assignment.model.Config;
import com.supervisor.assignment.template.TemplateGenerator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ArabicSchemaIntegrationTest {

    @Test
    public void endToEndArabicSchema() throws Exception {
        File outDir = new File("target/test-excels");
        if (!outDir.exists()) outDir.mkdirs();
        File inXlsx = new File(outDir, "input_arabic_small.xlsx");
        File outXlsx = new File(outDir, "output_arabic_small.xlsx");

        new TemplateGenerator().generateTemplate(inXlsx, new Config());

        try (FileInputStream fis = new FileInputStream(inXlsx); XSSFWorkbook wb = new XSSFWorkbook(fis)) {
            Sheet subjects = wb.getSheet("المواد");
            Sheet supervisors = wb.getSheet("المراقبون");

            // Two buildings, morning/evening, small set
            int r = 2;
            // Row: المعرف، المادة، المبنى، الفترة، اليوم، التاريخ، من، إلى، عدد_الملاحظين
            addSubjectRow(subjects, r++, "S1", "رياضيات", "المبنى الرابع - الدور الثاني", "صباحي", "السبت", nextDateFor("السبت"), "08:00", "10:00", 2);
            addSubjectRow(subjects, r++, "S2", "كيمياء", "المبنى الخامس - الدور الثالث", "مسائي", "السبت", nextDateFor("السبت"), "01:00", "03:00", 1);
            addSubjectRow(subjects, r++, "S3", "لغة عربية", "المبنى الرابع - الدور الثاني", "صباحي", "الأحد", nextDateFor("الأحد"), "09:00", "11:00", 1);
            addSubjectRow(subjects, r++, "S4", "تاريخ", "المبنى الخامس - الدور الثالث", "مسائي", "الأحد", nextDateFor("الأحد"), "02:00", "04:00", 2);

            // Supervisors: mix of ملاحظ and مشرف دور
            int s = 2;
            addSupervisorRow(supervisors, s++, "أحمد علي", "السبت, الأحد", 100, "ملاحظ");
            addSupervisorRow(supervisors, s++, "محمد حسن", "السبت, الأحد", 100, "ملاحظ");
            addSupervisorRow(supervisors, s++, "محمود إبراهيم", "السبت, الأحد", 50, "ملاحظ");
            addSupervisorRow(supervisors, s++, "خالد يوسف", "السبت, الأحد", 100, "مشرف دور");
            addSupervisorRow(supervisors, s++, "حسين سالم", "السبت, الأحد", 100, "مشرف دور");

            try (FileOutputStream fos = new FileOutputStream(inXlsx)) { wb.write(fos); }
        }

        ExcelReaderWriter rw = new ExcelReaderWriter();
        ExcelReaderWriter.ParsedInput pi = rw.readWorkbook(inXlsx);
        AssignmentEngine engine = new AssignmentEngine();
        AssignmentResult result = engine.assign(pi.sessions, pi.supervisors, pi.config);
        rw.writeOutput(inXlsx, outXlsx, result);

        assertTrue(outXlsx.exists());

        // Open output and verify key sheets/headers
        try (FileInputStream fis = new FileInputStream(outXlsx); XSSFWorkbook wb = new XSSFWorkbook(fis)) {
            Sheet assigns = wb.getSheet("التعيينات");
            assertNotNull(assigns, "Missing التعيينات sheet");
            Row header = assigns.getRow(0);
            assertEquals("المبنى", header.getCell(2).getStringCellValue());
            assertEquals("الفترة", header.getCell(3).getStringCellValue());

            Sheet totals = wb.getSheet("إجمالي المراقبين");
            assertNotNull(totals, "Missing إجمالي المراقبين sheet");

            Sheet schedules = wb.getSheet("جداول المراقبين");
            assertNotNull(schedules, "Missing جداول المراقبين sheet");

            Sheet backups = wb.getSheet("الاحتياط");
            assertNotNull(backups, "Missing الاحتياط sheet");
            Row bHeader = backups.getRow(0);
            assertEquals("الفترة", bHeader.getCell(2).getStringCellValue());
        }
    }

    private static String nextDateFor(String arabicDay) {
        java.time.DayOfWeek target = mapArabicDay(arabicDay);
        java.time.LocalDate d = java.time.LocalDate.now();
        while (d.getDayOfWeek() != target) d = d.plusDays(1);
        return d.toString();
    }

    private static java.time.DayOfWeek mapArabicDay(String d) {
        switch (d) {
            case "السبت": return java.time.DayOfWeek.SATURDAY;
            case "الأحد": return java.time.DayOfWeek.SUNDAY;
            case "الاثنين": return java.time.DayOfWeek.MONDAY;
            case "الثلاثاء": return java.time.DayOfWeek.TUESDAY;
            case "الأربعاء": return java.time.DayOfWeek.WEDNESDAY;
            case "الخميس": return java.time.DayOfWeek.THURSDAY;
            case "الجمعة": return java.time.DayOfWeek.FRIDAY;
            default: return java.time.DayOfWeek.SATURDAY;
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

    private static void addSupervisorRow(Sheet sheet, int rowIdx, String name, String days, int pct, String role) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(days);
        row.createCell(2).setCellValue(pct);
        row.createCell(3).setCellValue(role);
    }
}


