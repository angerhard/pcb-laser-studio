package de.andreasgerhard.pcblaserstudio.view;

import de.andreasgerhard.pcblaserstudio.model.Layer;
import de.andreasgerhard.pcblaserstudio.model.LayerType;
import de.andreasgerhard.pcblaserstudio.service.GCodeGenerator;
import de.andreasgerhard.pcblaserstudio.view.components.CollapsibleSection;
import de.andreasgerhard.pcblaserstudio.view.panels.*;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Right-side tool panel.
 * The three settings sections (Layer, Laser, Script) are stacked as
 * Lightroom-style collapsible sections, so everything is reachable by
 * scrolling rather than hidden behind sub-tabs.
 * GCODE generation controls and the histogram are pinned to the bottom.
 */
public class ToolPanel extends JPanel {

    public final LayerSettingsPanel layerSettingsPanel;
    public final LaserParametersPanel laserParametersPanel;
    public final GCodePrePostPanel gcodePrePostPanel;
    public final GCodeGenerationPanel gcodeGenerationPanel;
    public final HistogramPanel histogramPanel;

    public ToolPanel() {
        // Sections column grows; bottom row takes its (compact) preferred height.
        setLayout(new MigLayout("fill, insets 0, gapy 0", "[grow]", "[grow][pref]"));
        setBackground(DarkTheme.BG_DARK);
        setPreferredSize(new Dimension(300, 600));
        setMinimumSize(new Dimension(260, 0));

        // --- Collapsible settings sections (Layer / Laser / Script) ---
        layerSettingsPanel   = new LayerSettingsPanel();
        laserParametersPanel = new LaserParametersPanel();
        gcodePrePostPanel    = new GCodePrePostPanel();

        JPanel sections = new JPanel(new MigLayout("fillx, insets 0, gapy 2", "[grow]", ""));
        sections.setOpaque(false);
        sections.add(new CollapsibleSection("Layer", layerSettingsPanel, true), "growx, wrap");
        sections.add(new CollapsibleSection("Laser", laserParametersPanel, true), "growx, wrap");
        sections.add(new CollapsibleSection("Script", gcodePrePostPanel, false), "growx, wrap");

        JScrollPane sectionsScroll = new JScrollPane(sections,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sectionsScroll.getVerticalScrollBar().setUnitIncrement(14);
        sectionsScroll.setBorder(BorderFactory.createEmptyBorder());
        sectionsScroll.getViewport().setOpaque(false);
        sectionsScroll.setOpaque(false);

        // --- Bottom: generation controls + histogram (always visible) ---
        gcodeGenerationPanel = new GCodeGenerationPanel();
        histogramPanel       = new HistogramPanel();
        histogramPanel.setPreferredSize(new Dimension(260, 100));

        JPanel bottomPanel = new JPanel(new MigLayout("fillx, insets 0, gapy 0", "[grow]", ""));
        bottomPanel.setBackground(DarkTheme.BG_DARK);
        bottomPanel.add(gcodeGenerationPanel, "growx, wrap");
        bottomPanel.add(histogramPanel,       "growx, h 100:100:100, wrap");

        add(sectionsScroll, "grow, wrap");
        add(bottomPanel,     "growx");
    }

    public void setLayer(Layer layer) {
        if (layer == null) return;
        layerSettingsPanel.setLayer(layer);
        laserParametersPanel.setSettings(layer.getLaserSettings());
        gcodePrePostPanel.setLayer(layer);
        gcodeGenerationPanel.setCutEnabled(layer.getType() == LayerType.ETCH);
    }

    public void updateGcodeStats(GCodeGenerator.GCodeResult result) {
        gcodeGenerationPanel.updateStats(result);
        if (result != null) {
            histogramPanel.setHistogram(result.powerHistogram());
        }
    }
}
