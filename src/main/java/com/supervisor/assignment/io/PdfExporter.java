package com.supervisor.assignment.io;

import com.supervisor.assignment.model.*;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PdfExporter {
	public void exportArabic(File outputPdf, AssignmentResult result) throws Exception {
		Document document = new Document(PageSize.A4, 36, 36, 36, 36);
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPdf));
		writer.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
		document.open();

		String fontPath = resolveArabicFontFile();
		BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
		com.lowagie.text.Font f12 = new com.lowagie.text.Font(bf, 12);
		com.lowagie.text.Font f14 = new com.lowagie.text.Font(bf, 14);

		Paragraph title = new Paragraph("ملخص التعيينات", f14);
		title.setAlignment(Element.ALIGN_RIGHT);
		document.add(title);

		List<AssignmentResult.SessionAssignment> sorted = new ArrayList<>(result.getSessionAssignments());
		sorted.sort(
			Comparator
				.comparing((AssignmentResult.SessionAssignment sa) -> sa.getSession().getDate(), Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(sa -> sa.getSession().getFrom())
		);

		for (AssignmentResult.SessionAssignment sa : sorted) {
			SubjectSession s = sa.getSession();
			String line = String.format("%s | %s | %s - %s | %s",
				s.getSubjectName(),
				arabicDay(s.getDay()),
				s.getFrom(),
				s.getTo(),
				String.join(", ", sa.getAssignedSupervisors())
			);
			Paragraph p = new Paragraph(line, f12);
			p.setAlignment(Element.ALIGN_RIGHT);
			document.add(p);
		}

		document.close();
	}

	private String resolveArabicFontFile() throws Exception {
		// Prefer system fonts
		File arialUnicode = new File("/Library/Fonts/Arial Unicode.ttf");
		if (arialUnicode.exists()) return arialUnicode.getAbsolutePath();
		File geeza = new File("/System/Library/Fonts/Supplemental/GeezaPro.ttf");
		if (geeza.exists()) return geeza.getAbsolutePath();
		// Fallback from classpath
		InputStream in = PdfExporter.class.getResourceAsStream("/NotoNaskhArabic-Regular.ttf");
		if (in != null) {
			File tmp = File.createTempFile("arabic-font", ".ttf");
			Files.copy(in, tmp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			return tmp.getAbsolutePath();
		}
		throw new IllegalStateException("لم يتم العثور على خط عربي مناسب للنظام أو الموارد");
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
}
