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
import java.awt.geom.Dimension2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            /** Declared document viewport width in Batik device pixels. */
            double svgWidth,
            /** Declared document viewport height in Batik device pixels. */
            double svgHeight,
            List<Geometry> geometries
    ) {}

    /**
     * Declared SVG document canvas (the root {@code <svg>} viewport), in both
     * Batik device pixels and physical millimetres. This is the single source
     * of truth used by {@link de.andreasgerhard.pcblaserstudio.service.GCodeGenerator}
     * and the board-alignment logic, so every consumer agrees on the exact
     * physical size and origin of the SVG canvas.
     */
    public record DocumentBounds(double widthPx, double heightPx, double widthMm, double heightMm) {}

    /**
     * Returns the declared document canvas of a parsed SVG. This is based on
     * the root {@code <svg>} width/height (or, if absent, the viewBox/content
     * bounds), not on the artwork's ink bounding box.
     */
    public static DocumentBounds getDocumentBounds(SvgParseResult result) {
        double w = result.svgWidth();
        double h = result.svgHeight();
        if (!isPositiveFinite(w) || !isPositiveFinite(h)) {
            Rectangle2D bounds = result.graphicsNode().getBounds();
            if (bounds != null && isPositiveFinite(bounds.getWidth())
                    && isPositiveFinite(bounds.getHeight())) {
                w = bounds.getWidth();
                h = bounds.getHeight();
            }
        }
        if (!isPositiveFinite(w) || !isPositiveFinite(h)) {
            w = 800;
            h = 600;
        }
        return new DocumentBounds(w, h, w * MM_PER_BATIK_PX, h * MM_PER_BATIK_PX);
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

        org.w3c.dom.Element svgEl = doc.getDocumentElement();
        double attrWidth  = parseLength(svgEl.getAttribute("width"),  Double.NaN);
        double attrHeight = parseLength(svgEl.getAttribute("height"), Double.NaN);

        // viewBox fallback dimensions (SVG user units → device px).
        double vbWidth = Double.NaN, vbHeight = Double.NaN;
        String viewBox = svgEl.getAttribute("viewBox");
        if (viewBox != null && !viewBox.isBlank()) {
            String[] vb = viewBox.trim().split("[\\s,]+");
            if (vb.length == 4) {
                vbWidth  = parseNumber(vb[2], Double.NaN);
                vbHeight = parseNumber(vb[3], Double.NaN);
            }
        }

        // The declared document viewport is the physical canvas size. Batik has
        // already converted physical units (mm, cm, in, pt …) to device pixels.
        Dimension2D docSize = ctx.getDocumentSize();
        double docWidth  = docSize != null ? docSize.getWidth()  : Double.NaN;
        double docHeight = docSize != null ? docSize.getHeight() : Double.NaN;

        double width, height;
        if (isPositiveFinite(attrWidth) && isPositiveFinite(attrHeight)) {
            // Both root dimensions are absolute → trust Batik's converted viewport.
            width  = isPositiveFinite(docWidth)  ? docWidth  : attrWidth;
            height = isPositiveFinite(docHeight) ? docHeight : attrHeight;
        } else if (isPositiveFinite(vbWidth) && isPositiveFinite(vbHeight)) {
            // Missing/percentage dimensions → the viewBox defines the canvas.
            width  = vbWidth;
            height = vbHeight;
        } else if (isPositiveFinite(docWidth) && isPositiveFinite(docHeight)
                && docWidth > 1.0 && docHeight > 1.0) {
            width  = docWidth;
            height = docHeight;
        } else {
            width  = Double.NaN;
            height = Double.NaN;
        }

        // Fill any remaining gaps from individual attributes / viewBox / artwork.
        if (!isPositiveFinite(width))  width  = attrWidth;
        if (!isPositiveFinite(height)) height = attrHeight;
        if (!isPositiveFinite(width) && isPositiveFinite(vbWidth))   width  = vbWidth;
        if (!isPositiveFinite(height) && isPositiveFinite(vbHeight)) height = vbHeight;
        if (!isPositiveFinite(width) || !isPositiveFinite(height)) {
            Rectangle2D contentBounds = rootNode.getBounds();
            if (contentBounds != null) {
                if (!isPositiveFinite(width) && isPositiveFinite(contentBounds.getWidth())) {
                    width = contentBounds.getWidth();
                }
                if (!isPositiveFinite(height) && isPositiveFinite(contentBounds.getHeight())) {
                    height = contentBounds.getHeight();
                }
            }
        }
        if (!isPositiveFinite(width))  width = 800;
        if (!isPositiveFinite(height)) height = 600;

        List<Geometry> geometries = extractGeometries(rootNode);
        return new SvgParseResult(rootNode, doc, width, height, geometries);
    }

    /**
     * Render the parsed SVG document canvas to a BufferedImage.
     *
     * <p>The full root {@code <svg>} viewport is rendered, preserving each
     * element's position inside the canvas. Content outside the viewport is
     * clipped. This keeps the physical size and origin identical across SVGs
     * that declare the same width/height, even when their artwork bounds differ.</p>
     */
    public static BufferedImage renderToImage(SvgParseResult result, int targetWidth, int targetHeight) {
        BufferedImage img = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        // Leave background transparent (TYPE_INT_ARGB) – the canvas draws its own background.

        DocumentBounds bounds = getDocumentBounds(result);
        double scaleX = targetWidth  / bounds.widthPx();
        double scaleY = targetHeight / bounds.heightPx();

        g2d.scale(scaleX, scaleY);
        g2d.clip(new Rectangle2D.Double(0, 0, bounds.widthPx(), bounds.heightPx()));
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

    private static final Pattern LENGTH_PATTERN = Pattern.compile(
            "^([-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?)\\s*([a-zA-Z%]*)$");

    /**
     * Parses an SVG length into Batik device pixels (1 px = 1/96 inch).
     * Percentage/em/ex units cannot be resolved here and fall back to
     * {@code defaultVal}.
     */
    private static double parseLength(String val, double defaultVal) {
        if (val == null || val.isBlank()) return defaultVal;
        Matcher m = LENGTH_PATTERN.matcher(val.trim().toLowerCase(Locale.ROOT));
        if (!m.matches()) return defaultVal;
        double number = parseNumber(m.group(1), Double.NaN);
        if (!isPositiveFinite(number)) return defaultVal;
        return switch (m.group(2)) {
            case "", "px" -> number;
            case "mm" -> number * 96.0 / 25.4;
            case "cm" -> number * 96.0 / 2.54;
            case "in" -> number * 96.0;
            case "pt" -> number * 96.0 / 72.0;
            case "pc" -> number * 16.0;
            default -> defaultVal;
        };
    }

    private static double parseNumber(String val, double defaultVal) {
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static boolean isPositiveFinite(double value) {
        return Double.isFinite(value) && value > 0;
    }
}

