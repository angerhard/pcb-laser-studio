package de.andreasgerhard.pcblaserstudio.service;

import de.andreasgerhard.pcblaserstudio.model.BoardSettings;
import de.andreasgerhard.pcblaserstudio.model.FlipAxis;
import de.andreasgerhard.pcblaserstudio.model.LaserSettings;
import de.andreasgerhard.pcblaserstudio.model.Layer;
import de.andreasgerhard.pcblaserstudio.model.LayerSide;
import de.andreasgerhard.pcblaserstudio.model.LayerType;
import de.andreasgerhard.pcblaserstudio.model.ProcessingMode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Generates Marlin-compatible GCODE from a Layer's SVG data.
 * Uses image-based analysis for raster fill and JTS for outline offsets.
 */
public class GCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(GCodeGenerator.class);

    /**
     * @param renderWidth   pixel width used for the analysis image and segment coordinates
     * @param renderHeight  pixel height used for the analysis image and segment coordinates
     * @param borderSegments  board-border pass segments (exact SVG bounding box), empty if disabled
     * @param contentWidthMm  physical width of the SVG content (same coordinate space as segments)
     * @param contentHeightMm physical height of the SVG content
     * @param offsetXMm     final X offset actually used (board auto-alignment + manual fine-tune)
     * @param offsetYMm     final Y offset actually used (board auto-alignment + manual fine-tune)
     * @param mirrorX       whether the rendered artwork was mirrored horizontally for this layer
     * @param mirrorY       whether the rendered artwork was mirrored vertically for this layer
     */
    public record GCodeResult(
            String gcode,
            List<int[][]> outlineSegments,
            List<int[][]> fillSegments,
            List<int[][]> borderSegments,
            int lineCount,
            long estimatedTimeSeconds,
            double fileSizeKb,
            int[] powerHistogram,
            int renderWidth,    // coordinate space of the segments
            int renderHeight,
            double contentWidthMm,
            double contentHeightMm,
            double offsetXMm,
            double offsetYMm,
            boolean mirrorX,
            boolean mirrorY
    ) {}

    /**
     * Generate GCODE for a single layer.
     * @param layer     the layer model
     * @param parseResult  parsed SVG data
     * @param boardSettings  the project's board settings (size/margin/flip axis), used to
     *                       auto-align front and back side layers; may be {@code null} to
     *                       fall back to the layer's manual offset only
     * @param renderWidth  pixel width of rendered image
     * @param renderHeight pixel height of rendered image
     * @param progressCallback  optional callback (0-100)
     */
    public static GCodeResult generate(
            Layer layer,
            SvgParser.SvgParseResult parseResult,
            BoardSettings boardSettings,
            int renderWidth,
            int renderHeight,
            Consumer<Integer> progressCallback
    ) {
        LaserSettings ls = layer.getLaserSettings();
        boolean engraveBlack = layer.getProcessingMode() == ProcessingMode.BLACK_AREA;

        // ── 1. Determine the Batik rendering bounds ──────────────────────────
        // Batik converts SVG units (mm, cm, pt …) to device pixels internally.
        // JTS geometries and renderToImage() both operate in this pixel space,
        // so we must use it as our common coordinate system.
        SvgParser.ContentBounds contentBounds = SvgParser.getContentBounds(parseResult);
        double batikW = contentBounds.widthPx();
        double batikH = contentBounds.heightPx();

        // Scale Batik bounds to the requested render size, preserving aspect ratio.
        // This matches exactly what renderToImage() does, so there is NO centering
        // offset and coordinates in the analysis image map 1:1 to segment coordinates.
        double batikScale = Math.min((double) renderWidth / batikW,
                                     (double) renderHeight / batikH);
        int actualW = Math.max(1, (int) Math.round(batikW * batikScale));
        int actualH = Math.max(1, (int) Math.round(batikH * batikScale));

        if (progressCallback != null) progressCallback.accept(5);
        BufferedImage img = SvgParser.renderToImage(parseResult, actualW, actualH);

        // ── 1b. Mirror back-side artwork, if requested ───────────────────────
        // Many PCB/CAM tools export the back layer already mirrored; this is an
        // opt-in toggle for projects whose back SVG is drawn as seen from the top.
        boolean mirrorX = false, mirrorY = false;
        if (boardSettings != null && layer.getSide() == LayerSide.BACK && boardSettings.isMirrorBackArtwork()) {
            if (boardSettings.getFlipAxis() == FlipAxis.VERTICAL) mirrorX = true;
            else mirrorY = true;
        }
        if (mirrorX || mirrorY) {
            img = mirrorImage(img, mirrorX, mirrorY);
        }

        // ── 2. Laser parameters in pixel space ───────────────────────────────
        // Physical size of the rendered content:
        //   Batik device pixel = 1/96 inch = 25.4/96 mm  →  1 mm = 96/25.4 ≈ 3.7795 Batik px
        // So: contentWidthMm = batikW / 3.7795  and  mmPerRenderPx = contentWidthMm / actualW
        double contentWidthMm  = contentBounds.widthMm();
        double contentHeightMm = contentBounds.heightMm();
        double pxPerMm         = actualW / contentWidthMm;

        // ── 2b. Final offset: board auto-alignment + per-layer manual fine-tune ──
        double offsetXMm, offsetYMm;
        if (boardSettings != null) {
            BoardAlignmentService.Offset base = BoardAlignmentService.computeBaseOffset(
                    boardSettings, layer.getSide(), contentWidthMm, contentHeightMm);
            offsetXMm = base.xMm() + ls.getOffsetXMm();
            offsetYMm = base.yMm() + ls.getOffsetYMm();
        } else {
            offsetXMm = ls.getOffsetXMm();
            offsetYMm = ls.getOffsetYMm();
        }

        int radiusXPx = (int) Math.round(ls.getRadiusXMm() * pxPerMm);
        int radiusYPx = (int) Math.round(ls.getRadiusYMm() * pxPerMm);
        int stepoverPx     = Math.max(1, (int) Math.round(ls.getStepoverMm()  * pxPerMm));
        int power          = (int) Math.round(ls.getPowerPercent() * 10.0); // S0–S1000
        int feedRate       = (int) ls.getFeedRateMmPerMin();
        int contourPower   = (int) Math.round(ls.getContourPowerPercent() * 10.0);
        int contourFeedRate= (int) ls.getContourFeedRateMmPerMin();

        // ── 3. Analyse image pixels ──────────────────────────────────────────
        boolean[][] engrave = analyzeImage(img, engraveBlack);
        // Apply morphological compensation based on the anisotropic spot radius
        // (rectangle of radiusXPx × radiusYPx). The laser spot is not round, so
        // X and Y are eroded independently.
        if (radiusXPx != 0 || radiusYPx != 0) {
            engrave = applyMorphology(engrave, radiusXPx, radiusYPx);
        }
        if (progressCallback != null) progressCallback.accept(25);

        // ── 4. Fill segments (raster) – in actualW × actualH space ──────────
        List<int[][]> fillSegs = generateRasterSegments(engrave, stepoverPx, actualW, actualH);
        if (progressCallback != null) progressCallback.accept(55);

        // ── 5. Contour segments – boundary tracing on the engrave mask ──────────
        // The mask-based approach traces the exact boundary of the post-morphology
        // engrave mask, so the contour accounts for the spot-radius compensation
        // and works for all shape types (filled areas AND stroked open traces).
        // Consecutive boundary pixels in the same direction are merged into runs,
        // keeping the segment count low and avoiding pixel-scale artefacts.
        List<int[][]> outlineSegs = ls.isGenerateContour()
                ? generateContoursFromMask(engrave, actualW, actualH)
                : List.of();
        if (progressCallback != null) progressCallback.accept(80);

        // ── 5b. Border pass – exact SVG bounding box, one per side (Etch layer only) ──
        boolean addBorder = boardSettings != null && boardSettings.isGenerateBorder()
                && layer.getType() == LayerType.ETCH;
        List<int[][]> borderSegs = addBorder ? buildBorderSegments(actualW, actualH) : List.of();

        // ── 6. Build GCODE string ────────────────────────────────────────────
        StringBuilder sb = new StringBuilder();
        sb.append("; PCB Laser Studio - Generated GCODE\n");
        sb.append("; Layer: ").append(layer.getName()).append("\n");
        sb.append("; Mode: ").append(layer.getProcessingMode().getDisplayName()).append("\n");
        sb.append(String.format(Locale.ENGLISH,
                "; Content size: %.3f x %.3f mm\n", contentWidthMm, contentHeightMm));
        sb.append(String.format(Locale.ENGLISH,
                "; Radius: X=%.3f mm, Y=%.3f mm, Stepover: %.3f mm\n",
                ls.getRadiusXMm(), ls.getRadiusYMm(), ls.getStepoverMm()));
        sb.append(String.format(Locale.ENGLISH,
                "; Feed: %d mm/min, Power: %.0f%%\n", feedRate, ls.getPowerPercent()));
        sb.append(String.format(Locale.ENGLISH,
                "; Offset: X=%.3f mm, Y=%.3f mm%s\n\n",
                offsetXMm, offsetYMm, boardSettings != null ? " (board-aligned + manual fine-tune)" : ""));

        // Pre-GCODE
        if (layer.getGcodePrefix() != null && !layer.getGcodePrefix().isBlank()) {
            sb.append("; ===== PRE-GCODE =====\n");
            sb.append(layer.getGcodePrefix()).append("\n");
        }

        // Contour pass (outline inset by radius)
        if (!outlineSegs.isEmpty()) {
            sb.append(String.format(Locale.ENGLISH,
                    "; ===== CONTOUR PASS (inset X=%.3f mm / Y=%.3f mm) =====\n",
                    ls.getRadiusXMm(), ls.getRadiusYMm()));
            appendSegments(sb, outlineSegs, contourFeedRate, contourPower,
                    contentWidthMm, contentHeightMm, actualW, actualH, true,
                    offsetXMm, offsetYMm);
        }

        // Border pass (exact SVG bounding box, reuses the contour pass settings)
        if (!borderSegs.isEmpty()) {
            sb.append("; ===== BORDER PASS (SVG bounding box) =====\n");
            appendSegments(sb, borderSegs, contourFeedRate, contourPower,
                    contentWidthMm, contentHeightMm, actualW, actualH, true,
                    offsetXMm, offsetYMm);
        }

        // Fill / raster passes
        if (!fillSegs.isEmpty()) {
            sb.append("; ===== FILL PASS =====\n");
            appendSegments(sb, fillSegs, feedRate, power,
                    contentWidthMm, contentHeightMm, actualW, actualH, false,
                    offsetXMm, offsetYMm);
        }

        // Post-GCODE
        if (layer.getGcodeSuffix() != null && !layer.getGcodeSuffix().isBlank()) {
            sb.append("; ===== POST-GCODE =====\n");
            sb.append(layer.getGcodeSuffix()).append("\n");
        }

        if (progressCallback != null) progressCallback.accept(100);

        String gcode     = sb.toString();
        int lineCount    = (int) gcode.lines().count();
        double fileSizeKb = gcode.length() / 1024.0;

        long totalMoves  = fillSegs.size() + outlineSegs.size() + borderSegs.size();
        long estSeconds  = totalMoves > 0 ? (totalMoves * 10L) / (feedRate / 60 + 1) : 0;

        int[] histogram  = new int[101];
        histogram[Math.min(100, (int) ls.getPowerPercent())] = lineCount;

        log.info("GCODE generated: {} lines, {} KB, ~{} s", lineCount,
                String.format("%.1f", fileSizeKb), estSeconds);

        return new GCodeResult(gcode, outlineSegs, fillSegs, borderSegs,
                lineCount, estSeconds, fileSizeKb, histogram,
                actualW, actualH, contentWidthMm, contentHeightMm,
                offsetXMm, offsetYMm, mirrorX, mirrorY);
    }

    /** Mirrors a rendered image horizontally and/or vertically (for back-side artwork). */
    private static BufferedImage mirrorImage(BufferedImage src, boolean mirrorX, boolean mirrorY) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        AffineTransform t = new AffineTransform();
        if (mirrorX) { t.translate(w, 0); t.scale(-1, 1); }
        if (mirrorY) { t.translate(0, h); t.scale(1, -1); }
        g2.drawImage(src, t, null);
        g2.dispose();
        return out;
    }

    /** Rectangle matching the exact SVG bounding box, in render-pixel space. */
    private static List<int[][]> buildBorderSegments(int w, int h) {
        List<int[][]> segs = new ArrayList<>();
        segs.add(new int[][]{{0, 0}, {w, 0}});
        segs.add(new int[][]{{w, 0}, {w, h}});
        segs.add(new int[][]{{w, h}, {0, h}});
        segs.add(new int[][]{{0, h}, {0, 0}});
        return segs;
    }

    // =================== Pixel analysis ===================

    private static boolean[][] analyzeImage(BufferedImage img, boolean engraveBlack) {
        int w = img.getWidth(), h = img.getHeight();
        boolean[][] result = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = new Color(img.getRGB(x, y), true);
                if (c.getAlpha() < 128) { result[y][x] = !engraveBlack; continue; }
                float brightness = (c.getRed() + c.getGreen() + c.getBlue()) / (3f * 255f);
                result[y][x] = engraveBlack == (brightness < 0.5f);
            }
        }
        return result;
    }

    /**
     * Applies morphological erosion to the boolean engraving mask using an
     * anisotropic (rectangular) structuring element with half-extents
     * {@code radiusXPx} in X and {@code radiusYPx} in Y, via an efficient
     * separable box-filter.
     *
     * <p>Because the laser spot is not round (e.g. 0.05 × 0.08 mm), the erosion
     * distance differs per axis. A pixel is engraved only if <em>all</em> pixels
     * within the {@code radiusXPx × radiusYPx} box window are also engraved.
     * This keeps the laser centre at least its spot radius away from
     * non-engraved boundaries, preventing the laser spot from overrunning fine
     * structures.</p>
     *
     * <p>Two-pass separable morphology (horizontal then vertical) using prefix sums
     * gives O(h × w) complexity regardless of radius size.</p>
     *
     * @param radiusXPx  horizontal half-extent of the structuring element in px
     * @param radiusYPx  vertical half-extent of the structuring element in px
     */
    private static boolean[][] applyMorphology(boolean[][] src, int radiusXPx, int radiusYPx) {
        if (radiusXPx == 0 && radiusYPx == 0) return src;
        int h = src.length, w = src[0].length;

        // ── Horizontal pass (X radius) ─────────────────────────────────────
        boolean[][] temp = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            int[] prefix = new int[w + 1];
            for (int x = 0; x < w; x++) prefix[x + 1] = prefix[x] + (src[y][x] ? 1 : 0);
            for (int x = 0; x < w; x++) {
                int lo = Math.max(0, x - radiusXPx);
                int hi = Math.min(w, x + radiusXPx + 1);
                temp[y][x] = prefix[hi] - prefix[lo] == hi - lo;
            }
        }

        // ── Vertical pass (Y radius) ────────────────────────────────────────
        boolean[][] result = new boolean[h][w];
        for (int x = 0; x < w; x++) {
            int[] prefix = new int[h + 1];
            for (int y = 0; y < h; y++) prefix[y + 1] = prefix[y] + (temp[y][x] ? 1 : 0);
            for (int y = 0; y < h; y++) {
                int lo = Math.max(0, y - radiusYPx);
                int hi = Math.min(h, y + radiusYPx + 1);
                result[y][x] = prefix[hi] - prefix[lo] == hi - lo;
            }
        }
        return result;
    }

    // =================== Raster segments ===================

    private static List<int[][]> generateRasterSegments(boolean[][] engrave, int stepoverPx, int w, int h) {
        List<int[][]> segs = new ArrayList<>();
        boolean leftToRight = true;
        for (int y = 0; y < h; y += stepoverPx) {
            List<int[]> runs = findRuns(engrave[y], w);
            if (!runs.isEmpty()) {
                if (leftToRight) {
                    for (int[] run : runs) segs.add(new int[][]{{run[0], y}, {run[1], y}});
                } else {
                    for (int i = runs.size() - 1; i >= 0; i--) {
                        int[] run = runs.get(i);
                        segs.add(new int[][]{{run[1], y}, {run[0], y}});
                    }
                }
                leftToRight = !leftToRight;
            }
        }
        return segs;
    }

    private static List<int[]> findRuns(boolean[] row, int w) {
        List<int[]> runs = new ArrayList<>();
        int start = -1;
        for (int x = 0; x < w; x++) {
            if (row[x] && start < 0)        { start = x; }
            else if (!row[x] && start >= 0) { runs.add(new int[]{start, x - 1}); start = -1; }
        }
        if (start >= 0) runs.add(new int[]{start, w - 1});
        return runs;
    }

    // =================== Outline / Contour segments ===================

    /**
     * Traces the boundary of the post-morphology {@code engrave} mask and returns
     * axis-aligned (horizontal + vertical) contour segments.
     *
     * <p>The algorithm scans every inter-row and inter-column boundary line.
     * Wherever the mask transitions from engraved to non-engraved (or vice-versa)
     * it opens a new segment run; when the transition ends the run is closed and
     * saved.  Consecutive touching boundary pixels in the same direction are
     * automatically merged into a single longer segment, so the result is compact
     * and free of pixel-scale artefacts.</p>
     *
     * <p>The approach works for <em>all</em> SVG element types – filled shapes,
     * stroked open traces, copper pours – because it operates on the rasterised
     * mask rather than the original vector geometry.  The mask already incorporates
     * spot-radius morphological compensation, so the contour inherently respects
     * the configured radius offset.</p>
     *
     * @param engrave  boolean mask [height][width]; {@code true} = pixel to be engraved
     * @param w        mask width  (number of columns)
     * @param h        mask height (number of rows)
     * @return list of 2-point segments in image-pixel coordinates
     */
    private static List<int[][]> generateContoursFromMask(boolean[][] engrave, int w, int h) {
        List<int[][]> segs = new ArrayList<>();

        // ── Horizontal boundary lines ────────────────────────────────────────
        // Scan y from 0 to h (inclusive).  The "line" at y sits between row y-1
        // and row y.  A boundary exists at column x when the mask value changes
        // across that line.
        for (int y = 0; y <= h; y++) {
            int runStart = -1;
            for (int x = 0; x < w; x++) {
                boolean above = (y > 0) && engrave[y - 1][x];
                boolean below = (y < h) && engrave[y][x];
                boolean edge  = above != below;          // transition → boundary
                if (edge && runStart < 0) {
                    runStart = x;                        // open a new run
                } else if (!edge && runStart >= 0) {
                    segs.add(new int[][]{{runStart, y}, {x, y}});
                    runStart = -1;
                }
            }
            if (runStart >= 0) {
                segs.add(new int[][]{{runStart, y}, {w, y}});
            }
        }

        // ── Vertical boundary lines ──────────────────────────────────────────
        // Scan x from 0 to w (inclusive).  The "line" at x sits between column
        // x-1 and column x.
        for (int x = 0; x <= w; x++) {
            int runStart = -1;
            for (int y = 0; y < h; y++) {
                boolean left  = (x > 0) && engrave[y][x - 1];
                boolean right = (x < w) && engrave[y][x];
                boolean edge  = left != right;
                if (edge && runStart < 0) {
                    runStart = y;
                } else if (!edge && runStart >= 0) {
                    segs.add(new int[][]{{x, runStart}, {x, y}});
                    runStart = -1;
                }
            }
            if (runStart >= 0) {
                segs.add(new int[][]{{x, runStart}, {x, h}});
            }
        }

        log.info("generateContoursFromMask: {} contour segments ({}×{} mask)", segs.size(), w, h);
        return segs;
    }

    private static void processGeometry(Geometry g, List<int[][]> segs,
                                        double sx, double sy, double offX, double offY) {
        if (g == null || g.isEmpty()) return;
        if (g instanceof Polygon poly) {
            addRingSegments(poly.getExteriorRing().getCoordinates(), segs, sx, sy, offX, offY);
            for (int i = 0; i < poly.getNumInteriorRing(); i++)
                addRingSegments(poly.getInteriorRingN(i).getCoordinates(), segs, sx, sy, offX, offY);
        } else if (g instanceof GeometryCollection gc) {
            for (int i = 0; i < gc.getNumGeometries(); i++)
                processGeometry(gc.getGeometryN(i), segs, sx, sy, offX, offY);
        }
        // LineString case intentionally omitted: shapeToGeometry only produces Polygons now
    }

    private static void addRingSegments(Coordinate[] coords, List<int[][]> segs,
                                        double sx, double sy, double offX, double offY) {
        for (int i = 0; i < coords.length - 1; i++) {
            segs.add(new int[][]{
                    {(int) Math.round((coords[i].x   - offX) * sx), (int) Math.round((coords[i].y   - offY) * sy)},
                    {(int) Math.round((coords[i+1].x - offX) * sx), (int) Math.round((coords[i+1].y - offY) * sy)}
            });
        }
    }

    // =================== GCODE formatting ===================

    private static void appendSegments(
            StringBuilder sb, List<int[][]> segs, int feedRate, int power,
            double svgW, double svgH, int renderW, int renderH, boolean isOutline,
            double offsetXMm, double offsetYMm
    ) {
        // mm per render-pixel  (svgW mm spread over renderW pixels)
        double mmPerPxX = svgW / renderW;
        double mmPerPxY = svgH / renderH;
        int[]   lastPos = null;
        boolean laserOn = false;

        for (int[][] seg : segs) {
            int[] p1 = seg[0], p2 = seg[1];
            double x1mm = p1[0] * mmPerPxX + offsetXMm, y1mm = p1[1] * mmPerPxY + offsetYMm;
            double x2mm = p2[0] * mmPerPxX + offsetXMm, y2mm = p2[1] * mmPerPxY + offsetYMm;

            boolean needsMove = lastPos == null || lastPos[0] != p1[0] || lastPos[1] != p1[1];
            if (needsMove) {
                if (laserOn) { sb.append("M5 ; Laser off\n"); laserOn = false; }
                sb.append(String.format(Locale.ENGLISH, "G0 X%.3f Y%.3f ; Rapid move%s\n",
                        x1mm, y1mm, isOutline ? " (contour)" : " (fill)"));
            }
            if (!laserOn) { sb.append(String.format(Locale.ENGLISH, "M3 S%d ; Laser on\n", power)); laserOn = true; }
            sb.append(String.format(Locale.ENGLISH, "G1 X%.3f Y%.3f F%d\n", x2mm, y2mm, feedRate));
            lastPos = p2;
        }
        if (laserOn) sb.append("M5 ; Laser off\n");
        sb.append("\n");
    }

    public static String formatTime(long seconds) {
        return String.format("%d:%02d min", seconds / 60, seconds % 60);
    }
}

