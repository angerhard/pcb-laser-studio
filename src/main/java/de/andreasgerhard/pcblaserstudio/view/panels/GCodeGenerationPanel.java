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

    private GenerateCallback callback;

    public GCodeGenerationPanel() {
        // Single growing column; each logical row uses "split" so it stays compact.
        setLayout(new MigLayout("fillx, insets 6 8 6 8, gapy 4, gapx 4", "[grow]", ""));
        setBorder(DarkTheme.createSectionBorder("GCODE Generation"));
        setBackground(DarkTheme.BG_DARK);

        // Generate buttons
        btnGenerateActive = DarkTheme.createAccentButton("Generate Active", "Generate GCODE for the current layer");
        btnGenerateAll    = DarkTheme.createButton("Generate All", "Generate GCODE for all enabled layers");
        add(btnGenerateActive, "split 2, w 46%, h 22!, wmax 190");
        add(btnGenerateAll, "w 46%, h 22!, wmax 190, wrap");

        // Progress
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setValue(0);
        progressBar.setFont(DarkTheme.FONT_SMALL);
        add(progressBar, "growx, h 16!, wrap");

        // Stats panel
        JPanel statsPanel = new JPanel(new MigLayout("fillx, insets 3 8 3 8, gapy 1, gapx 8", "[grow][grow]", ""));
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

        add(statsPanel, "growx, wrap");

        // Preview/Show layer visibility controls
        JCheckBox chkShowSvg      = createCheck("SVG",      true, "Show/hide the SVG background");
        JCheckBox chkShowOutlines = createCheck("Outlines", true, "Show/hide red outline paths");
        JCheckBox chkShowFill     = createCheck("Fill",     true, "Show/hide blue fill paths");

        add(DarkTheme.createDimLabel("Show:"), "split 4, gapright 6");
        add(chkShowSvg, "");
        add(chkShowOutlines, "");
        add(chkShowFill, "wrap");

        // Zoom controls
        JButton btnFit  = DarkTheme.createButton("Fit",    "Fit SVG to window");
        JButton btnZIn  = DarkTheme.createButton("+",      "Zoom in");
        JButton btnZOut = DarkTheme.createButton("-",      "Zoom out");
        JButton btn100  = DarkTheme.createButton("100%",   "Actual size");

        add(btnFit, "split 4, growx, h 22!, wmax 140");
        add(btnZOut, "w 28!, h 22!");
        add(btn100, "w 50!, h 22!");
        add(btnZIn, "w 28!, h 22!, wrap");

        // Export buttons
        btnExportGcode   = DarkTheme.createButton("Export GCODE",   "Save GCODE to file");
        btnExportProject = DarkTheme.createButton("Export Project",  "Save project as .pcblaser");
        add(btnExportGcode, "split 2, w 46%, h 22!, wmax 190");
        add(btnExportProject, "w 46%, h 22!, wmax 190, wrap");

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

        // Wire generate/export buttons once; they delegate to the current callback.
        btnGenerateActive.addActionListener(e -> { if (callback != null) callback.onGenerateActive(); });
        btnGenerateAll.addActionListener(e -> { if (callback != null) callback.onGenerateAll(); });
        btnExportGcode.addActionListener(e -> { if (callback != null) callback.onExportGcode(); });
        btnExportProject.addActionListener(e -> { if (callback != null) callback.onExportProject(); });
    }

    // =================== Wiring from controller ===================

    public void setCallback(GenerateCallback cb) { this.callback = cb; }

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

