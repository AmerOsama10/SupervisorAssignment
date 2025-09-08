package com.supervisor.assignment.cli;

import com.supervisor.assignment.io.ExcelReaderWriter;
import com.supervisor.assignment.logic.AssignmentEngine;
import com.supervisor.assignment.model.AssignmentResult;
import com.supervisor.assignment.model.Config;
import com.supervisor.assignment.template.TemplateGenerator;

import java.io.File;

public class CliRunner {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage:\n  template <output.xlsx>\n  assign <input.xlsx> <output.xlsx>");
            System.exit(1);
        }
        String cmd = args[0];
        switch (cmd) {
            case "template":
                if (args.length != 2) {
                    System.err.println("Usage: template <output.xlsx>");
                    System.exit(2);
                }
                File out = new File(args[1]);
                try {
                    // Instead of empty template, prefill with example similar to input_large_A.xlsx
                    new TemplateGenerator().generateTemplate(out, new Config());
                } catch (Exception e) {
                    System.err.println("Failed generating template: " + e.getMessage());
                }
                System.out.println("Template written: " + out.getAbsolutePath());
                break;
            case "assign":
                if (args.length != 3) {
                    System.err.println("Usage: assign <input.xlsx> <output.xlsx>");
                    System.exit(2);
                }
                File inFile = new File(args[1]);
                File outFile = new File(args[2]);
                ExcelReaderWriter rw = new ExcelReaderWriter();
                ExcelReaderWriter.ParsedInput pi = rw.readWorkbook(inFile);
                AssignmentEngine engine = new AssignmentEngine();
                AssignmentResult result = engine.assign(pi.sessions, pi.supervisors, pi.config);
                rw.writeOutput(inFile, outFile, result);
                System.out.println("Assigned: " + result.getSessionAssignments().stream().filter(a -> "Assigned".equals(a.getStatus())).count());
                System.out.println("Partial: " + result.getSessionAssignments().stream().filter(a -> "PartiallyAssigned".equals(a.getStatus())).count());
                System.out.println("Unassigned: " + result.getSessionAssignments().stream().filter(a -> "Unassigned".equals(a.getStatus())).count());
                System.out.println("Output: " + outFile.getAbsolutePath());
                break;
            case "pdf":
                if (args.length != 3) {
                    System.err.println("Usage: pdf <input.xlsx> <output.pdf>");
                    System.exit(2);
                }
                File inXlsx = new File(args[1]);
                File outPdf = new File(args[2]);
                ExcelReaderWriter rw2 = new ExcelReaderWriter();
                ExcelReaderWriter.ParsedInput pi2 = rw2.readWorkbook(inXlsx);
                AssignmentEngine engine2 = new AssignmentEngine();
                AssignmentResult result2 = engine2.assign(pi2.sessions, pi2.supervisors, pi2.config);
                new com.supervisor.assignment.io.PdfExporter().exportArabic(outPdf, result2);
                System.out.println("PDF: " + outPdf.getAbsolutePath());
                break;
            default:
                System.err.println("Unknown command: " + cmd);
                System.exit(3);
        }
    }
}


