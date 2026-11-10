package de.andreasgerhard.pcblaserstudio.service;

import de.andreasgerhard.pcblaserstudio.model.Layer;
import de.andreasgerhard.pcblaserstudio.model.LayerSide;
import de.andreasgerhard.pcblaserstudio.model.LayerType;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SvgParserTest {

    private static final double MM_PER_PX = 25.4 / 96.0;

    @Test
    void usesDeclaredPhysicalSizeInsteadOfArtworkBounds() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg"
                     width="100mm" height="80mm" viewBox="0 0 1000 800">
                  <rect x="400" y="300" width="200" height="200" fill="black"/>
                </svg>
                """;

        SvgParser.SvgParseResult result = SvgParser.parseXmlString(svg);
        SvgParser.DocumentBounds bounds = SvgParser.getDocumentBounds(result);

        assertEquals(100.0, bounds.widthMm(), 0.01);
        assertEquals(80.0, bounds.heightMm(), 0.01);
        assertEquals(100.0 / MM_PER_PX, bounds.widthPx(), 0.1);
        assertEquals(80.0 / MM_PER_PX, bounds.heightPx(), 0.1);

        // The artwork itself is much smaller than the declared canvas.
        double artworkWidthMm = result.graphicsNode().getBounds().getWidth() * MM_PER_PX;
        assertTrue(artworkWidthMm < 50.0);
    }

    @Test
    void convertsPixelDimensionsToMillimetres() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="1000" height="800">
                  <rect x="0" y="0" width="10" height="10" fill="black"/>
                </svg>
                """;

        SvgParser.DocumentBounds bounds =
                SvgParser.getDocumentBounds(SvgParser.parseXmlString(svg));

        assertEquals(1000.0, bounds.widthPx(), 0.001);
        assertEquals(800.0, bounds.heightPx(), 0.001);
        assertEquals(1000.0 * MM_PER_PX, bounds.widthMm(), 0.01);
        assertEquals(800.0 * MM_PER_PX, bounds.heightMm(), 0.01);
    }

    @Test
    void fallsBackToViewBoxWhenWidthAndHeightAreMissing() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 800">
                  <rect x="0" y="0" width="100" height="100" fill="black"/>
                </svg>
                """;

        SvgParser.DocumentBounds bounds =
                SvgParser.getDocumentBounds(SvgParser.parseXmlString(svg));

        assertEquals(1000.0, bounds.widthPx(), 0.001);
        assertEquals(800.0, bounds.heightPx(), 0.001);
    }

    @Test
    void rendersArtworkAtItsPositionWithinTheFullCanvas() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg"
                     width="100mm" height="80mm" viewBox="0 0 1000 800">
                  <rect x="500" y="400" width="200" height="200" fill="black"/>
                </svg>
                """;

        SvgParser.SvgParseResult result = SvgParser.parseXmlString(svg);
        BufferedImage image = SvgParser.renderToImage(result, 100, 80);

        int x = (int) Math.round(600 * 100.0 / 1000.0);
        int y = (int) Math.round(500 * 80.0 / 800.0);
        assertEquals(255, (image.getRGB(x, y) >>> 24) & 0xFF);
        assertEquals(0, (image.getRGB(10, 10) >>> 24) & 0xFF);
    }

    @Test
    void identicalCanvasWithDifferentArtworkProducesIdenticalGcodeSize() throws Exception {
        String svgA = """
                <svg xmlns="http://www.w3.org/2000/svg"
                     width="50mm" height="40mm" viewBox="0 0 500 400">
                  <rect x="100" y="100" width="100" height="100" fill="black"/>
                </svg>
                """;
        String svgB = """
                <svg xmlns="http://www.w3.org/2000/svg"
                     width="50mm" height="40mm" viewBox="0 0 500 400">
                  <rect x="0" y="0" width="50" height="50" fill="black"/>
                </svg>
                """;

        GCodeGenerator.GCodeResult a = generate(svgA);
        GCodeGenerator.GCodeResult b = generate(svgB);

        assertEquals(50.0, a.contentWidthMm(), 0.01);
        assertEquals(40.0, a.contentHeightMm(), 0.01);
        assertEquals(a.contentWidthMm(), b.contentWidthMm(), 0.0001);
        assertEquals(a.contentHeightMm(), b.contentHeightMm(), 0.0001);
    }

    private static GCodeGenerator.GCodeResult generate(String svg) throws Exception {
        Layer layer = new Layer("test", "Test", LayerType.ETCH, LayerSide.FRONT);
        return GCodeGenerator.generate(layer, SvgParser.parseXmlString(svg), null, 128, 128, null);
    }
}
