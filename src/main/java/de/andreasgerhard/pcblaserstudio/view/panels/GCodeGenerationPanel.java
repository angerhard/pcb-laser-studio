package de.andreasgerhard.pcblaserstudio.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import de.andreasgerhard.pcblaserstudio.service.GCodeGenerator;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import de.andreasgerhard.pcblaserstudio.view.theme.Icons;
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
        void onSendToLaser();
    }

    private final JProgressBar progressBar;
    private final JLabel lblLineCount;
    private final JLabel lblEstTime;
    private final JLabel lblFileSize;
    private final JButton btnGenerateActive;
    private final JButton btnGenerateAll;
    private final JButton btnExportGcode;
    private final JButton btnExportProject;
    private final JButton btnSendToLaser;

    private GenerateCallback callback;

    public GCodeGenerationPanel() {
        // Single growing column; each logical row uses "split" so it stays compact.
        setLayout(new MigLayout("fillx, insets 6 8 6 8, gapy 4, gapx 4", "[grow]", ""));
        setBorder(DarkTheme.createSectionBorder("GCODE Generation"));
        setBackground(DarkTheme.BG_DARK);

        // Generate buttons
        btnGenerateActive = DarkTheme.createAccentButton("Generate Active", "Generate GCODE for the current layer");
        btnGenerateActive.setIcon(Icons.of(Icons.PLAY, 13, Color.WHITE));
        btnGenerateAll    = DarkTheme.createButton("Generate All", "Generate GCODE for all enabled layers");
        btnGenerateAll.setIcon(Icons.of(Icons.LAYERS, 13));
        add(btnGenerateActive, "split 2, w 46%, h 24!, wmax 190");
        add(btnGenerateAll, "w 46%, h 24!, wmax 190, wrap");

        // Progress
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setValue(0);
        progressBar.setFont(DarkTheme.FONT_SMALL);
        add(progressBar, "growx, h 14!, wrap");

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

        // Preview/Show layer visibility controls - compact toggles, tinted to match path colors
        JToggleButton chkShowSvg      = createVisToggle("SVG",  true, DarkTheme.TEXT_DIM,     "Show/hide the SVG background");
        JToggleButton chkShowOutlines = createVisToggle("OUT",  true, DarkTheme.COLOR_OUTLINE,"Show/hide contour (outline) paths");
        JToggleButton chkShowFill     = createVisToggle("FILL", true, DarkTheme.COLOR_FILL,   "Show/hide raster fill paths");

        add(DarkTheme.createDimLabel("Show:"), "split 4, gapright 4");
        add(chkShowSvg, "");
        add(chkShowOutlines, "");
        add(chkShowFill, "wrap");

        // Zoom controls - compact icon buttons
        JButton btnFit  = DarkTheme.createIconButton(Icons.of(Icons.FIT, 14), "Fit SVG to window");
        JButton btnZOut = DarkTheme.createIconButton(Icons.of(Icons.ZOOM_OUT, 14), "Zoom out");
        JButton btn100  = DarkTheme.createButton("1:1", "Actual size");
        JButton btnZIn  = DarkTheme.createIconButton(Icons.of(Icons.ZOOM_IN, 14), "Zoom in");

        add(btnFit, "split 4");
        add(btnZOut, "w 26!, h 22!");
        add(btn100, "w 36!, h 22!");
        add(btnZIn, "w 26!, h 22!, wrap");

        // Export buttons
        btnExportGcode   = DarkTheme.createButton("Export GCODE",   "Save GCODE to file");
        btnExportGcode.setIcon(Icons.of(Icons.DOWNLOAD, 13));
        btnExportProject = DarkTheme.createButton("Export Project",  "Save project as .pcblaser");
        btnExportProject.setIcon(Icons.of(Icons.SAVE, 13));
        add(btnExportGcode, "split 2, w 46%, h 22!, wmax 190");
        add(btnExportProject, "w 46%, h 22!, wmax 190, wrap");

        // Send to laser cutter
        btnSendToLaser = DarkTheme.createAccentButton("Send to Laser…",
                "Connect to a laser cutter and stream this layer's GCODE");
        btnSendToLaser.setIcon(Icons.of(Icons.SEND, 13, Color.WHITE));
        btnSendToLaser.setEnabled(false);
        add(btnSendToLaser, "growx, h 24!, wrap");

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
        btnSendToLaser.addActionListener(e -> { if (callback != null) callback.onSendToLaser(); });
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
            btnSendToLaser.setEnabled(result != null);
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

    /** Compact toggle button tinted with the color of the path it controls. */
    private static JToggleButton createVisToggle(String label, boolean selected, Color tint, String tip) {
        JToggleButton btn = new JToggleButton(label, selected);
        btn.setFont(DarkTheme.FONT_SMALL);
        btn.setToolTipText(tip);
        btn.setFocusable(false);
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        btn.putClientProperty(FlatClientProperties.STYLE,
                "arc: 4; margin: 2,8,2,8; selectedForeground: #111111; selectedBackground: " + DarkTheme.toHex(tint) + ";");
        return btn;
    }
}
