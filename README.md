Supervisor Assignment — Desktop App

Overview
This desktop app reads an Excel workbook (Arabic/English) with Subjects and Supervisors, then produces Assignments, Supervisor Totals, and Supervisor Schedules with strict rules (no overlaps, max 2 consecutive days, backups per session, fairness, Friday off). The UI lets you browse input/output files and run assignments; a ready example file can be downloaded from the app.

What you get
- Import Excel (.xlsx) and validate
- Assignments with 2 primary + 2 backup per session (distinct) when required
- Supervisor totals by role with target/actual hours and load%
- Individual supervisor schedules (primary + backup) sorted by date/time
- JavaFX UI: Run Assignment, Download Example (input_large_A.xlsx), Export PDF

Run on Windows (no tools preinstalled)
These steps work on a fresh Windows 10/11 machine.

1) Install prerequisites
- Install JDK 17 (Temurin/Adoptium recommended): https://adoptium.net
  - During install, check “Set JAVA_HOME” and “Add to PATH”.
- Install Maven (optional if you only run the fat JAR): https://maven.apache.org/download.cgi
  - Unzip, then add the bin folder to PATH (or use Chocolatey: choco install maven).
- Install Git (to clone the repo): https://git-scm.com

Verify installation in a new Command Prompt (cmd):
```
java -version
mvn -version
git --version
```

2) Get the project
```
git clone https://github.com/AmerOsama10/SupervisorAssignment.git
cd SupervisorAssignment/supervisor-assignment
```

3) Quickest way to run the UI (Maven)
Maven will download JavaFX and all dependencies automatically.
```
mvn -q clean package
mvn -q javafx:run
```
The UI window “Supervisor Assignment” should open. Use “تحميل المثال” to save a ready example `input_large_A.xlsx`, then choose it as input and run.

4) Run the UI without Maven (fat JAR)
Build the fat JAR once, then run using Java only:
```
mvn -q -DskipTests clean package
java -cp target/supervisor-assignment-1.0.0-jar-with-dependencies.jar com.supervisor.assignment.ui.Launcher
```
Note: The fat JAR includes dependencies. If Windows reports a security prompt, allow Java to run.

5) Create a native Windows app (optional)
Requires JDK 17 with jpackage (Oracle/Temurin JDKs include it). On Windows host:
```
mvn -q -DskipTests clean package jpackage:jpackage
```
The installer/app image is created under `target/installer/`. Run the generated `.exe` to install and launch the app like any other Windows program.

Using the app
- Download example: Click “تحميل المثال” to save `input_large_A.xlsx` with realistic data and correct headers.
- Select input/output files, then click Run Assignment.
- Output will include sheets: التعيينات (Assignments), إجمالي المراقبين (Supervisor Totals), جداول المراقبين (Supervisor Schedules).

CLI mode (advanced)
The fat JAR’s manifest points to CLI. If you prefer CLI only:
```
java -jar target/supervisor-assignment-1.0.0-jar-with-dependencies.jar --help
```
If you need the UI from the fat JAR, use the class path form shown in step 4.

Troubleshooting
- Java not found: Reboot after JDK install, or ensure JAVA_HOME and PATH are set.
- Window does not open: Use `mvn javafx:run` from step 3 to ensure JavaFX is resolved correctly.
- Excel errors: Ensure sheet names/headers match the example. Friday must be excluded in inputs.
- Fonts in Arabic PDF: Ensure system has Arabic fonts; OpenPDF will embed if configured.

