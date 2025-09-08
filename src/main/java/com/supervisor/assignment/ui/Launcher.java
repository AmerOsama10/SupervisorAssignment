package com.supervisor.assignment.ui;

import com.supervisor.assignment.io.ExcelReaderWriter;
import com.supervisor.assignment.logic.AssignmentEngine;
import com.supervisor.assignment.model.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Launcher extends Application {

	private TextField inputPath;
	private TextArea outputArea;
	private RadioButton rbConsecutive;
	private RadioButton rbBreak;
	private RadioButton rbMixed;
	private CheckBox cbExcel;
	private TextField searchField;
	private TextArea searchResult;
	private ListView<String> suggestionList;
	private ComboBox<String> filterType;
	private DatePicker datePicker;
	private AssignmentResult lastResult;
	private I18n i18n;
	private VBox root;
	private Scene scene;
	private Label lblFile;
	private Label lblSummary;
	private Label lblSearch;
	private Label lblLanguage;
	private Button btnBrowse;
	private Button btnRun;
	private Button btnTemplate;
	private HBox fileRow;
	private HBox modesRow;
	private HBox exportRow;
	private HBox actionsRow;
	private VBox searchBox;

	@Override
	public void start(Stage stage) {
		this.i18n = new I18n(new Locale("ar"));
		stage.setTitle("تعيين المشرفين");
		// Generate a simple runtime icon (blue tile with white letter م) for Dock/Taskbar
		try {
			WritableImage iconImg = generateArabicIcon();
			if (iconImg != null) {
				stage.getIcons().add(iconImg);
			}
		} catch (Exception ignored) {}
		root = new VBox(10);
		root.setPadding(new Insets(14));
		root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

		HBox langRow = new HBox(8);
		ComboBox<String> langCombo = new ComboBox<>();
		langCombo.getItems().addAll(i18n.t("language.ar"), i18n.t("language.en"));
		langCombo.getSelectionModel().select(0);
		langCombo.valueProperty().addListener((obs, o, n) -> switchLanguage(n != null && n.contains("English") ? Locale.ENGLISH : new Locale("ar"), stage));
		lblLanguage = new Label("اللغة:");
		langRow.getChildren().addAll(lblLanguage, langCombo);
		langRow.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

		fileRow = new HBox(8);
		fileRow.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
		inputPath = new TextField();
		inputPath.getStyleClass().add("text-input");
		inputPath.setPromptText(i18n.t("browse"));
		btnBrowse = new Button(i18n.t("browse"));
		btnBrowse.setOnAction(e -> onBrowse(stage));
		lblFile = new Label("");
		fileRow.getChildren().addAll(btnBrowse, inputPath);
		HBox.setHgrow(inputPath, Priority.ALWAYS);

		ToggleGroup group = new ToggleGroup();
		rbConsecutive = new RadioButton("متتابع");
		rbConsecutive.setToggleGroup(group);
		rbMixed = new RadioButton("مختلط");
		rbMixed.setToggleGroup(group);
		rbMixed.setSelected(true);
		rbBreak = new RadioButton("مع فاصل بين الجلسات");
		rbBreak.setToggleGroup(group);
		modesRow = new HBox(12, rbBreak, rbMixed, rbConsecutive);
		modesRow.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

		cbExcel = new CheckBox("Excel");
		cbExcel.setSelected(true);
		exportRow = new HBox(12, cbExcel);
		exportRow.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

		btnRun = new Button("تشغيل");
		btnTemplate = new Button("تحميل المثال");
		btnRun.setOnAction(e -> onRun(stage));
		btnTemplate.setOnAction(e -> onDownloadExample(stage));
		actionsRow = new HBox(10, btnTemplate, btnRun);
		actionsRow.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

		filterType = new ComboBox<>();
		filterType.getItems().addAll("مراقب", "مادة", "تاريخ");
		filterType.getSelectionModel().select(0);
		datePicker = new DatePicker();
		datePicker.setVisible(false);
		filterType.valueProperty().addListener((o, ov, nv) -> {
			boolean byDate = "تاريخ".equals(nv);
			datePicker.setVisible(byDate);
			datePicker.setManaged(byDate);
			searchField.setDisable(byDate);
		});

		searchField = new TextField();
		searchField.setPromptText("ابحث بالاسم أو المادة");
		searchField.textProperty().addListener((obs, o, n) -> onSearchLive(n));
		suggestionList = new ListView<>();
		suggestionList.setPrefHeight(120);
		suggestionList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
			if (n != null) showSupervisorSchedule(n);
		});
		searchResult = new TextArea();
		searchResult.getStyleClass().add("search-area");
		searchResult.setEditable(false);
		searchResult.setPrefRowCount(10);
		lblSearch = new Label(i18n.t("search") + ":");
		Button btnSearch = new Button("بحث");
		btnSearch.setOnAction(e -> onSearch());
		searchField.setOnAction(e -> onSearch());
		HBox searchControls = new HBox(6, btnSearch, filterType, searchField, datePicker);
		searchBox = new VBox(6, lblSearch, searchControls, suggestionList);
		searchBox.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

		outputArea = new TextArea();
		outputArea.getStyleClass().add("summary-area");
		outputArea.setEditable(false);
		outputArea.setPrefRowCount(10);
		lblSummary = new Label("التحليل:");

		root.getChildren().addAll(langRow, fileRow, modesRow, exportRow, actionsRow, lblSummary, outputArea, searchBox, searchResult);
		scene = new Scene(root, 900, 640);
		scene.getStylesheets().add(getClass().getResource("/ar.css").toExternalForm());
		stage.setScene(scene);
		stage.show();
	}

	private WritableImage generateArabicIcon() {
		int w = 128, h = 128;
		Group root = new Group();
		Rectangle bg = new Rectangle(0, 0, w, h);
		bg.setArcWidth(24);
		bg.setArcHeight(24);
		bg.setFill(Color.web("#1e88e5"));
		Text t = new Text("م");
		t.setFill(Color.WHITE);
		t.setFont(Font.font("Arial", 84));
		t.setX(w / 2.0 - 24);
		t.setY(h / 2.0 + 28);
		root.getChildren().addAll(bg, t);
		SnapshotParameters sp = new SnapshotParameters();
		sp.setFill(Color.TRANSPARENT);
		return root.snapshot(sp, null);
	}

	private void switchLanguage(Locale locale, Stage stage) {
		this.i18n = new I18n(locale);
		boolean ar = !locale.getLanguage().equalsIgnoreCase("en");
		NodeOrientation dir = ar ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT;
		root.setNodeOrientation(dir);
		fileRow.setNodeOrientation(dir);
		modesRow.setNodeOrientation(dir);
		exportRow.setNodeOrientation(dir);
		actionsRow.setNodeOrientation(dir);
		searchBox.setNodeOrientation(dir);
		stage.setTitle("تعيين المشرفين");
		lblFile.setText("الملف:");
		rbConsecutive.setText("متتابع");
		rbMixed.setText("مختلط");
		rbBreak.setText("مع فاصل بين الجلسات");
		cbExcel.setText("Excel");
		lblSummary.setText("التحليل:");
		lblSearch.setText("بحث:");
		inputPath.setPromptText("استعراض...");
		btnBrowse.setText("استعراض");
		btnRun.setText("تشغيل");
		btnTemplate.setText("تحميل المثال");
		lblLanguage.setText("اللغة:");
	}

	private void onRun(Stage stage) {
		String path = inputPath.getText();
		if (path == null || path.trim().isEmpty()) {
			alert(i18n.t("error.no_file"));
			return;
		}
		File inFile = new File(path);
		if (!inFile.exists()) {
			alert(i18n.t("error.file_not_found", path));
			return;
		}
		try {
			ExcelReaderWriter rw = new ExcelReaderWriter();
			ExcelReaderWriter.ParsedInput pi = rw.readWorkbook(inFile);
			Config cfg = pi.config;
			cfg.setSchedulingMode(rbConsecutive.isSelected() ? SchedulingMode.CONSECUTIVE : rbBreak.isSelected() ? SchedulingMode.BREAK : SchedulingMode.MIXED);
			AssignmentEngine engine = new AssignmentEngine();
			lastResult = engine.assign(pi.sessions, pi.supervisors, cfg);

			File out = suggestOutput(inFile);
			if (cbExcel.isSelected()) {
				File xlsx = new File(out.getParentFile(), addSuffix(out.getName(), "_مع_التعيينات.xlsx"));
				rw.writeOutput(inFile, xlsx, lastResult);
			}
			// PDF export removed per requirements

			long assigned = lastResult.getSessionAssignments().stream().filter(a -> "Assigned".equals(a.getStatus())).count();
			long partial = lastResult.getSessionAssignments().stream().filter(a -> "PartiallyAssigned".equals(a.getStatus())).count();
			long unassigned = lastResult.getSessionAssignments().stream().filter(a -> "Unassigned".equals(a.getStatus())).count();
			StringBuilder sum = new StringBuilder();
			sum.append("عدد الجلسات: ").append(lastResult.getSessionAssignments().size()).append('\n');
			sum.append("تم تعيينها: ").append(assigned)
				.append(", جزئي: ").append(partial)
				.append(", غير معيّن: ").append(unassigned).append('\n');
			// تحليل إضافي
			double totalHours = lastResult.getTotalHoursNeeded();
			sum.append("إجمالي الساعات المطلوبة: ").append(String.format(java.util.Locale.US, "%.2f", totalHours)).append('\n');
			sum.append("عدد المشرفين: ").append(lastResult.getSupervisorTotals().size()).append('\n');
			java.util.List<AssignmentResult.SessionAssignment> sortedSessions = new java.util.ArrayList<>(lastResult.getSessionAssignments());
			sortedSessions.sort(java.util.Comparator
				.comparing((AssignmentResult.SessionAssignment sa) -> sa.getSession().getDate(), java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
				.thenComparing(sa -> sa.getSession().getFrom())
			);
			sortedSessions.forEach(sa -> {
				SubjectSession s = sa.getSession();
				sum.append(s.getDate()).append(" ")
					.append(s.getFrom()).append("-").append(s.getTo())
					.append(" | ").append(s.getSubjectName())
					.append(" -> ").append(String.join(", ", sa.getAssignedSupervisors()))
					.append(" [").append(sa.getStatus()).append("]")
					.append('\n');
			});
			outputArea.setText(sum.toString());
		} catch (Exception ex) {
			ex.printStackTrace();
			alert(i18n.t("error.generic", ex.getMessage()));
		}
	}

	private void onShowSchedules(Stage stage) {
		if (lastResult == null) {
			alert(i18n.t("error.run_assignment_first"));
			return;
		}
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
		fc.setInitialFileName("جداول_المشرفين.xlsx");
		File dest = fc.showSaveDialog(stage);
		if (dest == null) return;
		try {
			// Write a workbook containing only supervisor schedules
			ExcelReaderWriter rw = new ExcelReaderWriter();
			File temp = File.createTempFile("assign", ".xlsx");
			// Reuse input if path present; else create minimal workbook
			File inFile = inputPath.getText() != null ? new File(inputPath.getText()) : null;
			if (inFile != null && inFile.exists()) {
				rw.writeOutput(inFile, temp, lastResult);
			} else {
				// fallback: create from a minimal template
				com.supervisor.assignment.template.TemplateGenerator gen = new com.supervisor.assignment.template.TemplateGenerator();
				File tmpl = File.createTempFile("tmpl", ".xlsx");
				gen.generateTemplate(tmpl, new Config());
				rw.writeOutput(tmpl, temp, lastResult);
			}
			java.nio.file.Files.copy(temp.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			alert(i18n.t("print.pdf_created", dest.getAbsolutePath()));
			outputArea.appendText("\n" + dest.getAbsolutePath());
		} catch (Exception ex) {
			ex.printStackTrace();
			alert(i18n.t("error.template_creation", ex.getMessage()));
		}
	}

	private void onPrint(Stage stage) {
		if (lastResult == null) {
			alert(i18n.t("error.run_assignment_first"));
			return;
		}
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
		fc.setInitialFileName(i18n.t("print.report_filename"));
		File dest = fc.showSaveDialog(stage);
		if (dest == null) return;
		try {
			new com.supervisor.assignment.io.PdfExporter().exportArabic(dest, lastResult);
			alert(i18n.t("print.pdf_created", dest.getAbsolutePath()));
		} catch (Exception ex) {
			ex.printStackTrace();
			alert(i18n.t("error.pdf_creation", ex.getMessage()));
		}
	}

	private void onBrowse(Stage stage) {
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
		File f = fc.showOpenDialog(stage);
		if (f != null) inputPath.setText(f.getAbsolutePath());
	}

	private void onDownloadExample(Stage stage) {
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
		fc.setInitialFileName("input_large_A.xlsx");
		File out = fc.showSaveDialog(stage);
		if (out == null) return;
		try {
			File example = findExampleInput();
			if (example == null || !example.exists()) {
				alert("لم يتم العثور على ملف المثال input_large_A.xlsx");
				return;
			}
			java.nio.file.Files.copy(example.toPath(), out.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			alert("تم حفظ المثال: " + out.getAbsolutePath());
		} catch (Exception ex) {
			ex.printStackTrace();
			alert("فشل حفظ المثال: " + ex.getMessage());
		}
	}

	private File findExampleInput() {
		// Try a few likely locations
		String[] candidates = new String[] {
			"target/test-excels/input_large_A.xlsx",
			"test-excels/input_large_A.xlsx"
		};
		for (String rel : candidates) {
			File f = new File(rel);
			if (f.exists()) return f;
		}
		// Try relative to the running jar location
		try {
			File here = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
			for (String rel : candidates) {
				File f = new File(here, rel);
				if (f.exists()) return f;
			}
		} catch (Exception ignored) {}
		return null;
	}

	private void onSearch() {
		if (lastResult == null) {
			alert(i18n.t("error.run_assignment_first"));
			return;
		}
		String q = searchField.getText();
		if (q == null || q.trim().isEmpty()) {
			searchResult.setText("");
			return;
		}
		String query = q.trim().toLowerCase(Locale.ROOT);
		String filter = filterType.getSelectionModel().getSelectedItem();
		java.util.List<AssignmentResult.SessionAssignment> list = new java.util.ArrayList<>(lastResult.getSessionAssignments());
		list.sort(java.util.Comparator
			.comparing((AssignmentResult.SessionAssignment sa) -> sa.getSession().getDate(), java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
			.thenComparing(sa -> sa.getSession().getFrom())
		);
		StringBuilder sb = new StringBuilder();
		for (AssignmentResult.SessionAssignment sa : list) {
			SubjectSession s = sa.getSession();
			String subj = s.getSubjectName().toLowerCase(Locale.ROOT);
			boolean ok = false;
			if ("تاريخ".equals(filter)) {
				java.time.LocalDate d = datePicker.getValue();
				ok = (d != null && s.getDate() != null && s.getDate().equals(d));
			} else if ("مادة".equals(filter)) {
				ok = subj.contains(query);
			} else { // مراقب
				ok = sa.getAssignedSupervisors().stream().anyMatch(n -> n.toLowerCase(Locale.ROOT).contains(query));
			}
			if (ok) {
				sb.append(s.getDate()).append(" ")
					.append(s.getFrom()).append("-").append(s.getTo())
					.append(" | ").append(s.getSubjectName())
					.append(" -> ").append(String.join(", ", sa.getAssignedSupervisors()))
					.append('\n');
			}
		}
		if (sb.length() == 0) sb.append("لا توجد نتائج");
		searchResult.setText(sb.toString());
	}

	private void onSearchLive(String q) {
		if (lastResult == null) { searchResult.setText(""); return; }
		if (q == null || q.trim().isEmpty()) { searchResult.setText(""); return; }
		String query = q.trim().toLowerCase(Locale.ROOT);
		List<String> names = lastResult.getSupervisorTotals().stream().map(AssignmentResult.SupervisorTotals::getSupervisor).collect(java.util.stream.Collectors.toList());
		java.util.List<String> filtered = names.stream()
			.filter(n -> fuzzyMatch(n.toLowerCase(Locale.ROOT), query))
			.limit(8)
			.collect(java.util.stream.Collectors.toList());
		suggestionList.getItems().setAll(filtered);
		if (!filtered.isEmpty()) {
			showSupervisorSchedule(filtered.get(0));
		}
	}

	private void showSupervisorSchedule(String name) {
		StringBuilder sb = new StringBuilder();
		sb.append((i18n != null ? i18n.t("search") : "المشرف") + ": ").append(name).append('\n');
		lastResult.getSessionAssignments().stream()
			.filter(sa -> sa.getAssignedSupervisors().contains(name))
			.forEach(sa -> {
				SubjectSession s = sa.getSession();
				sb.append(s.getDate()).append(" ")
					.append(s.getFrom()).append("-").append(s.getTo())
					.append(" | ").append(s.getSubjectName()).append('\n');
			});
		searchResult.setText(sb.toString());
	}

	private boolean fuzzyMatch(String text, String pattern) {
		String a = normalizeArabic(text);
		String b = normalizeArabic(pattern);
		int ti = 0, pi = 0;
		while (ti < a.length() && pi < b.length()) {
			char tc = a.charAt(ti);
			char pc = b.charAt(pi);
			if (tc == pc) { pi++; }
			ti++;
		}
		return pi == b.length();
	}

	private String normalizeArabic(String s) {
		if (s == null) return "";
		String t = s.toLowerCase(java.util.Locale.ROOT);
		// unify Arabic characters
		t = t.replace('\u0623', '\u0627') // أ -> ا
			.replace('\u0625', '\u0627') // إ -> ا
			.replace('\u0622', '\u0627') // آ -> ا
			.replace('\u0649', '\u064a') // ى -> ي
			.replace('\u0629', '\u0647'); // ة -> ه
		// remove tatweel and diacritics if any
		t = t.replace("\u0640", "");
		return t;
	}

	private File suggestOutput(File input) {
		String name = input.getName();
		int dot = name.lastIndexOf('.');
		String base = dot >= 0 ? name.substring(0, dot) : name;
		return new File(input.getParentFile(), base + ".xlsx");
	}

	private void alert(String msg) {
		Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
		a.showAndWait();
	}

	private void alert(String msg, String... args) {
		Alert a = new Alert(Alert.AlertType.INFORMATION, String.format(msg, (Object[]) args), ButtonType.OK);
		a.showAndWait();
	}

	private String addSuffix(String filename, String suffix) {
		int dot = filename.lastIndexOf('.');
		if (dot < 0) return filename + suffix;
		return filename.substring(0, dot) + suffix;
	}

	public static void main(String[] args) {
		launch(args);
	}
}


