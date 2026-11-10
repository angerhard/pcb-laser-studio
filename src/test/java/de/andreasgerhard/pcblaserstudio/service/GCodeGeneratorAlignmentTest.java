package de.andreasgerhard.pcblaserstudio.service;

import de.andreasgerhard.pcblaserstudio.model.BoardSettings;
import de.andreasgerhard.pcblaserstudio.model.Layer;
import de.andreasgerhard.pcblaserstudio.model.LayerSide;
import de.andreasgerhard.pcblaserstudio.model.LayerType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GCodeGeneratorAlignmentTest {

    private static final Pattern MOVE =
            Pattern.compile("^G[01]\\s+X([-+0-9.]+)\\s+Y([-+0-9.]+)");

    /**
     * 50 x 40 mm canvas whose black artwork sits at the SVG's lower-left
     * (2..8 mm from the left, 0..6 mm above the SVG bottom).
     */
    private static final String SVG = """
            <svg xmlns="http://www.w3.org/2000/svg"
                 width="50mm" height="40mm" viewBox="0 0 500 400">
              <rect x="20" y="340" width="60" height="60" fill="black"/>
            </svg>
            """;

    @Test
    void frontArtworkKeepsItsSvgOrientationAndBottomLeftAnchor() throws Exception {
        BoardSettings board = new BoardSettings();
        board.setWidthMm(100.0);
        board.setHeightMm(80.0);
        board.setMarginMm(5.0);
        board.setPositionXMm(10.0);
        board.setPositionYMm(20.0);

        Layer layer = new Layer("test", "Test", LayerType.ETCH, LayerSide.FRONT);
        layer.setGcodePrefix("");
        layer.setGcodeSuffix("");
        // Remove spot compensation and use a coarse stepover: the test only
        // checks coordinate placement, not raster quality.
        layer.getLaserSettings().setRadiusXMm(0.0);
        layer.getLaserSettings().setRadiusYMm(0.0);
        layer.getLaserSettings().setStepoverMm(0.5);

        GCodeGenerator.GCodeResult result = GCodeGenerator.generate(
                layer, SvgParser.parseXmlString(SVG), board, 256, 256, null);

        List<double[]> moves = parseMoves(result.gcode());
        assertFalse(moves.isEmpty(), "expected G-code moves");

        double minX = moves.stream().mapToDouble(m -> m[0]).min().orElseThrow();
        double maxX = moves.stream().mapToDouble(m -> m[0]).max().orElseThrow();
        double minY = moves.stream().mapToDouble(m -> m[1]).min().orElseThrow();
        double maxY = moves.stream().mapToDouble(m -> m[1]).max().orElseThrow();

        // Canvas lower-left is (15, 25) and the artwork is at its lower-left,
        // so the burned area must be near x=17..23 / y=25..31 mm.
        assertTrue(minX >= 16.0 && minX <= 18.0, "minX=" + minX);
        assertTrue(maxX >= 22.0 && maxX <= 24.0, "maxX=" + maxX);
        assertTrue(minY >= 24.0 && minY <= 26.0, "minY=" + minY);
        assertTrue(maxY >= 30.0 && maxY <= 32.0, "maxY=" + maxY);
    }

    private static List<double[]> parseMoves(String gcode) {
        List<double[]> moves = new ArrayList<>();
        for (String line : gcode.lines().toList()) {
            Matcher m = MOVE.matcher(line);
            if (m.find()) {
                moves.add(new double[]{
                        Double.parseDouble(m.group(1)),
                        Double.parseDouble(m.group(2))
                });
            }
        }
        return moves;
    }
}
