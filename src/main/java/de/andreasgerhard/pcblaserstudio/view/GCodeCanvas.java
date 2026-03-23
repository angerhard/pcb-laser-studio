package de.andreasgerhard.pcblaserstudio.view;

import de.andreasgerhard.pcblaserstudio.service.GCodeGenerator;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.function.Consumer;

/**
 * Canvas that shows only the laser paths (GCode result) on a white PCB background.
 * Viewport (zoom + pan) is kept in sync with the sibling SvgCanvas by DualCanvasPanel.
 * The laser paths are pure vector data, so no bitmap scaling artefacts occur.
 */
public class GCodeCanvas extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(GCodeCanvas.class);

    private GCodeGenerator.GCodeResult gcodeResult;

    /** Coordinate-space size – must match the SVG image dimensions set via setImageSize(). */
    private int imgWidth  = 0;
    private int imgHeight = 0;

    private boolean showOutlines = true;
    private boolean showFill     = true;

    double zoom = 1.0;
    double panX = 0;
    double panY = 0;
    private Point lastDragPoint;

    /** Fired when the user changes zoom or pan; payload = [zoom, panX, panY]. */
    private Consumer<double[]> onViewportChanged;

    // =================== Constructor ===================

    public GCodeCanvas() {
        setBackground(DarkTheme.BG_DARKEST);
        setOpaque(true);
        setBorder(BorderFactory.createLineBorder(DarkTheme.BG_MEDIUM, 1));
        setupMouseHandlers();
    }

    // =================== Mouse handlers ===================

    private void setupMouseHandlers() {
        addMouseWheelListener(e -> {
            double factor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
            Point  mouse  = e.getPoint();
            panX = mouse.x - factor * (mouse.x - panX);
            panY = mouse.y - factor * (mouse.y - panY);
            zoom *= factor;
            zoom  = Math.max(0.05, Math.min(zoom, 50.0));
            fireViewportChanged();
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e) || e.isAltDown()) {
                    lastDragPoint = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }
            @Override public void mouseReleased(MouseEvent e) {
                lastDragPoint = null;
                setCursor(Cursor.getDefaultCursor());
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (lastDragPoint != null) {
                    panX += e.getX() - lastDragPoint.x;
                    panY += e.getY() - lastDragPoint.y;
                    lastDragPoint = e.getPoint();
                    fireViewportChanged();
                    repaint();
                }
            }
        });
    }

    private void fireViewportChanged() {
        if (onViewportChanged != null)
            onViewportChanged.accept(new double[]{zoom, panX, panY});
    }

    // =================== Data ===================

    public void setGcodeResult(GCodeGenerator.GCodeResult result) {
        this.gcodeResult = result;
        repaint();
    }

    public void clearGcodeResult() {
        this.gcodeResult = null;
        repaint();
    }

    /**
     * Sets the coordinate-space dimensions.
     * Must match the pixel size of the SvgCanvas's rendered svgImage so that
     * laser-path coordinates (which are in image-pixel space) align correctly.
     */
    public void setImageSize(int w, int h) {
        this.imgWidth  = w;
        this.imgHeight = h;
        repaint();
    }

    public void setShowOutlines(boolean show) { showOutlines = show; repaint(); }
    public void setShowFill    (boolean show) { showFill     = show; repaint(); }

    // =================== Viewport (sync API) ===================

    public void setOnViewportChanged(Consumer<double[]> cb) { this.onViewportChanged = cb; }

    /** Sets viewport without firing the onChange callback (prevents sync loops). */
    public void setViewport(double zoom, double panX, double panY) {
        this.zoom = zoom;
        this.panX = panX;
        this.panY = panY;
        repaint();
    }

    public void fitToWindow() {
        if (imgWidth <= 0 || imgHeight <= 0) return;
        int pw = getWidth()  > 0 ? getWidth()  : 800;
        int ph = getHeight() > 0 ? getHeight() : 600;
        double sx = (pw - 40.0) / imgWidth;
        double sy = (ph - 40.0) / imgHeight;
        zoom = Math.min(sx, sy);
        panX = (pw - imgWidth  * zoom) / 2.0;
        panY = (ph - imgHeight * zoom) / 2.0;
        repaint();
    }

    // =================== Painting ===================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        if (imgWidth <= 0 || imgHeight <= 0) {
            drawPlaceholder(g2);
        } else {
            g2.translate(panX, panY);
            g2.scale(zoom, zoom);

            // White PCB board background
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, imgWidth, imgHeight);

            if (gcodeResult != null) {
                drawLaserPaths(g2);
            } else {
                // Inline hint – rendered in image space so it scales with zoom
                g2.setColor(new Color(0x88, 0x88, 0x88));
                g2.setFont(DarkTheme.FONT_BOLD);
                String msg  = "Generate GCode to see laser paths";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg,
                        (int)((imgWidth  - fm.stringWidth(msg)) / 2.0),
                        (int)((imgHeight + fm.getAscent())       / 2.0));
            }

            // SVG-area border
            g2.setColor(DarkTheme.BG_MEDIUM);
            g2.setStroke(new BasicStroke((float)(1.0 / zoom)));
            g2.drawRect(0, 0, imgWidth, imgHeight);
        }

        g2.dispose();

        // Zoom overlay
        if (imgWidth > 0) {
            g.setColor(DarkTheme.TEXT_DIM);
            g.setFont(DarkTheme.FONT_SMALL);
            g.drawString(String.format("Zoom: %.0f%%", zoom * 100), 8, getHeight() - 8);
        }
    }

    private void drawPlaceholder(Graphics2D g2) {
        int w = getWidth(), h = getHeight();
        g2.setFont(DarkTheme.FONT_BOLD);
        g2.setColor(DarkTheme.TEXT_DIM);
        String msg = "Load SVG  →  Generate GCode";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2 + fm.getAscent() / 2);
    }

    private void drawLaserPaths(Graphics2D g2) {
        // Segments were generated in a render space of renderWidth × renderHeight.
        // The canvas coordinate space is imgWidth × imgHeight (= svgImage pixel size).
        // Both share the same aspect ratio, so we just need a uniform scale factor.
        double scaleX = gcodeResult.renderWidth()  > 0 ? (double) imgWidth  / gcodeResult.renderWidth()  : 1.0;
        double scaleY = gcodeResult.renderHeight() > 0 ? (double) imgHeight / gcodeResult.renderHeight() : 1.0;

        // Draw fill FIRST so the contour lines are always visible on top
        if (showFill && gcodeResult.fillSegments() != null) {
            g2.setColor(DarkTheme.COLOR_FILL);
            g2.setStroke(new BasicStroke((float) Math.max(0.3, 0.8 / zoom),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int[][] seg : gcodeResult.fillSegments()) {
                int x1 = (int) Math.round(seg[0][0] * scaleX), y1 = (int) Math.round(seg[0][1] * scaleY);
                int x2 = (int) Math.round(seg[1][0] * scaleX), y2 = (int) Math.round(seg[1][1] * scaleY);
                g2.drawLine(x1, y1, x2, y2);
            }
        }

        // Draw contour ON TOP of fill so the red outline lines are always visible
        if (showOutlines && gcodeResult.outlineSegments() != null) {
            g2.setColor(DarkTheme.COLOR_OUTLINE);
            g2.setStroke(new BasicStroke((float) Math.max(0.5, 1.0 / zoom),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int[][] seg : gcodeResult.outlineSegments()) {
                int x1 = (int) Math.round(seg[0][0] * scaleX), y1 = (int) Math.round(seg[0][1] * scaleY);
                int x2 = (int) Math.round(seg[1][0] * scaleX), y2 = (int) Math.round(seg[1][1] * scaleY);
                g2.drawLine(x1, y1, x2, y2);
            }
        }
    }
}

