package de.andreasgerhard.pcblaserstudio.view;

import de.andreasgerhard.pcblaserstudio.model.Layer;
import de.andreasgerhard.pcblaserstudio.service.GCodeGenerator;

import javax.swing.*;
import java.awt.*;

/**
 * Content panel for a single tab:
 * DualCanvasPanel (left, SVG + GCode side by side) + ToolPanel (right) in a JSplitPane.
 */
public class LayerTab extends JPanel {

    public final DualCanvasPanel dualCanvas;
    /** Shortcut to dualCanvas.svgCanvas (used by controller). */
    public final SvgCanvas   svgCanvas;
    /** Shortcut to dualCanvas.gcodeCanvas (used by controller). */
    public final GCodeCanvas gcodeCanvas;
    public final ToolPanel   toolPanel;

    private final JSplitPane splitPane;
    private Layer layer;

    public LayerTab(Layer layer) {
        super(new BorderLayout());
        this.layer = layer;

        dualCanvas = new DualCanvasPanel();
        svgCanvas  = dualCanvas.svgCanvas;
        gcodeCanvas= dualCanvas.gcodeCanvas;

        svgCanvas.setLayer(layer);

        toolPanel = new ToolPanel();
        toolPanel.setLayer(layer);
        toolPanel.setMinimumSize(new Dimension(250, 0));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dualCanvas, toolPanel);
        splitPane.setDividerLocation(0.75);
        splitPane.setResizeWeight(1.0);
        splitPane.setBorder(null);
        splitPane.setDividerSize(5);
        splitPane.setBackground(new Color(0x1a1a1a));

        add(splitPane, BorderLayout.CENTER);
    }

    public Layer getLayer() { return layer; }

    public void setLayer(Layer layer) {
        this.layer = layer;
        svgCanvas.setLayer(layer);
        toolPanel.setLayer(layer);
    }

    public void applyDivider() {
        SwingUtilities.invokeLater(() -> {
            int total     = splitPane.getWidth();
            int toolWidth = toolPanel.getPreferredSize().width;
            int divider   = splitPane.getDividerSize();
            if (total > toolWidth + divider) {
                splitPane.setDividerLocation(total - toolWidth - divider);
            }
            dualCanvas.applyDivider();
        });
    }

    public void updateGcodeResult(GCodeGenerator.GCodeResult result) {
        gcodeCanvas.setGcodeResult(result);
        toolPanel.updateGcodeStats(result);
    }
}
