package com.supervisor.assignment.integration;

import com.supervisor.assignment.model.Config;
import com.supervisor.assignment.template.TemplateGenerator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class IntegrationSample {
    public static void main(String[] args) throws Exception {
        File out = new File("target/integration_input.xlsx");
        out.getParentFile().mkdirs();
        new TemplateGenerator().generateTemplate(out, new Config());
        try (FileInputStream fis = new FileInputStream(out); XSSFWorkbook wb = new XSSFWorkbook(fis)) {
            Sheet subjects = wb.getSheet("Subjects");
            Sheet supervisors = wb.getSheet("Supervisors");

            // Subjects: headers at row index 1, start data at row index 2
            Row s1 = subjects.createRow(2);
            s1.createCell(0).setCellValue("S1");
            s1.createCell(1).setCellValue("Math");
            s1.createCell(2).setCellValue("Saturday");
            s1.createCell(3).setCellValue("10:00");
            s1.createCell(4).setCellValue("12:00");
            s1.createCell(5).setCellValue(1);

            Row s2 = subjects.createRow(3);
            s2.createCell(0).setCellValue("S2");
            s2.createCell(1).setCellValue("Physics");
            s2.createCell(2).setCellValue("Saturday");
            s2.createCell(3).setCellValue("12:00");
            s2.createCell(4).setCellValue("13:30");
            s2.createCell(5).setCellValue(1);

            Row s3 = subjects.createRow(4);
            s3.createCell(0).setCellValue("S3");
            s3.createCell(1).setCellValue("Chemistry");
            s3.createCell(2).setCellValue("Saturday");
            s3.createCell(3).setCellValue("13:30");
            s3.createCell(4).setCellValue("14:30");
            s3.createCell(5).setCellValue(1);

            // Supervisors
            Row p1 = supervisors.createRow(2);
            p1.createCell(0).setCellValue("Ahmed");
            p1.createCell(1).setCellValue("Saturday, Sunday");

            Row p2 = supervisors.createRow(3);
            p2.createCell(0).setCellValue("Sara");
            p2.createCell(1).setCellValue("Saturday");

            Row p3 = supervisors.createRow(4);
            p3.createCell(0).setCellValue("Omar");
            p3.createCell(1).setCellValue("Saturday");

            try (FileOutputStream fos = new FileOutputStream(out)) {
                wb.write(fos);
            }
        }
        System.out.println("Sample input written: " + out.getAbsolutePath());
    }
}


