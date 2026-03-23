package de.andreasgerhard.pcblaserstudio.view.panels;

import de.andreasgerhard.pcblaserstudio.service.GCodeGenerator;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Panel showing GCODE generation controls, stats, and export buttons.
 */
public class GCodeGenerationPanel extends JPanel {

    public interface GenerateCallback {
        void onGenerateActive();
        void onGenerateAll();
        void onExportGcode();
        void onExportProject();
    }

    private final JProgressBar progressBar;
    private final JLabel lblLineCount;
    private final JLabel lblEstTime;
    private final JLabel lblFileSize;
    private final JButton btnGenerateActive;
    private final JButton btnGenerateAll;
    private final JButton btnExportGcode;
    private final JButton btnExportProject;

    public GCodeGenerationPanel() {
        setLayout(new MigLayout("fillx, insets 8 8 8 8, gapy 5", "[grow][grow]", ""));
        setBorder(DarkTheme.createSectionBorder("GCODE Generation"));
        setBackground(DarkTheme.BG_DARK);

        // Generate buttons
        btnGenerateActive = DarkTheme.createAccentButton("Generate Active", "Generate GCODE for the current layer");
        btnGenerateAll    = DarkTheme.createButton("Generate All", "Generate GCODE for all enabled layers");
        add(btnGenerateActive, "growx");
        add(btnGenerateAll, "growx, wrap 8");

        // Progress
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setValue(0);
        progressBar.setFont(DarkTheme.FONT_SMALL);
        add(progressBar, "span 2, growx, wrap 6");

        // Stats panel
        JPanel statsPanel = new JPanel(new MigLayout("fillx, insets 6 6 6 6, gapy 2", "[][grow]", ""));
        statsPanel.setBackground(DarkTheme.BG_DARKEST);
        statsPanel.setBorder(BorderFactory.createLineBorder(DarkTheme.BG_MEDIUM, 1));

        statsPanel.add(DarkTheme.createDimLabel("Lines:"), "");
        lblLineCount = DarkTheme.createLabel("-");
        statsPanel.add(lblLineCount, "wrap");

        statsPanel.add(DarkTheme.createDimLabel("Est. time:"), "");
        lblEstTime = DarkTheme.createLabel("-");
        statsPanel.add(lblEstTime, "wrap");

        statsPanel.add(DarkTheme.createDimLabel("File size:"), "");
        lblFileSize = DarkTheme.createLabel("-");
        statsPanel.add(lblFileSize, "wrap");

        add(statsPanel, "span 2, growx, wrap 8");

        // Preview/Show layer visibility controls
        JLabel lblVisTitle = DarkTheme.createDimLabel("Overlay Visibility:");
        add(lblVisTitle, "span 2, wrap 2");

        JCheckBox chkShowSvg      = createCheck("Show SVG",      true, "Show/hide the SVG background");
        JCheckBox chkShowOutlines = createCheck("Show Outlines",  true, "Show/hide red outline paths");
        JCheckBox chkShowFill     = createCheck("Show Fill",      true, "Show/hide blue fill paths");

        add(chkShowSvg,      "");
        add(chkShowOutlines, "wrap");
        add(new JLabel(""), "");
        add(chkShowFill, "wrap 8");

        // Zoom controls
        JLabel lblZoom = DarkTheme.createDimLabel("Zoom:");
        add(lblZoom, "span 2, wrap 2");

        JButton btnFit  = DarkTheme.createButton("Fit",    "Fit SVG to window");
        JButton btnZIn  = DarkTheme.createButton("+",      "Zoom in");
        JButton btnZOut = DarkTheme.createButton("-",      "Zoom out");
        JButton btn100  = DarkTheme.createButton("100%",   "Actual size");

        add(btnFit, "growx");
        JPanel zoomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        zoomRow.setBackground(DarkTheme.BG_DARK);
        zoomRow.add(btnZOut);
        zoomRow.add(btn100);
        zoomRow.add(btnZIn);
        add(zoomRow, "wrap 8");

        // Export buttons
        btnExportGcode   = DarkTheme.createButton("Export GCODE",   "Save GCODE to file");
        btnExportProject = DarkTheme.createButton("Export Project",  "Save project as .pcblaser");
        add(btnExportGcode,   "growx");
        add(btnExportProject, "growx, wrap");

        // Expose zoom buttons via action commands (controller wires them via name lookup)
        btnFit.setActionCommand("zoom.fit");
        btnZIn.setActionCommand("zoom.in");
        btnZOut.setActionCommand("zoom.out");
        btn100.setActionCommand("zoom.100");
        chkShowSvg.setActionCommand("vis.svg");
        chkShowOutlines.setActionCommand("vis.outlines");
        chkShowFill.setActionCommand("vis.fill");

        // Tag for lookup
        btnFit.setName("zoom.fit");
        btnZIn.setName("zoom.in");
        btnZOut.setName("zoom.out");
        btn100.setName("zoom.100");
        chkShowSvg.setName("vis.svg");
        chkShowOutlines.setName("vis.outlines");
        chkShowFill.setName("vis.fill");
    }

    // =================== Wiring from controller ===================

    public void setCallback(GenerateCallback cb) {
        btnGenerateActive.addActionListener(e -> cb.onGenerateActive());
        btnGenerateAll.addActionListener(e -> cb.onGenerateAll());
        btnExportGcode.addActionListener(e -> cb.onExportGcode());
        btnExportProject.addActionListener(e -> cb.onExportProject());
    }

    public void setProgress(int value, String text) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(value);
            progressBar.setString(text);
        });
    }

    public void updateStats(GCodeGenerator.GCodeResult result) {
        SwingUtilities.invokeLater(() -> {
            if (result == null) {
                lblLineCount.setText("-");
                lblEstTime.setText("-");
                lblFileSize.setText("-");
            } else {
                lblLineCount.setText(String.format("%,d", result.lineCount()));
                lblEstTime.setText(GCodeGenerator.formatTime(result.estimatedTimeSeconds()));
                lblFileSize.setText(String.format("%.1f KB", result.fileSizeKb()));
            }
        });
    }

    public void setGenerating(boolean busy) {
        btnGenerateActive.setEnabled(!busy);
        btnGenerateAll.setEnabled(!busy);
        if (!busy) {
            progressBar.setValue(100);
            progressBar.setString("Done");
        }
    }

    private static JCheckBox createCheck(String label, boolean selected, String tip) {
        JCheckBox cb = new JCheckBox(label, selected);
        cb.setFont(DarkTheme.FONT_SMALL);
        cb.setForeground(DarkTheme.TEXT_DIM);
        cb.setBackground(DarkTheme.BG_DARK);
        cb.setToolTipText(tip);
        return cb;
    }
}

