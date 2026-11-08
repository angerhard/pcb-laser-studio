package de.andreasgerhard.pcblaserstudio.service;

import de.andreasgerhard.pcblaserstudio.model.BoardSettings;
import de.andreasgerhard.pcblaserstudio.model.FlipAxis;
import de.andreasgerhard.pcblaserstudio.model.Layer;
import de.andreasgerhard.pcblaserstudio.model.LayerSide;
import de.andreasgerhard.pcblaserstudio.model.LayerType;
import de.andreasgerhard.pcblaserstudio.model.Project;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Computes where each layer's SVG content must be burned so that front and
 * back side layers end up at exactly the same physical location once the
 * board is turned over.
 *
 * <p>The board is assumed to be flipped in place (180° rotation around its
 * own centerline), so its bounding box never moves. The front side is always
 * anchored at the board's origin corner plus the configured margin; the back
 * side is anchored at the mirror image of that corner, reflected across the
 * board's center line on the configured {@link FlipAxis}.</p>
 */
public class BoardAlignmentService {

    private static final double DEFAULT_TOLERANCE_MM = 0.1;

    public record Offset(double xMm, double yMm) {}

    private BoardAlignmentService() {}

    /**
     * Computes the auto-aligned base offset (before any per-layer manual
     * fine-tune offset is added) for a piece of content of the given
     * physical size.
     */
    public static Offset computeBaseOffset(BoardSettings board, LayerSide side,
                                            double contentWidthMm, double contentHeightMm) {
        double margin = board.getMarginMm();
        if (side == LayerSide.FRONT) {
            return new Offset(margin, margin);
        }
        // BACK — mirrored across the board's own center line on the flip axis
        if (board.getFlipAxis() == FlipAxis.VERTICAL) {
            double x = board.getWidthMm() - margin - contentWidthMm;
            return new Offset(x, margin);
        } else {
            double y = board.getHeightMm() - margin - contentHeightMm;
            return new Offset(margin, y);
        }
    }

    /**
     * Compares SVG content sizes across layers and returns a human-readable
     * warning for every mismatch found: circuit vs. documentation on the same
     * side, and front side vs. back side. Returns an empty list when
     * everything lines up (within {@link #DEFAULT_TOLERANCE_MM}).
     */
    public static List<String> findSizeMismatches(Project project,
                                                    Map<String, SvgParser.SvgParseResult> parseCache) {
        List<String> warnings = new ArrayList<>();

        Map<LayerSide, Map<LayerType, SvgParser.ContentBounds>> bySideType = new EnumMap<>(LayerSide.class);
        for (Layer layer : project.getLayers()) {
            SvgParser.SvgParseResult pr = parseCache.get(layer.getId());
            if (pr == null) continue;
            bySideType.computeIfAbsent(layer.getSide(), s -> new EnumMap<>(LayerType.class))
                    .put(layer.getType(), SvgParser.getContentBounds(pr));
        }

        for (LayerSide side : LayerSide.values()) {
            Map<LayerType, SvgParser.ContentBounds> m = bySideType.get(side);
            if (m == null) continue;
            SvgParser.ContentBounds etch = m.get(LayerType.ETCH);
            SvgParser.ContentBounds doc = m.get(LayerType.DOCUMENTATION);
            if (etch != null && doc != null && !sizesMatch(etch, doc)) {
                warnings.add(String.format(Locale.ENGLISH,
                        "%s: Etch (%.2f×%.2f mm) and Documentation (%.2f×%.2f mm) SVG sizes differ.",
                        side.getDisplayName(), etch.widthMm(), etch.heightMm(), doc.widthMm(), doc.heightMm()));
            }
        }

        SvgParser.ContentBounds front = preferEtch(bySideType.get(LayerSide.FRONT));
        SvgParser.ContentBounds back = preferEtch(bySideType.get(LayerSide.BACK));
        if (front != null && back != null && !sizesMatch(front, back)) {
            warnings.add(String.format(Locale.ENGLISH,
                    "Front (%.2f×%.2f mm) and Back (%.2f×%.2f mm) SVG sizes differ — " +
                            "the two sides may not align after flipping.",
                    front.widthMm(), front.heightMm(), back.widthMm(), back.heightMm()));
        }

        return warnings;
    }

    private static SvgParser.ContentBounds preferEtch(Map<LayerType, SvgParser.ContentBounds> m) {
        if (m == null) return null;
        SvgParser.ContentBounds etch = m.get(LayerType.ETCH);
        return etch != null ? etch : m.get(LayerType.DOCUMENTATION);
    }

    private static boolean sizesMatch(SvgParser.ContentBounds a, SvgParser.ContentBounds b) {
        return Math.abs(a.widthMm() - b.widthMm()) <= DEFAULT_TOLERANCE_MM
                && Math.abs(a.heightMm() - b.heightMm()) <= DEFAULT_TOLERANCE_MM;
    }
}
