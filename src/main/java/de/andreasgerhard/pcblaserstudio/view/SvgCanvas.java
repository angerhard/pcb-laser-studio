package de.andreasgerhard.pcblaserstudio.view;

import de.andreasgerhard.pcblaserstudio.model.BoardSettings;
import de.andreasgerhard.pcblaserstudio.model.FlipAxis;
import de.andreasgerhard.pcblaserstudio.model.Layer;
import de.andreasgerhard.pcblaserstudio.model.LayerSide;
import de.andreasgerhard.pcblaserstudio.service.BoardAlignmentService;
import de.andreasgerhard.pcblaserstudio.service.SvgParser;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Canvas for displaying the SVG image.
 * Supports zoom (mouse wheel) and pan (middle-button / Alt+drag).
 * Viewport changes are broadcast via {@link #setOnViewportChanged} so that
 * DualCanvasPanel can synchronise the sibling GCodeCanvas.
 */
public class SvgCanvas extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(SvgCanvas.class);

    private Layer layer;
    private SvgParser.SvgParseResult parseResult;
    private BufferedImage svgImage;

    private boolean showSvg = true;

    // ── Board-space alignment (physical PCB preview) ────────────────────────
    private BoardSettings boardSettings;
    private double boardWidthPx  = 0;
    private double boardHeightPx = 0;
    private double offsetXPx = 0;
    private double offsetYPx = 0;
    private boolean mirrorX = false;
    private boolean mirrorY = false;

    private double zoom = 1.0;
    private double panX = 0;
    private double panY = 0;
    private Point lastDragPoint;

    /** True when an SVG was loaded but fitToWindow() couldn't run yet (size not available). */
    private boolean pendingFit = false;

    /** Debounce timer: fires ~350 ms after the last zoom gesture and triggers a high-res re-render. */
    private final Timer renderDebounceTimer;

    // ── Callbacks ─────────────────────────────────────────────────────────────
    /** Fired when the user changes zoom or pan; payload = [zoom, panX, panY]. */
    private Consumer<double[]> onViewportChanged;
    /** Fired when a new SVG image has been rendered; payload = image pixel dimensions. */
    private Consumer<Dimension> onImageReady;
    private Consumer<File>   onFileDropped;
    private Consumer<String> onStatusMessage;

    // =================== Constructor ===================

    public SvgCanvas() {
        setBackground(DarkTheme.BG_DARKEST);
        setOpaque(true);
        setBorder(BorderFactory.createLineBorder(DarkTheme.BG_MEDIUM, 1));

        renderDebounceTimer = new Timer(350, e -> triggerHighResRender());
        renderDebounceTimer.setRepeats(false);

        setupDragDrop();
        setupMouseHandlers();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (pendingFit && getWidth() > 0 && getHeight() > 0) {
                    pendingFit = false;
                    fitToWindow();
                }
            }
        });
    }

    // =================== Setup ===================

    private void setupDragDrop() {
        setTransferHandler(new TransferHandler() {
            @Override public boolean canImport(TransferSupport s) {
                return s.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }
            @Override public boolean importData(TransferSupport s) {
                if (!canImport(s)) return false;
                try {
                    Transferable t = s.getTransferable();
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        File f = files.get(0);
                        if (f.getName().toLowerCase().endsWith(".svg") && onFileDropped != null) {
                            onFileDropped.accept(f);
                            return true;
                        }
                    }
                } catch (Exception e) { log.error("Drag-drop error", e); }
                return false;
            }
        });
    }

    private void setupMouseHandlers() {
        // Zoom towards mouse cursor
        addMouseWheelListener(e -> {
            double factor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
            Point  mouse  = e.getPoint();
            panX = mouse.x - factor * (mouse.x - panX);
            panY = mouse.y - factor * (mouse.y - panY);
            zoom *= factor;
            zoom  = Math.max(0.05, Math.min(zoom, 50.0));
            renderDebounceTimer.restart();
            fireViewportChanged();
            repaint();
        });

        // Pan (middle button or Alt+drag)
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
            @Override public void mouseMoved(MouseEvent e) {
                if (onStatusMessage != null && svgImage != null && parseResult != null) {
                    double mmX = (e.getX() - panX) / zoom * (parseResult.svgWidth()  / svgImage.getWidth());
                    double mmY = (e.getY() - panY) / zoom * (parseResult.svgHeight() / svgImage.getHeight());
                    onStatusMessage.accept(String.format("X: %.2f mm  Y: %.2f mm  Zoom: %.0f%%",
                            mmX, mmY, zoom * 100));
                }
            }
        });
    }

    // =================== Callbacks ===================

    public void setOnViewportChanged(Consumer<double[]> cb)  { this.onViewportChanged = cb; }
    public void setOnImageReady     (Consumer<Dimension> cb) { this.onImageReady = cb; }
    public void setOnFileDropped    (Consumer<File>   cb)    { this.onFileDropped = cb; }
    public void setOnStatusMessage  (Consumer<String> cb)    { this.onStatusMessage = cb; }

    private void fireViewportChanged() {
        if (onViewportChanged != null)
            onViewportChanged.accept(new double[]{zoom, panX, panY});
    }

    // =================== Data ===================

    public void setLayer(Layer layer) { this.layer = layer; }

    /** Sets the project's board settings and recomputes the board-space preview. */
    public void setBoardSettings(BoardSettings boardSettings) {
        this.boardSettings = boardSettings;
        recomputeAlignment();
        repaint();
    }

    /**
     * Recomputes the board-space layout (board size, content offset, mirror).
     * Call after the SVG, layer side, or board settings change.
     */
    public void refreshAlignment() {
        recomputeAlignment();
        repaint();
    }

    private void recomputeAlignment() {
        mirrorX = false;
        mirrorY = false;
        if (boardSettings == null || parseResult == null || svgImage == null) {
            boardWidthPx  = svgImage != null ? svgImage.getWidth()  : 0;
            boardHeightPx = svgImage != null ? svgImage.getHeight() : 0;
            offsetXPx = 0;
            offsetYPx = 0;
            return;
        }
        SvgParser.ContentBounds cb = SvgParser.getContentBounds(parseResult);
        double pxPerMmX = svgImage.getWidth()  / cb.widthMm();
        double pxPerMmY = svgImage.getHeight() / cb.heightMm();

        boardWidthPx  = boardSettings.getWidthMm()  * pxPerMmX;
        boardHeightPx = boardSettings.getHeightMm() * pxPerMmY;

        LayerSide side  = layer != null ? layer.getSide() : LayerSide.FRONT;
        BoardAlignmentService.Offset base = BoardAlignmentService.computeBaseOffset(
                boardSettings, side, cb.widthMm(), cb.heightMm());
        offsetXPx = base.xMm() * pxPerMmX;
        offsetYPx = base.yMm() * pxPerMmY;

        if (side == LayerSide.BACK && boardSettings.isMirrorBackArtwork()) {
            if (boardSettings.getFlipAxis() == FlipAxis.VERTICAL) mirrorX = true;
            else mirrorY = true;
        }
    }

    public void setSvgParseResult(SvgParser.SvgParseResult result, int renderSize) {
        renderDebounceTimer.stop();
        this.parseResult = result;
        if (result == null) {
            svgImage   = null;
            pendingFit = false;
            repaint();
            return;
        }
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            @Override protected BufferedImage doInBackground() {
                return renderAtSize(result, renderSize);
            }
            @Override protected void done() {
                try {
                    svgImage = get();
                    recomputeAlignment();
                    // Notify GCodeCanvas of the new coordinate-space size
                    if (onImageReady != null)
                        onImageReady.accept(new Dimension(svgImage.getWidth(), svgImage.getHeight()));
                    if (getWidth() > 0 && getHeight() > 0) {
                        fitToWindow();   // fires repaint + viewport change
                    } else {
                        pendingFit = true;
                        repaint();
                    }
                } catch (Exception e) { log.error("Render error", e); }
            }
        };
        worker.execute();
    }

    // =================== Visibility ===================

    public void setShowSvg(boolean show) { showSvg = show; repaint(); }

    // =================== Viewport (sync API) ===================

    /** Sets viewport without firing the onChange callback (prevents sync loops). */
    public void setViewport(double zoom, double panX, double panY) {
        this.zoom = zoom;
        this.panX = panX;
        this.panY = panY;
        repaint();
    }

    // =================== Zoom controls ===================

    public void fitToWindow() {
        if (svgImage == null) return;
        double contentW = boardWidthPx  > 0 ? boardWidthPx  : svgImage.getWidth();
        double contentH = boardHeightPx > 0 ? boardHeightPx : svgImage.getHeight();
        int pw = getWidth()  > 0 ? getWidth()  : 800;
        int ph = getHeight() > 0 ? getHeight() : 600;
        double scaleX = (pw - 40.0) / contentW;
        double scaleY = (ph - 40.0) / contentH;
        zoom = Math.min(scaleX, scaleY);
        panX = (pw - contentW * zoom) / 2.0;
        panY = (ph - contentH * zoom) / 2.0;
        fireViewportChanged();
        repaint();
    }

    public void zoomIn()  { zoom = Math.min(zoom * 1.25, 50.0); renderDebounceTimer.restart(); fireViewportChanged(); repaint(); }
    public void zoomOut() { zoom = Math.max(zoom * 0.8,  0.05); renderDebounceTimer.restart(); fireViewportChanged(); repaint(); }
    public void zoom100() { zoom = 1.0; centerImage();           renderDebounceTimer.restart(); fireViewportChanged(); repaint(); }

    private void centerImage() {
        if (svgImage == null) return;
        panX = (getWidth()  - svgImage.getWidth()  * zoom) / 2.0;
        panY = (getHeight() - svgImage.getHeight() * zoom) / 2.0;
    }

    // =================== Painting ===================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (svgImage == null) {
            drawDropZone(g2);
        } else {
            g2.translate(panX, panY);
            g2.scale(zoom, zoom);

            // Physical board outline, drawn behind the SVG content at its true scale.
            if (boardWidthPx > 0 && boardHeightPx > 0) {
                int bw = (int) Math.round(boardWidthPx), bh = (int) Math.round(boardHeightPx);
                g2.setColor(new Color(0x3a, 0x3a, 0x3a));
                g2.fillRect(0, 0, bw, bh);
                g2.setColor(DarkTheme.BG_LIGHT);
                g2.setStroke(new BasicStroke((float)(1.5 / zoom)));
                g2.drawRect(0, 0, bw, bh);
            }

            // Content, translated to its board-aligned position (and mirrored for
            // back-side artwork when enabled), drawn on top of the board outline.
            Graphics2D cg = (Graphics2D) g2.create();
            cg.translate(offsetXPx, offsetYPx);
            if (mirrorX) { cg.translate(svgImage.getWidth(), 0); cg.scale(-1, 1); }
            if (mirrorY) { cg.translate(0, svgImage.getHeight()); cg.scale(1, -1); }

            if (showSvg) {
                cg.drawImage(svgImage, 0, 0, null);
            } else {
                cg.setColor(Color.WHITE);
                cg.fillRect(0, 0, svgImage.getWidth(), svgImage.getHeight());
            }

            // SVG-area border
            cg.setColor(DarkTheme.BG_MEDIUM);
            cg.setStroke(new BasicStroke((float)(1.0 / zoom)));
            cg.drawRect(0, 0, svgImage.getWidth(), svgImage.getHeight());
            cg.dispose();
        }

        g2.dispose();

        if (svgImage != null) {
            g.setColor(DarkTheme.TEXT_DIM);
            g.setFont(DarkTheme.FONT_SMALL);
            g.drawString(String.format("Zoom: %.0f%%", zoom * 100), 8, getHeight() - 8);
        }
    }

    private void drawDropZone(Graphics2D g2) {
        int w = getWidth(), h = getHeight();
        float[] dash = {8f, 6f};
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
        g2.setColor(DarkTheme.BG_MEDIUM);
        int margin = 40;
        g2.drawRoundRect(margin, margin, w - 2*margin, h - 2*margin, 20, 20);

        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        g2.setColor(DarkTheme.BG_LIGHT);
        String arrow = "⬇";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(arrow, (w - fm.stringWidth(arrow)) / 2, h / 2 - 20);

        g2.setFont(DarkTheme.FONT_BOLD);
        g2.setColor(DarkTheme.TEXT_DIM);
        String msg = "Drop SVG file here";
        fm = g2.getFontMetrics();
        g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2 + 15);

        g2.setFont(DarkTheme.FONT_SMALL);
        String sub = "or use File → Open SVG";
        fm = g2.getFontMetrics();
        g2.drawString(sub, (w - fm.stringWidth(sub)) / 2, h / 2 + 35);
    }

    // =================== Progressive high-res render ===================

    /**
     * Renders the SVG at the given max pixel size.
     * Dimensions are derived from {@code graphicsNode.getBounds()} (the actual Batik
     * content bounds), which is the same basis used by renderToImage() and by
     * GCodeGenerator – so all coordinate spaces stay consistent.
     */
    private static BufferedImage renderAtSize(SvgParser.SvgParseResult result, int maxSize) {
        java.awt.geom.Rectangle2D bounds = result.graphicsNode().getBounds();
        double bW, bH;
        if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0) {
            bW = bounds.getWidth();
            bH = bounds.getHeight();
        } else {
            bW = Math.max(100, result.svgWidth());
            bH = Math.max(100, result.svgHeight());
        }
        double s = (double) maxSize / Math.max(bW, bH);
        int w = (int) Math.max(1, Math.round(bW * s));
        int h = (int) Math.max(1, Math.round(bH * s));
        return SvgParser.renderToImage(result, w, h);
    }

    private void triggerHighResRender() {
        if (parseResult == null || svgImage == null) return;
        if (zoom <= 1.0) return;

        int currentMaxSize = Math.max(svgImage.getWidth(), svgImage.getHeight());
        int neededSize     = (int) Math.ceil(currentMaxSize * zoom);
        neededSize         = Math.min(neededSize, 8192);

        if (neededSize <= currentMaxSize) return;

        final double scaleFactor = (double) neededSize / currentMaxSize;
        final SvgParser.SvgParseResult pr = parseResult;
        final int targetSize = neededSize;

        log.debug("High-res render: {}px (scale={})", targetSize, String.format("%.2f", scaleFactor));

        new SwingWorker<BufferedImage, Void>() {
            @Override protected BufferedImage doInBackground() {
                return renderAtSize(pr, targetSize);
            }
            @Override protected void done() {
                try {
                    svgImage = get();
                    recomputeAlignment();
                    zoom = zoom / scaleFactor;   // keep screen position identical
                    // Forward new image size to GCodeCanvas
                    if (onImageReady != null)
                        onImageReady.accept(new Dimension(svgImage.getWidth(), svgImage.getHeight()));
                    fireViewportChanged();
                    repaint();
                } catch (Exception ex) { log.error("High-res render error", ex); }
            }
        }.execute();
    }
}
