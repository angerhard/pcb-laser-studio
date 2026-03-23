package de.andreasgerhard.pcblaserstudio.view;

import de.andreasgerhard.pcblaserstudio.model.Layer;
import de.andreasgerhard.pcblaserstudio.service.GCodeGenerator;
import de.andreasgerhard.pcblaserstudio.view.panels.*;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Right-side tool panel.
 * The three settings sections (Layer, Laser, Script) live in a compact
 * tabbed pane so they are always reachable without scrolling.
 * GCODE generation controls and the histogram are pinned to the bottom.
 */
public class ToolPanel extends JPanel {

    public final LayerSettingsPanel layerSettingsPanel;
    public final LaserParametersPanel laserParametersPanel;
    public final GCodePrePostPanel gcodePrePostPanel;
    public final GCodeGenerationPanel gcodeGenerationPanel;
    public final HistogramPanel histogramPanel;

    public ToolPanel() {
        setLayout(new BorderLayout());
        setBackground(DarkTheme.BG_DARK);
        setPreferredSize(new Dimension(300, 600));

        // --- Settings tabs (Layer / Laser / Script) ---
        layerSettingsPanel   = new LayerSettingsPanel();
        laserParametersPanel = new LaserParametersPanel();
        gcodePrePostPanel    = new GCodePrePostPanel();

        JTabbedPane settingsTabs = new JTabbedPane(JTabbedPane.TOP);
        settingsTabs.setFont(DarkTheme.FONT_SMALL);
        settingsTabs.setBackground(DarkTheme.BG_DARKEST);
        settingsTabs.setForeground(DarkTheme.TEXT_PRIMARY);
        settingsTabs.addTab("Layer",  wrapScroll(layerSettingsPanel));
        settingsTabs.addTab("Laser",  wrapScroll(laserParametersPanel));
        settingsTabs.addTab("Script", wrapScroll(gcodePrePostPanel));
        settingsTabs.setToolTipTextAt(0, "Layer metadata (name, type, side, mode)");
        settingsTabs.setToolTipTextAt(1, "Laser engraving parameters (radius, stepover, feed, power, offset)");
        settingsTabs.setToolTipTextAt(2, "GCODE prefix / suffix scripts");

        // --- Bottom: generation controls + histogram (always visible) ---
        gcodeGenerationPanel = new GCodeGenerationPanel();
        histogramPanel       = new HistogramPanel();
        histogramPanel.setPreferredSize(new Dimension(260, 120));

        JPanel bottomPanel = new JPanel(new MigLayout("fillx, insets 0, gapy 0", "[grow]", ""));
        bottomPanel.setBackground(DarkTheme.BG_DARK);
        bottomPanel.add(gcodeGenerationPanel, "growx, wrap");
        bottomPanel.add(histogramPanel,       "growx, h 120:120:120, wrap");

        add(settingsTabs, BorderLayout.CENTER);
        add(bottomPanel,  BorderLayout.SOUTH);
    }

    /** Wraps a panel in a scroll pane without a visible border. */
    private static JScrollPane wrapScroll(JPanel panel) {
        JScrollPane sp = new JScrollPane(panel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(12);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setBackground(DarkTheme.BG_DARK);
        return sp;
    }

    public void setLayer(Layer layer) {
        if (layer == null) return;
        layerSettingsPanel.setLayer(layer);
        laserParametersPanel.setSettings(layer.getLaserSettings());
        gcodePrePostPanel.setLayer(layer);
    }

    public void updateGcodeStats(GCodeGenerator.GCodeResult result) {
        gcodeGenerationPanel.updateStats(result);
        if (result != null) {
            histogramPanel.setHistogram(result.powerHistogram());
        }
    }
}

