Supervisor Assignment — Desktop App

Overview
This Java desktop application reads an Excel workbook with Subjects and Supervisors sheets and produces Assignments and Supervisor Totals sheets, enforcing availability, no-overlap, and fairness rules. It also includes a template generator to create a validated input workbook.

Features
- Import Excel (.xlsx), validate inputs
- Greedy assignment with fairness balancing and overlap checks
- Outputs Assignments and Supervisor Totals, with logs and conditional formatting
- Template generator with dropdown lists and config sheet
- JavaFX desktop UI (Browse, Run Assignment, Generate Template)

Build Requirements
- JDK 17+
- Maven 3.8+

Build & Run
- Build: mvn package
- Run (CLI): java -jar target/supervisor-assignment-1.0.0-jar-with-dependencies.jar
- Run (JavaFX plugin): mvn javafx:run

Template
- Use the app’s Generate Template button to create a starter workbook.
- Fill Subjects and Supervisors sheets per column headers. Days and Times have dropdowns.

Packaging (Native Installers)
This project is configured for jpackage. Ensure JDK includes jpackage.
- Create fat jar: mvn -DskipTests package
- Generate app-image: mvn jpackage:jpackage
- Windows .exe/.msi: Run on Windows host with proper signing if needed
- macOS .dmg: Run on macOS host (optional signing/notarization)

Endpoint (Optional)
Desktop-first; no network endpoints are required. A CLI mode can be added later.

Troubleshooting
- Ensure Excel file has required sheets and columns
- Check Config sheet values if fairness results are unexpected
- Verify JavaFX runtime is available when running outside the packaged app


