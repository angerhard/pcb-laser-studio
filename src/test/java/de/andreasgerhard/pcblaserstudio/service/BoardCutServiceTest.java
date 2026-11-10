package de.andreasgerhard.pcblaserstudio.service;

import de.andreasgerhard.pcblaserstudio.model.BoardSettings;
import de.andreasgerhard.pcblaserstudio.model.CutSettings;
import de.andreasgerhard.pcblaserstudio.model.LayerSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardCutServiceTest {

    private static BoardSettings board() {
        BoardSettings b = new BoardSettings();
        b.setWidthMm(100.0);
        b.setHeightMm(80.0);
        b.setMarginMm(5.0);
        b.setPositionXMm(10.0);
        b.setPositionYMm(20.0);
        return b;
    }

    private static CutSettings cut(double power, int feed, int repeats) {
        CutSettings c = new CutSettings();
        c.setPowerPercent(power);
        c.setFeedRateMmPerMin(feed);
        c.setRepeats(repeats);
        return c;
    }

    @Test
    void emitsTheSvgBorderRectangleWithConfiguredPowerFeedAndPasses() {
        BoardCutService.CutResult result = BoardCutService.generate(
                board(), LayerSide.FRONT, 50.0, 40.0,
                cut(80.0, 200, 3), "", "");

        assertEquals(15.0, result.xMm(), 1e-9);
        assertEquals(25.0, result.yMm(), 1e-9);
        assertEquals(50.0, result.widthMm(), 1e-9);
        assertEquals(40.0, result.heightMm(), 1e-9);
        assertEquals(3, result.passes());

        String gcode = result.gcode();
        assertTrue(gcode.contains("G0 X15.000 Y25.000"), gcode);
        assertTrue(gcode.contains("G1 X65.000 Y25.000 F200"), gcode);
        assertTrue(gcode.contains("G1 X65.000 Y65.000"), gcode);
        assertTrue(gcode.contains("G1 X15.000 Y65.000"), gcode);
        assertTrue(gcode.contains("G1 X15.000 Y25.000"), gcode);
        assertTrue(gcode.contains("M3 S800"), gcode);
        assertEquals(3, count(gcode, "CUT PASS"));
    }

    @Test
    void backVerticalCutUsesMirroredBorderPosition() {
        BoardCutService.CutResult result = BoardCutService.generate(
                board(), LayerSide.BACK, 50.0, 40.0,
                cut(100.0, 300, 1), "", "");

        assertEquals(55.0, result.xMm(), 1e-9);
        assertEquals(25.0, result.yMm(), 1e-9);
        assertTrue(result.gcode().contains("G0 X55.000 Y25.000"), result.gcode());
    }

    @Test
    void includesLayerPrefixAndSuffix() {
        BoardCutService.CutResult result = BoardCutService.generate(
                board(), LayerSide.FRONT, 50.0, 40.0,
                cut(100.0, 300, 1), "G21\nG90\n", "M5\n");

        String gcode = result.gcode();
        int prefix = gcode.indexOf("G21");
        int firstPass = gcode.indexOf("CUT PASS");
        int suffix = gcode.indexOf("M5\n", firstPass);
        assertTrue(prefix >= 0 && prefix < firstPass, gcode);
        assertTrue(suffix > firstPass, gcode);
    }

    private static int count(String text, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
