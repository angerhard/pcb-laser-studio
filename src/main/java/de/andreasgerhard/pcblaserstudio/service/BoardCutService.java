package de.andreasgerhard.pcblaserstudio.service;

import de.andreasgerhard.pcblaserstudio.model.BoardSettings;
import de.andreasgerhard.pcblaserstudio.model.CutSettings;
import de.andreasgerhard.pcblaserstudio.model.LayerSide;

import java.util.Locale;

/**
 * Generates a dedicated cut-out job along the existing SVG-border rectangle.
 *
 * <p>The geometry is the same rectangle traced by the border pass in
 * {@link GCodeGenerator}: the SVG canvas placed on the board for the selected
 * side. No kerf compensation is applied.</p>
 */
public final class BoardCutService {

    private BoardCutService() {}

    public record CutResult(
            String gcode,
            int lineCount,
            long estimatedTimeSeconds,
            double fileSizeKb,
            int passes,
            double xMm,
            double yMm,
            double widthMm,
            double heightMm
    ) {}

    /**
     * Builds the cut G-code.
     *
     * @param board            project board settings (position + flip axis)
     * @param side             side whose border is being cut
     * @param contentWidthMm   SVG canvas width
     * @param contentHeightMm  SVG canvas height
     * @param cut              power / feed / pass count
     * @param prefix           pre-GCODE from the source layer (may be blank)
     * @param suffix           post-GCODE from the source layer (may be blank)
     */
    public static CutResult generate(BoardSettings board, LayerSide side,
                                     double contentWidthMm, double contentHeightMm,
                                     CutSettings cut,
                                     String prefix, String suffix) {
        if (cut == null) cut = new CutSettings();
        if (contentWidthMm <= 0 || contentHeightMm <= 0) {
            throw new IllegalArgumentException("Cut rectangle must have a positive size.");
        }

        BoardAlignmentService.Rect rect = BoardAlignmentService.computeSvgBorderRect(
                board, side, contentWidthMm, contentHeightMm);

        double x0 = rect.xMm();
        double y0 = rect.yMm();
        double x1 = x0 + rect.widthMm();
        double y1 = y0 + rect.heightMm();

        double power = Math.max(0.0, Math.min(100.0, cut.getPowerPercent()));
        int sValue = (int) Math.round(power * 10.0);
        int feed = Math.max(1, cut.getFeedRateMmPerMin());
        int passes = Math.max(1, cut.getRepeats());

        StringBuilder sb = new StringBuilder();
        sb.append("; PCB Laser Studio - Board Cutout\n");
        sb.append("; Side: ").append(side.getDisplayName()).append("\n");
        sb.append(String.format(Locale.ENGLISH,
                "; Cut rectangle: %.3f x %.3f mm at X=%.3f Y=%.3f mm (lower-left)\n",
                rect.widthMm(), rect.heightMm(), x0, y0));
        sb.append(String.format(Locale.ENGLISH,
                "; Power: %.1f%%, Feed: %d mm/min, Passes: %d\n\n",
                power, feed, passes));

        appendBlock(sb, "; ===== PRE-GCODE =====", prefix);

        for (int pass = 1; pass <= passes; pass++) {
            sb.append(String.format(Locale.ENGLISH, "; ===== CUT PASS %d/%d =====\n", pass, passes));
            sb.append(String.format(Locale.ENGLISH,
                    "G0 X%.3f Y%.3f ; Rapid move (cut)\n", x0, y0));
            sb.append(String.format(Locale.ENGLISH, "M3 S%d ; Laser on\n", sValue));
            sb.append(String.format(Locale.ENGLISH,
                    "G1 X%.3f Y%.3f F%d\n", x1, y0, feed));
            sb.append(String.format(Locale.ENGLISH,
                    "G1 X%.3f Y%.3f\n", x1, y1));
            sb.append(String.format(Locale.ENGLISH,
                    "G1 X%.3f Y%.3f\n", x0, y1));
            sb.append(String.format(Locale.ENGLISH,
                    "G1 X%.3f Y%.3f\n", x0, y0));
            sb.append("M5 ; Laser off\n\n");
        }

        appendBlock(sb, "; ===== POST-GCODE =====", suffix);

        String gcode = sb.toString();
        int lineCount = (int) gcode.lines().count();
        double fileSizeKb = gcode.length() / 1024.0;

        double perimeterMm = 2.0 * (rect.widthMm() + rect.heightMm());
        long estimatedSeconds = Math.max(1,
                Math.round(perimeterMm * passes / feed * 60.0));

        return new CutResult(gcode, lineCount, estimatedSeconds, fileSizeKb,
                passes, x0, y0, rect.widthMm(), rect.heightMm());
    }

    private static void appendBlock(StringBuilder sb, String header, String block) {
        if (block == null || block.isBlank()) return;
        sb.append(header).append('\n');
        sb.append(block.stripTrailing()).append("\n\n");
    }
}
