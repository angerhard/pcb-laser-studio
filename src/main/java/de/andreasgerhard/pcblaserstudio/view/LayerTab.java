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

    /** Constrain the tool panel width so its contents never balloon or overflow. */
    private static final int TOOL_MIN = 260;
    private static final int TOOL_PREF = 300;
    private static final int TOOL_MAX = 380;

    public LayerTab(Layer layer) {
        super(new BorderLayout());
        this.layer = layer;

        dualCanvas = new DualCanvasPanel();
        svgCanvas  = dualCanvas.svgCanvas;
        gcodeCanvas= dualCanvas.gcodeCanvas;

        svgCanvas.setLayer(layer);

        toolPanel = new ToolPanel();
        toolPanel.setLayer(layer);
        toolPanel.setMinimumSize(new Dimension(TOOL_MIN, 0));
        toolPanel.setMaximumSize(new Dimension(TOOL_MAX, Integer.MAX_VALUE));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dualCanvas, toolPanel);
        splitPane.setDividerLocation(TOOL_PREF);
        splitPane.setResizeWeight(1.0);
        splitPane.setBorder(null);
        splitPane.setDividerSize(5);
        splitPane.setBackground(new Color(0x1a1a1a));

        // Keep the tool panel within [TOOL_MIN .. TOOL_MAX] even when the user drags the splitter.
        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e ->
                SwingUtilities.invokeLater(() -> {
                    int w = splitPane.getWidth();
                    if (w <= 0) return;
                    int div = splitPane.getDividerSize();
                    int tool = w - div - splitPane.getDividerLocation();
                    if (tool > TOOL_MAX) {
                        splitPane.setDividerLocation(w - div - TOOL_MAX);
                    } else if (tool < TOOL_MIN && w - div > TOOL_MIN) {
                        splitPane.setDividerLocation(w - div - TOOL_MIN);
                    }
                }));

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
            int total   = splitPane.getWidth();
            int divider = splitPane.getDividerSize();
            if (total > divider + TOOL_PREF) {
                splitPane.setDividerLocation(total - divider - TOOL_PREF);
            } else if (total > 0) {
                splitPane.setDividerLocation(0);
            }
            dualCanvas.applyDivider();
        });
    }

    public void updateGcodeResult(GCodeGenerator.GCodeResult result) {
        gcodeCanvas.setGcodeResult(result);
        toolPanel.updateGcodeStats(result);
    }
}
