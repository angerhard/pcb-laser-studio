# PCB Laser Studio

An open-source Java application for generating and previewing G-code from SVG designs, focused on PCB milling/laser workflows.

This README is written in English and provides a comprehensive guide to building, running, and contributing to the project. The repository is a Maven project and the sources live under `src/main/java`.

Table of contents
-----------------
- Project overview
- Key features
- Requirements
- Building and running
- Quick start examples
- Project architecture
- Development and debugging
- Testing
- Troubleshooting
- Contributing
- License

Project overview
----------------

- Language: Java 17
- Build system: Maven (wrapper available)
- Recommended IDE: IntelliJ IDEA
- Main package: `de.andreasgerhard.pcblaserstudio`
- Entry point (main class): `de.andreasgerhard.pcblaserstudio.MainApplication`

Key features
------------

- Parse SVG files (Apache Batik)
- Convert vector shapes and layers into G-code suitable for laser cutters / plotters
- Multi-layer preview and layer management
- Canvas views for rendering SVGs and generated G-code
- Project serialization (save / load project files)

Requirements
------------

- Java Development Kit (JDK) 17 or newer
- Maven 3.6+ (recommended) — you can also use the included Maven Wrapper (`mvnw.cmd` on Windows)
- Recommended: IntelliJ IDEA for editing and running the app

Building
--------

From the command line (recommended):

```powershell
mvnw.cmd -v ; mvnw.cmd clean package
```

or if you have Maven installed globally:

```powershell
mvn -v ; mvn clean package
```

The build artifacts will be placed in the `target/` directory.

Running the application
-----------------------

If the build produces an executable JAR, run it with:

```powershell
java -jar target/pcb-laser-studio-1.0-SNAPSHOT.jar
```

If the project does not produce a fat JAR, start the main class with dependencies on the classpath:

```powershell
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency
java -cp "target/classes;target/dependency/*" de.andreasgerhard.pcblaserstudio.MainApplication
```

Or simply run from your IDE by launching `de.andreasgerhard.pcblaserstudio.MainApplication`.

Quick start example: SVG -> G-code
----------------------------------

1. Prepare an SVG file (example: `examples/board-outline.svg`).
2. Launch the application (see Running the application).
3. Use the UI to open the SVG and inspect layers.
4. Configure laser/tool settings (power, feed, passes) in the Tool/Settings panel.
5. Generate G-code via the "Generate" button and preview it on the G-code canvas.
6. Export the G-code to a file (`File -> Export G-code`).

If you want to script the process from the command line, you can write a small Java wrapper that loads a project, calls the internal G-code generator, and writes the output. The project's `service.GCodeGenerator` class is the logical starting point for automation.

Project architecture
--------------------

The main code is organized under `src/main/java/de/andreasgerhard/pcblaserstudio` and contains the following packages:

- `controller` – UI controllers and event handlers
- `model` – domain objects (Project, Layer, LaserSettings, ProcessingMode, etc.)
- `service` – core services like `SvgParser`, `GCodeGenerator`, and `ProjectSerializer`
- `view` – Swing/Canvas UI components and panels

Main classes of interest
------------------------

- `MainApplication` – application entry point
- `MainWindow` – top-level window and menu handling
- `SvgParser` – parses SVG files into internal model representation
- `GCodeGenerator` – generates G-code from the model + settings
- `ProjectSerializer` – save/load project files

Development & debugging
-----------------------

Recommended workflow in IntelliJ IDEA:

1. Open the project (File → Open) and choose the project root where `pom.xml` is located.
2. Ensure the Project SDK is set to Java 17 (File → Project Structure).
3. Build the project using the Maven tool window or `mvnw.cmd clean package`.
4. Run `MainApplication` from the IDE Run/Debug configurations for quick iterative development.

Use the IDE's debugger to set breakpoints in `SvgParser` and `GCodeGenerator` when diagnosing parsing or generation issues.

Testing
-------

If the repository contains tests, run them with:

```powershell
mvnw.cmd test
```

or

```powershell
mvn test
```

Troubleshooting
---------------

- Missing JavaFX or UI errors: the application uses Swing-based canvases. If you see errors related to graphics environments headless exceptions, ensure you run with a desktop session and the correct JDK.
- XML/SVG parsing errors: ensure input SVGs are valid XML and avoid external references that Batik cannot resolve offline.
- Classpath issues when running with `java -cp`: be careful with the separator on Windows (`;`) and that `target/dependency/*` contains all dependency jars.

Recommended checks
------------------

- Verify JDK version: `java -version`
- Verify Maven (or wrapper): `mvn -v` or `./mvnw -v` (Windows: `mvnw.cmd -v`)

Contributing
------------

Contributions are welcome. Suggested workflow:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Implement changes and add tests
4. Run tests and ensure the project builds
5. Open a pull request describing the change

Please follow these guidelines:

- Keep user-facing changes documented in the README or a changelog
- Write tests for critical logic (parsing, G-code generation)
- Run code style checks if any are configured

License
-------

No license file is included in the repository by default. If you plan to publish this project, add a `LICENSE` file. Common choices are:

- MIT: permissive and concise
- Apache-2.0: permissive with patent grant

Example: to add an MIT license, create a `LICENSE` file containing the MIT text and update the `README` to reference it.

Contact and support
-------------------

If you need help or want to report issues, please open an issue in the repository. For quick questions, add a short description of the problem, the Java and Maven versions you used, and any stack traces.

Appendix: Developer notes
------------------------

- Important packages: `service.SvgParser`, `service.GCodeGenerator`, `model.Layer`, `model.Project`.
- If you add CLI entrypoints or automation scripts, consider adding a `bin/` folder with platform-specific helpers (e.g. `run-windows.bat`, `run-unix.sh`).
- Consider adding example SVG files to `examples/` and documentation images to `docs/` for a better onboarding experience.

If you'd like, I can also:

- Add a `LICENSE` file (e.g. MIT) and reference it here
- Add an `examples/` folder with a minimal SVG and example output G-code
- Add a `docs/` folder with screenshots and a short architecture diagram

---

Generated on 2026-03-23.

