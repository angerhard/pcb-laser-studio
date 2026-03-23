package de.andreasgerhard.pcblaserstudio.view;

import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Holds SvgCanvas (SVG view) and GCodeCanvas (laser-path view) in a JSplitPane.
 *
 * <p>Features:
 * <ul>
 *   <li>Vertical split (side-by-side) by default, toggled to horizontal (stacked) via button.</li>
 *   <li>Viewport (zoom + pan) is synchronised bidirectionally between both canvases.</li>
 *   <li>When the SVG image is ready, its pixel dimensions are forwarded to GCodeCanvas
 *       so that laser-path coordinates map to the same coordinate space.</li>
 * </ul>
 */
public class DualCanvasPanel extends JPanel {

    public final SvgCanvas   svgCanvas;
    public final GCodeCanvas gcodeCanvas;

    private final JSplitPane splitPane;
    private final JButton    toggleBtn;

    /** true = side-by-side (vertical divider / HORIZONTAL_SPLIT in Swing terms) */
    private boolean splitVertical = true;

    /** Guard flag to prevent viewport-sync feedback loops. */
    private boolean syncingViewport = false;

    // =================== Constructor ===================

    public DualCanvasPanel() {
        super(new BorderLayout(0, 0));
        setBackground(DarkTheme.BG_DARKEST);

        svgCanvas   = new SvgCanvas();
        gcodeCanvas = new GCodeCanvas();

        // ── Header bar ─────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(DarkTheme.BG_DARK);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DarkTheme.BG_MEDIUM));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DarkTheme.BG_MEDIUM),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));

        JLabel lblSvg   = DarkTheme.createDimLabel("SVG");
        JLabel lblGcode = DarkTheme.createDimLabel("G-Code Vorschau");

        // Two labels centered over their respective panels
        JPanel lblBar = new JPanel(new GridLayout(1, 2, 0, 0));
        lblBar.setBackground(DarkTheme.BG_DARK);
        lblBar.add(lblSvg);
        lblBar.add(lblGcode);

        toggleBtn = DarkTheme.createButton("⬍ Horizontal", "Wechsel zu Oben/Unten-Aufteilung");
        toggleBtn.setFont(DarkTheme.FONT_SMALL);
        toggleBtn.addActionListener(e -> toggleSplitOrientation());

        header.add(lblBar,     BorderLayout.CENTER);
        header.add(toggleBtn,  BorderLayout.EAST);

        // ── Inner split pane ───────────────────────────────────────────────────
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, svgCanvas, gcodeCanvas);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(5);
        splitPane.setBorder(null);
        splitPane.setBackground(DarkTheme.BG_DARKEST);

        add(header,    BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        // ── Viewport sync (bidirectional, loop-safe) ───────────────────────────
        svgCanvas.setOnViewportChanged(vp -> {
            if (!syncingViewport) {
                syncingViewport = true;
                gcodeCanvas.setViewport(vp[0], vp[1], vp[2]);
                syncingViewport = false;
            }
        });

        gcodeCanvas.setOnViewportChanged(vp -> {
            if (!syncingViewport) {
                syncingViewport = true;
                svgCanvas.setViewport(vp[0], vp[1], vp[2]);
                syncingViewport = false;
            }
        });

        // When SvgCanvas has rendered a new image, forward its pixel size to GCodeCanvas
        // so that laser-path coordinates (in image-pixel space) align correctly.
        svgCanvas.setOnImageReady(dim -> gcodeCanvas.setImageSize(dim.width, dim.height));
    }

    // =================== Split toggle ==========================================

    private void toggleSplitOrientation() {
        splitVertical = !splitVertical;
        if (splitVertical) {
            splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
            toggleBtn.setText("⬍ Horizontal");
            toggleBtn.setToolTipText("Wechsel zu Oben/Unten-Aufteilung");
        } else {
            splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            toggleBtn.setText("⬌ Vertikal");
            toggleBtn.setToolTipText("Wechsel zu Links/Rechts-Aufteilung");
        }
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.5));
    }

    /** Apply the 50/50 inner divider after the component has been laid out. */
    public void applyDivider() {
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.5));
    }

    // =================== Proxy zoom controls (applied to SVG canvas; sync does the rest) =

    public void fitToWindow() { svgCanvas.fitToWindow(); }
    public void zoomIn()      { svgCanvas.zoomIn();      }
    public void zoomOut()     { svgCanvas.zoomOut();     }
    public void zoom100()     { svgCanvas.zoom100();     }
}

