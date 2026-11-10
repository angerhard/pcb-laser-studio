package de.andreasgerhard.pcblaserstudio.controller;

import de.andreasgerhard.pcblaserstudio.model.BoardSettings;
import de.andreasgerhard.pcblaserstudio.model.ConnectionSettings;
import de.andreasgerhard.pcblaserstudio.model.Layer;
import de.andreasgerhard.pcblaserstudio.model.Project;
import de.andreasgerhard.pcblaserstudio.service.BoardAlignmentService;
import de.andreasgerhard.pcblaserstudio.service.GCodeGenerator;
import de.andreasgerhard.pcblaserstudio.service.PreferencesService;
import de.andreasgerhard.pcblaserstudio.service.ProjectSerializer;
import de.andreasgerhard.pcblaserstudio.service.SvgParser;
import de.andreasgerhard.pcblaserstudio.view.LayerTab;
import de.andreasgerhard.pcblaserstudio.view.MainWindow;
import de.andreasgerhard.pcblaserstudio.view.dialogs.BoardSettingsDialog;
import de.andreasgerhard.pcblaserstudio.view.dialogs.LaserControlDialog;
import de.andreasgerhard.pcblaserstudio.view.panels.GCodeGenerationPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main application controller – wires the view to services.
 */
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    private Project project;
    private MainWindow window;

    private final PreferencesService preferencesService = new PreferencesService();
    private ConnectionSettings connectionSettings;
    private LaserControlDialog laserDialog;
    private BoardSettingsDialog boardSettingsDialog;

    // Per-tab parse results cache
    private final Map<String, SvgParser.SvgParseResult> parseCache   = new HashMap<>();
    private final Map<String, GCodeGenerator.GCodeResult> gcodeCache = new HashMap<>();

    public void initialize() {
        connectionSettings = preferencesService.load();
        project = new Project();
        project.clearDirty(); // default layer setup shouldn't count as unsaved changes
        window  = new MainWindow();

        window.initTabs(project);
        wireMenus();
        wireTabCallbacks();
        wirePanelCallbacks();

        window.updateTitle(project);
        window.setStatus("Ready  –  New project created");
        window.setVisible(true);

        // Select first tab
        SwingUtilities.invokeLater(() -> window.setStatus(
                "Drop an SVG file onto the canvas or use File → Open SVG"));
    }

    // =================== Wiring ===================

    private void wireMenus() {
        window.menuNew.addActionListener(e -> onNewProject());
        window.menuOpen.addActionListener(e -> onOpenProject());
        window.menuSave.addActionListener(e -> onSaveProject(false));
        window.menuSaveAs.addActionListener(e -> onSaveProject(true));
        window.menuOpenSvg.addActionListener(e -> onOpenSvg());
        window.menuClearSvg.addActionListener(e -> onClearSvg());
        window.menuExit.addActionListener(e -> onExit());
        window.menuGenActive.addActionListener(e -> onGenerateActive());
        window.menuGenAll.addActionListener(e -> onGenerateAll());
        window.menuExportGcode.addActionListener(e -> onExportGcode());
        window.menuExportProject.addActionListener(e -> onSaveProject(true));
        window.menuSendToLaser.addActionListener(e -> onSendToLaser());
        window.menuBoardSettings.addActionListener(e -> onBoardSettings());
        window.menuAbout.addActionListener(e -> onAbout());
    }

    private void wireTabCallbacks() {
        window.setOnTabChanged(idx -> {
            LayerTab tab = window.getActiveTab();
            if (tab != null) {
                tab.toolPanel.setLayer(tab.getLayer());
                GCodeGenerator.GCodeResult cached = gcodeCache.get(tab.getLayer().getId());
                tab.updateGcodeResult(cached);
                wirePanelCallbacks(); // re-wire for new active panel
            }
        });

        window.setOnCloseRequested(v -> onExit());
    }

    private void wirePanelCallbacks() {
        LayerTab active = window.getActiveTab();
        if (active == null) return;

        // Layer settings changes → refresh tab header (side changes affect board alignment)
        active.toolPanel.layerSettingsPanel.setOnChange(layer -> {
            window.refreshTabHeaders();
            window.updateTitle(project);
            active.svgCanvas.refreshAlignment();
        });

        // Laser settings changes → clear cached gcode, mark project dirty
        active.toolPanel.laserParametersPanel.setOnChange(settings -> {
            active.getLayer().setDirty(true);
            gcodeCache.remove(active.getLayer().getId());
            active.gcodeCanvas.clearGcodeResult();
            active.toolPanel.updateGcodeStats(null);
            window.updateTitle(project);
            window.refreshTabHeaders();
        });

        // GCode pre/post changes → clear cached gcode
        active.toolPanel.gcodePrePostPanel.setOnChange(layer -> {
            gcodeCache.remove(active.getLayer().getId());
            active.gcodeCanvas.clearGcodeResult();
            active.toolPanel.updateGcodeStats(null);
            window.updateTitle(project);
            window.refreshTabHeaders();
        });

        // GCode pre/post panel → apply to all layers
        active.toolPanel.gcodePrePostPanel.setOnApplyToAll(v -> {
            String prefix = active.toolPanel.gcodePrePostPanel.getPrefix();
            String suffix = active.toolPanel.gcodePrePostPanel.getSuffix();
            for (Layer layer : project.getLayers()) {
                layer.setGcodePrefix(prefix);
                layer.setGcodeSuffix(suffix);
                gcodeCache.remove(layer.getId());
            }
            window.setStatus("GCODE prefix/suffix applied to all layers");
        });

        // SVG drag-drop
        active.svgCanvas.setOnFileDropped(file -> loadSvgFile(file, active));
        active.svgCanvas.setOnStatusMessage(msg -> window.setStatusRight(msg));

        // GCode panel buttons
        active.toolPanel.gcodeGenerationPanel.setCallback(new GCodeGenerationPanel.GenerateCallback() {
            @Override public void onGenerateActive()  { MainController.this.onGenerateActive(); }
            @Override public void onGenerateAll()     { MainController.this.onGenerateAll(); }
            @Override public void onExportGcode()     { MainController.this.onExportGcode(); }
            @Override public void onExportProject()   { onSaveProject(true); }
            @Override public void onSendToLaser()     { MainController.this.onSendToLaser(); }
        });

        // Zoom controls: find named buttons inside the panel
        findAndWireButtons(active);
    }

    private void findAndWireButtons(LayerTab tab) {
        // Walk the GCodeGenerationPanel component tree for named buttons/checkboxes.
        // wireOnce() prevents duplicate listeners when a tab is re-selected.
        walkComponents(tab.toolPanel.gcodeGenerationPanel, c -> {
            String name = c.getName();
            if (name == null) return;
            switch (name) {
                case "zoom.fit"      -> wireOnce((AbstractButton)c, e -> tab.dualCanvas.fitToWindow());
                case "zoom.in"       -> wireOnce((AbstractButton)c, e -> tab.dualCanvas.zoomIn());
                case "zoom.out"      -> wireOnce((AbstractButton)c, e -> tab.dualCanvas.zoomOut());
                case "zoom.100"      -> wireOnce((AbstractButton)c, e -> tab.dualCanvas.zoom100());
                case "vis.svg"       -> wireOnce((AbstractButton)c, e -> tab.svgCanvas.setShowSvg(((JCheckBox)c).isSelected()));
                case "vis.outlines"  -> wireOnce((AbstractButton)c, e -> tab.gcodeCanvas.setShowOutlines(((JCheckBox)c).isSelected()));
                case "vis.fill"      -> wireOnce((AbstractButton)c, e -> tab.gcodeCanvas.setShowFill(((JCheckBox)c).isSelected()));
            }
        });
    }

    private void wireOnce(AbstractButton button, java.awt.event.ActionListener listener) {
        if (button.getActionListeners().length == 0) button.addActionListener(listener);
    }

    private void walkComponents(Container container, java.util.function.Consumer<Component> consumer) {
        for (Component c : container.getComponents()) {
            consumer.accept(c);
            if (c instanceof Container child) walkComponents(child, consumer);
        }
    }

    // =================== SVG Loading ===================

    private void onOpenSvg() {
        JFileChooser fc = createFileChooser("SVG Files", "svg");
        if (fc.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
            LayerTab tab = window.getActiveTab();
            if (tab != null) loadSvgFile(fc.getSelectedFile(), tab);
        }
    }

    private void loadSvgFile(File file, LayerTab tab) {
        window.setStatus("Loading SVG: " + file.getName() + "  …");
        new SwingWorker<SvgParser.SvgParseResult, Void>() {
            @Override
            protected SvgParser.SvgParseResult doInBackground() throws Exception {
                return SvgParser.parseFile(file);
            }

            @Override
            protected void done() {
                try {
                    SvgParser.SvgParseResult result = get();
                    Layer layer = tab.getLayer();

                    // Encode SVG as base64 and store in model
                    byte[] bytes = readFileBytes(file);
                    layer.setSvgData(Base64.getEncoder().encodeToString(bytes));
                    layer.setSvgFilePath(file.getAbsolutePath());

                    parseCache.put(layer.getId(), result);
                    gcodeCache.remove(layer.getId());

                    tab.svgCanvas.setSvgParseResult(result, 2048);
                    tab.gcodeCanvas.clearGcodeResult();
                    tab.toolPanel.updateGcodeStats(null);

                    SvgParser.DocumentBounds bounds = SvgParser.getDocumentBounds(result);
                    window.setStatus(String.format("SVG loaded: %s  (%.2f × %.2f mm)",
                            file.getName(), bounds.widthMm(), bounds.heightMm()));
                    window.updateTitle(project);
                    window.refreshTabHeaders();
                    if (boardSettingsDialog != null && boardSettingsDialog.isDisplayable()) {
                        boardSettingsDialog.refreshWarnings();
                    }
                } catch (Exception ex) {
                    log.error("SVG load error", ex);
                    JOptionPane.showMessageDialog(window,
                            "Could not load SVG:\n" + ex.getMessage(),
                            "SVG Error", JOptionPane.ERROR_MESSAGE);
                    window.setStatus("SVG load failed: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onClearSvg() {
        LayerTab tab = window.getActiveTab();
        if (tab == null) return;
        int r = JOptionPane.showConfirmDialog(window,
                "Clear SVG data from layer \"" + tab.getLayer().getName() + "\"?",
                "Clear SVG", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            Layer layer = tab.getLayer();
            layer.setSvgData(null);
            layer.setSvgFilePath(null);
            parseCache.remove(layer.getId());
            gcodeCache.remove(layer.getId());
            tab.gcodeCanvas.clearGcodeResult();
            // Force canvas repaint with no image
            tab.svgCanvas.setSvgParseResult(null, 0); // will be handled gracefully
            tab.gcodeCanvas.clearGcodeResult();
            window.setStatus("SVG cleared from layer: " + layer.getName());
        }
    }

    // =================== GCODE Generation ===================

    private void onGenerateActive() {
        LayerTab tab = window.getActiveTab();
        if (tab == null) return;
        generateForTab(tab);
    }

    private void onGenerateAll() {
        for (LayerTab tab : window.getAllTabs()) {
            if (tab.getLayer().isEnabled()) {
                generateForTab(tab);
            }
        }
    }

    private void generateForTab(LayerTab tab) {
        Layer layer = tab.getLayer();
        SvgParser.SvgParseResult parseResult = parseCache.get(layer.getId());

        if (parseResult == null) {
            window.setStatus("No SVG loaded for layer: " + layer.getName());
            JOptionPane.showMessageDialog(window,
                    "Please load an SVG file first.",
                    "No SVG", JOptionPane.WARNING_MESSAGE);
            return;
        }

        tab.toolPanel.gcodeGenerationPanel.setProgress(0, "Generating…");
        tab.toolPanel.gcodeGenerationPanel.setGenerating(true);
        window.setStatus("Generating GCODE for: " + layer.getName() + "  …");

        new SwingWorker<GCodeGenerator.GCodeResult, Integer>() {
            @Override
            protected GCodeGenerator.GCodeResult doInBackground() {
                return GCodeGenerator.generate(layer, parseResult, project.getBoardSettings(), 4096, 4096, this::publish);
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                int progress = chunks.get(chunks.size() - 1);
                tab.toolPanel.gcodeGenerationPanel.setProgress(progress, "Generating… " + progress + "%");
            }

            @Override
            protected void done() {
                try {
                    GCodeGenerator.GCodeResult result = get();
                    gcodeCache.put(layer.getId(), result);
                    tab.updateGcodeResult(result);
                    tab.toolPanel.gcodeGenerationPanel.setGenerating(false);
                    window.setStatus(String.format(
                            "GCODE generated: %,d lines, %.1f KB, ~%s  [%s]",
                            result.lineCount(), result.fileSizeKb(),
                            GCodeGenerator.formatTime(result.estimatedTimeSeconds()),
                            layer.getName()));
                } catch (Exception ex) {
                    log.error("GCODE generation error", ex);
                    tab.toolPanel.gcodeGenerationPanel.setGenerating(false);
                    tab.toolPanel.gcodeGenerationPanel.setProgress(0, "Error");
                    JOptionPane.showMessageDialog(window,
                            "GCODE generation failed:\n" + ex.getMessage(),
                            "Generation Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // =================== Export ===================

    private void onExportGcode() {
        LayerTab tab = window.getActiveTab();
        if (tab == null) return;
        GCodeGenerator.GCodeResult result = gcodeCache.get(tab.getLayer().getId());
        if (result == null) {
            JOptionPane.showMessageDialog(window, "Generate GCODE first.", "No GCODE", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export GCODE");
        fc.addChoosableFileFilter(new FileNameExtensionFilter("GCODE Files", "gcode", "nc", "ngc"));
        fc.setSelectedFile(new File(tab.getLayer().getName().replaceAll("[^a-zA-Z0-9]", "_") + ".gcode"));

        if (fc.showSaveDialog(window) == JFileChooser.APPROVE_OPTION) {
            File out = fc.getSelectedFile();
            if (!out.getName().contains(".")) out = new File(out.getAbsolutePath() + ".gcode");
            try (PrintWriter pw = new PrintWriter(new FileWriter(out, StandardCharsets.UTF_8))) {
                pw.print(result.gcode());
                window.setStatus("GCODE exported to: " + out.getAbsolutePath());
            } catch (IOException ex) {
                log.error("Export error", ex);
                JOptionPane.showMessageDialog(window, "Export failed: " + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =================== Send to laser ===================

    private void onSendToLaser() {
        LayerTab tab = window.getActiveTab();
        if (tab == null) return;
        GCodeGenerator.GCodeResult result = gcodeCache.get(tab.getLayer().getId());
        if (result == null) {
            JOptionPane.showMessageDialog(window, "Generate GCODE first.", "No GCODE",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (connectionSettings == null) connectionSettings = preferencesService.load();

        if (laserDialog == null || !laserDialog.isDisplayable()) {
            laserDialog = new LaserControlDialog(window, connectionSettings, preferencesService);
        }
        laserDialog.setJob(tab.getLayer().getName(), result.gcode(), result.estimatedTimeSeconds());
        laserDialog.setVisible(true);
        laserDialog.toFront();
        window.setStatus("Send to laser: " + tab.getLayer().getName());
    }

    // =================== Board settings ===================

    private void onBoardSettings() {
        if (boardSettingsDialog == null || !boardSettingsDialog.isDisplayable()) {
            boardSettingsDialog = new BoardSettingsDialog(window, this::computeSizeMismatches);
            boardSettingsDialog.setOnChange(this::onBoardSettingsChanged);
        }
        boardSettingsDialog.setBoardSettings(project.getBoardSettings());
        boardSettingsDialog.setVisible(true);
        boardSettingsDialog.toFront();
    }

    private List<String> computeSizeMismatches() {
        return BoardAlignmentService.findSizeMismatches(project, parseCache);
    }

    private void onBoardSettingsChanged(BoardSettings settings) {
        // Board settings affect the auto-alignment of every layer - invalidate all cached GCODE.
        gcodeCache.clear();
        for (LayerTab tab : window.getAllTabs()) {
            tab.gcodeCanvas.clearGcodeResult();
            tab.toolPanel.updateGcodeStats(null);
            tab.getLayer().setDirty(true);
            tab.svgCanvas.refreshAlignment();
        }
        window.updateTitle(project);
        window.refreshTabHeaders();
        window.setStatus("Board settings updated — regenerate GCODE to apply the new alignment");
    }

    // =================== Project save/load ===================

    private void onNewProject() {
        if (!confirmDiscard()) return;
        project = new Project();
        project.clearDirty(); // default layer setup shouldn't count as unsaved changes
        parseCache.clear();
        gcodeCache.clear();
        window.initTabs(project);
        wirePanelCallbacks();
        window.updateTitle(project);
        window.setStatus("New project created");
        if (boardSettingsDialog != null && boardSettingsDialog.isDisplayable()) {
            boardSettingsDialog.setBoardSettings(project.getBoardSettings());
        }
    }

    private void onOpenProject() {
        if (!confirmDiscard()) return;
        JFileChooser fc = createFileChooser("PCB Laser Projects", "pcblaser");
        if (fc.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
            try {
                project = ProjectSerializer.load(fc.getSelectedFile());
                parseCache.clear();
                gcodeCache.clear();

                // Re-parse embedded SVG data
                for (Layer layer : project.getLayers()) {
                    if (layer.hasSvg()) {
                        try {
                            SvgParser.SvgParseResult result = SvgParser.parseBase64(layer.getSvgData());
                            parseCache.put(layer.getId(), result);
                        } catch (Exception ex) {
                            log.warn("Could not re-parse SVG for layer {}: {}", layer.getId(), ex.getMessage());
                        }
                    }
                }

                window.initTabs(project);
                wirePanelCallbacks();

                // Render SVGs in tabs
                for (int i = 0; i < project.getLayers().size(); i++) {
                    Layer layer = project.getLayers().get(i);
                    SvgParser.SvgParseResult result = parseCache.get(layer.getId());
                    if (result != null) {
                        window.getAllTabs().get(i).svgCanvas.setSvgParseResult(result, 2048);
                    }
                }

                window.updateTitle(project);
                window.setStatus("Project loaded: " + fc.getSelectedFile().getName());
                if (boardSettingsDialog != null && boardSettingsDialog.isDisplayable()) {
                    boardSettingsDialog.setBoardSettings(project.getBoardSettings());
                }
            } catch (Exception ex) {
                log.error("Project load error", ex);
                JOptionPane.showMessageDialog(window,
                        "Could not load project:\n" + ex.getMessage(),
                        "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** @return {@code false} if the save was cancelled or failed (caller should abort). */
    private boolean onSaveProject(boolean saveAs) {
        File file = null;
        if (!saveAs && project.getFilePath() != null) {
            file = new File(project.getFilePath());
        }
        if (file == null) {
            JFileChooser fc = createFileChooser("PCB Laser Projects", "pcblaser");
            fc.setSelectedFile(new File(
                    project.getMetadata().getName().replaceAll("[^a-zA-Z0-9]", "_") + ".pcblaser"));
            if (fc.showSaveDialog(window) != JFileChooser.APPROVE_OPTION) return false;
            file = fc.getSelectedFile();
            if (!file.getName().endsWith(".pcblaser")) {
                file = new File(file.getAbsolutePath() + ".pcblaser");
            }
        }
        try {
            ProjectSerializer.save(project, file);
            window.updateTitle(project);
            window.setStatus("Project saved: " + file.getAbsolutePath());
            return true;
        } catch (IOException ex) {
            log.error("Save error", ex);
            JOptionPane.showMessageDialog(window, "Save failed: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void onExit() {
        if (project.isDirty()) {
            int r = JOptionPane.showConfirmDialog(window,
                    "There are unsaved changes. Save before exit?",
                    "Exit", JOptionPane.YES_NO_CANCEL_OPTION);
            if (r == JOptionPane.CANCEL_OPTION) return;
            // If the user chose to save but cancels the file dialog or the save
            // fails, abort the exit so no changes are silently lost.
            if (r == JOptionPane.YES_OPTION && !onSaveProject(false)) return;
        }
        System.exit(0);
    }

    private void onAbout() {
        JOptionPane.showMessageDialog(window,
                "<html><b>PCB Laser Studio</b><br>" +
                "Version 1.0<br><br>" +
                "A Marlin-compatible GCODE generator<br>" +
                "for PCB laser engraving.<br><br>" +
                "Built with Apache Batik · JTS · MigLayout · Jackson</html>",
                "About PCB Laser Studio",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // =================== Helpers ===================

    private boolean confirmDiscard() {
        if (!project.isDirty()) return true;
        int r = JOptionPane.showConfirmDialog(window,
                "Discard unsaved changes?",
                "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return r == JOptionPane.YES_OPTION;
    }

    private static JFileChooser createFileChooser(String desc, String ext) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter(desc, ext));
        return fc;
    }

    private static byte[] readFileBytes(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            return in.readAllBytes();
        }
    }
}

