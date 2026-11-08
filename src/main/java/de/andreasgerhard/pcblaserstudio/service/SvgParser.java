package de.andreasgerhard.pcblaserstudio.service;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Parses SVG data into a renderable Batik GraphicsNode and JTS geometries.
 */
public class SvgParser {

    private static final Logger log = LoggerFactory.getLogger(SvgParser.class);
    private static final GeometryFactory GF = new GeometryFactory();

    /** Batik device pixel = 1/96 inch = 25.4/96 mm → 1 mm = 96/25.4 ≈ 3.7795 Batik px. */
    public static final double MM_PER_BATIK_PX = 25.4 / 96.0;

    public record SvgParseResult(
            GraphicsNode graphicsNode,
            Document document,
            double svgWidth,
            double svgHeight,
            List<Geometry> geometries
    ) {}

    /**
     * Actual rendered content bounds of a parsed SVG, in both Batik device
     * pixels and physical millimetres. This is the single source of truth
     * used by {@link de.andreasgerhard.pcblaserstudio.service.GCodeGenerator}
     * and the board-alignment logic, so every consumer agrees on the exact
     * physical size of the content.
     */
    public record ContentBounds(double widthPx, double heightPx, double widthMm, double heightMm) {}

    /**
     * Returns the actual rendering bounds of the parsed SVG content.
     * Falls back to the declared {@code svgWidth}/{@code svgHeight} attributes
     * when Batik can't report bounds (e.g. empty document).
     */
    public static ContentBounds getContentBounds(SvgParseResult result) {
        Rectangle2D bounds = result.graphicsNode().getBounds();
        double w, h;
        if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0) {
            w = bounds.getWidth();
            h = bounds.getHeight();
        } else {
            w = result.svgWidth();
            h = result.svgHeight();
        }
        return new ContentBounds(w, h, w * MM_PER_BATIK_PX, h * MM_PER_BATIK_PX);
    }

    /**
     * Parse SVG from a base64-encoded string.
     */
    public static SvgParseResult parseBase64(String base64Svg) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64Svg);
        String xmlString = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        return parseXmlString(xmlString);
    }

    /**
     * Parse SVG from a File.
     */
    public static SvgParseResult parseFile(File svgFile) throws Exception {
        String uri = svgFile.toURI().toString();
        String parserClass = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parserClass);
        Document doc = factory.createDocument(uri);
        return buildResult(doc, uri);
    }

    /**
     * Parse SVG from raw XML string.
     */
    public static SvgParseResult parseXmlString(String xml) throws Exception {
        String parserClass = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parserClass);
        String dummyUri = "file:///svg-content.svg";
        Document doc = factory.createDocument(dummyUri, new StringReader(xml));
        return buildResult(doc, dummyUri);
    }

    private static SvgParseResult buildResult(Document doc, String uri) {
        UserAgentAdapter userAgent = new UserAgentAdapter();
        DocumentLoader loader = new DocumentLoader(userAgent);
        BridgeContext ctx = new BridgeContext(userAgent, loader);
        ctx.setDynamicState(BridgeContext.STATIC);

        GVTBuilder builder = new GVTBuilder();
        GraphicsNode rootNode = builder.build(ctx, doc);

        // Get SVG dimensions
        org.w3c.dom.Element svgEl = doc.getDocumentElement();
        double width = parseDimension(svgEl.getAttribute("width"), 800);
        double height = parseDimension(svgEl.getAttribute("height"), 600);

        // Try viewBox if width/height missing
        String viewBox = svgEl.getAttribute("viewBox");
        if (width == 800 && height == 600 && viewBox != null && !viewBox.isEmpty()) {
            String[] vb = viewBox.trim().split("[\\s,]+");
            if (vb.length == 4) {
                width = Double.parseDouble(vb[2]);
                height = Double.parseDouble(vb[3]);
            }
        }

        List<Geometry> geometries = extractGeometries(rootNode);
        return new SvgParseResult(rootNode, doc, width, height, geometries);
    }

    /**
     * Render the parsed SVG to a BufferedImage.
     */
    public static BufferedImage renderToImage(SvgParseResult result, int targetWidth, int targetHeight) {
        BufferedImage img = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        // Leave background transparent (TYPE_INT_ARGB) – the canvas draws its own background.

        // Use the actual rendering bounds from the Batik graphicsNode.
        // This is essential for SVGs that use physical units (mm, cm, pt, in),
        // because Batik converts those to device pixels internally (at ~96 dpi).
        // Using svgWidth/svgHeight directly (e.g. 210 for "210mm") would result
        // in a scale factor that is ~3.78× too large, showing only a small portion.
        Rectangle2D bounds = result.graphicsNode().getBounds();
        double nodeW, nodeH, nodeX, nodeY;
        if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0) {
            nodeW = bounds.getWidth();
            nodeH = bounds.getHeight();
            nodeX = bounds.getX();
            nodeY = bounds.getY();
        } else {
            // Fallback: SVG uses px units → Batik renders 1:1 with attribute values
            nodeW = result.svgWidth();
            nodeH = result.svgHeight();
            nodeX = 0;
            nodeY = 0;
        }

        // Scale to fit the target dimensions while preserving aspect ratio
        double scaleX = targetWidth / nodeW;
        double scaleY = targetHeight / nodeH;
        double scale  = Math.min(scaleX, scaleY);

        // Centre the content and offset by the node's own origin
        double tx = (targetWidth  - nodeW * scale) / 2.0 - nodeX * scale;
        double ty = (targetHeight - nodeH * scale) / 2.0 - nodeY * scale;

        g2d.translate(tx, ty);
        g2d.scale(scale, scale);
        result.graphicsNode().paint(g2d);
        g2d.dispose();
        return img;
    }

    private static List<Geometry> extractGeometries(GraphicsNode node) {
        List<Geometry> result = new ArrayList<>();

        if (node instanceof org.apache.batik.gvt.CompositeGraphicsNode cgn) {
            // Skip invisible groups (display:none / visibility:hidden on a group)
            if (!node.isVisible()) return result;
            for (Object child : cgn.getChildren()) {
                if (child instanceof GraphicsNode childNode) {
                    result.addAll(extractGeometries(childNode));
                }
            }
        } else if (node instanceof org.apache.batik.gvt.ShapeNode) {
            // Only process basic shape nodes.
            // TextNode, ImageNode etc. are excluded because their "outlines" are
            // glyph bounding boxes / image rectangles – not laser-engraveable shapes.

            // 1. Skip nodes marked as invisible (visibility:hidden)
            if (!node.isVisible()) return result;

            // 2. Skip fully-transparent nodes (opacity:0 or similar)
            java.awt.Composite comp = node.getComposite();
            if (comp instanceof java.awt.AlphaComposite ac && ac.getAlpha() < 0.01f)
                return result;

            // 3. Extract the shape geometry
            java.awt.Shape outline = node.getOutline();
            if (outline == null) return result;
            Geometry geom = shapeToGeometry(outline, node.getGlobalTransform());
            if (geom != null && !geom.isEmpty()) {
                result.add(geom);
            }
        }
        return result;
    }

    private static Geometry shapeToGeometry(Shape shape, AffineTransform transform) {
        try {
            Shape transformed = transform != null ? transform.createTransformedShape(shape) : shape;
            PathIterator pi   = transformed.getPathIterator(null, 0.5);

            List<Geometry>   parts = new ArrayList<>();
            List<Coordinate> pts   = new ArrayList<>();
            double[]         buf   = new double[6];

            while (!pi.isDone()) {
                int type = pi.currentSegment(buf);
                switch (type) {
                    case PathIterator.SEG_MOVETO -> {
                        // Previous subpath was open → add as LineString (not a closed shape)
                        finishSubpath(parts, pts, false);
                        pts = new ArrayList<>();
                        pts.add(new Coordinate(buf[0], buf[1]));
                    }
                    case PathIterator.SEG_LINETO ->
                        pts.add(new Coordinate(buf[0], buf[1]));
                    case PathIterator.SEG_CLOSE -> {
                        // Explicit close → connect back to start and emit as Polygon
                        if (!pts.isEmpty()) {
                            Coordinate first = pts.get(0);
                            if (!pts.get(pts.size() - 1).equals2D(first))
                                pts.add(new Coordinate(first.x, first.y));
                            finishSubpath(parts, pts, true);
                            pts = new ArrayList<>();
                        }
                    }
                    default -> pts.add(new Coordinate(buf[0], buf[1]));
                }
                pi.next();
            }
            // Any remaining open subpath → LineString
            finishSubpath(parts, pts, false);

            if (parts.isEmpty()) return null;
            if (parts.size() == 1) return parts.get(0);
            return GF.createGeometryCollection(parts.toArray(new Geometry[0])).buffer(0);
        } catch (Exception e) {
            log.debug("Could not convert shape to geometry: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Appends one subpath to {@code parts}.
     * Only explicitly closed rings with enough points become Polygons.
     * Open paths (open traces, centerlines of stroked elements) are discarded –
     * they are not shape outlines and would clutter the laser outline pass.
     */
    private static void finishSubpath(List<Geometry> parts, List<Coordinate> pts, boolean closed) {
        if (!closed || pts.size() < 4) return;   // discard open paths and degenerate rings
        try {
            parts.add(GF.createPolygon(pts.toArray(new Coordinate[0])));
        } catch (Exception ignored) {
            // Degenerate ring (collinear / self-intersecting) → discard
        }
    }

    private static double parseDimension(String val, double defaultVal) {
        if (val == null || val.isEmpty()) return defaultVal;
        try {
            // Strip units (px, mm, pt, etc.)
            val = val.replaceAll("[^0-9.]", "");
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}

