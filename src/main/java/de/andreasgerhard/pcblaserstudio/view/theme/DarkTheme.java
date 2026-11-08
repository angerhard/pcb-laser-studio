package de.andreasgerhard.pcblaserstudio.view.theme;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * "Graphite &amp; Cyan" color palette and small Swing component factories.
 *
 * <p>Component chrome (sliders, combo boxes, scrollbars, tabs, ...) is themed
 * globally by {@link PcbLaserTheme} / {@code PcbLaserTheme.properties}; the
 * constants here are used by custom-painted views ({@code SvgCanvas},
 * {@code GCodeCanvas}, {@code HistogramPanel}, ...) and must be kept in sync
 * with the base colors defined in that properties file.</p>
 */
public class DarkTheme {

    public static final Color BG_DARKEST   = new Color(0x1a, 0x1a, 0x1a);
    public static final Color BG_DARK      = new Color(0x1e, 0x1e, 0x1e);
    public static final Color BG_MEDIUM    = new Color(0x26, 0x26, 0x26);
    public static final Color BG_LIGHT     = new Color(0x33, 0x33, 0x33);

    public static final Color TEXT_PRIMARY = new Color(0xf2, 0xf2, 0xf2);
    public static final Color TEXT_DIM     = new Color(0x9a, 0x9a, 0x9a);
    public static final Color TEXT_DISABLED= new Color(0x5c, 0x5c, 0x5c);

    public static final Color ACCENT         = new Color(0x2d, 0xd4, 0xbf);
    public static final Color ACCENT_HOVER   = new Color(0x52, 0xe5, 0xd1);
    public static final Color ACCENT_PRESSED = new Color(0x20, 0xa3, 0x93);

    public static final Color DANGER   = new Color(0xe5, 0x48, 0x4d);
    public static final Color SUCCESS  = new Color(0x4a, 0xde, 0x80);
    public static final Color WARNING  = new Color(0xf5, 0xa5, 0x24);

    // Laser path colors
    public static final Color COLOR_OUTLINE = new Color(0xff, 0x44, 0x44, 0xe0);
    public static final Color COLOR_FILL    = new Color(0x44, 0xa8, 0xff, 0xe0);
    public static final Color COLOR_BORDER  = new Color(0xf5, 0xa5, 0x24, 0xe0);

    public static final Font FONT_SMALL  = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_NORMAL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_BOLD   = new Font("SansSerif", Font.BOLD, 12);
    public static final Font FONT_TITLE  = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_MONO   = new Font("Monospaced", Font.PLAIN, 11);

    public static Border createSectionBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BG_MEDIUM, 1),
                title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                FONT_TITLE,
                ACCENT
        );
        return border;
    }

    /** Small, flat, rounded-rect button (default style). */
    public static JButton createButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_SMALL);
        btn.setToolTipText(tooltip);
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        return btn;
    }

    /** Small, flat, accent-colored button for primary actions. */
    public static JButton createAccentButton(String text, String tooltip) {
        JButton btn = createButton(text, tooltip);
        btn.putClientProperty(FlatClientProperties.STYLE,
                "background: " + toHex(ACCENT) + "; foreground: #111111;" +
                "hoverBackground: " + toHex(ACCENT_HOVER) + "; pressedBackground: " + toHex(ACCENT_PRESSED) + ";" +
                "focusedBackground: " + toHex(ACCENT) + ";");
        return btn;
    }

    /**
     * Compact, icon-only, borderless button (toolbar style): only shows a
     * subtle hover/pressed highlight, no visible border or background by
     * default. Used for zoom controls, visibility toggles, etc.
     */
    public static JButton createIconButton(Icon icon, String tooltip) {
        JButton btn = new JButton(icon);
        btn.setToolTipText(tooltip);
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 4; margin: 3,3,3,3");
        btn.setFocusable(false);
        return btn;
    }

    /** Icon-only toggle button (e.g. show/hide SVG, outlines, fill). */
    public static JToggleButton createIconToggleButton(Icon icon, String tooltip, boolean selected) {
        JToggleButton btn = new JToggleButton(icon, selected);
        btn.setToolTipText(tooltip);
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        btn.putClientProperty(FlatClientProperties.STYLE,
                "arc: 4; margin: 3,3,3,3; selectedBackground: " + toHex(ACCENT) + "33;");
        btn.setFocusable(false);
        return btn;
    }

    public static JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(TEXT_PRIMARY);
        lbl.setFont(FONT_NORMAL);
        return lbl;
    }

    public static JLabel createDimLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(TEXT_DIM);
        lbl.setFont(FONT_SMALL);
        return lbl;
    }

    public static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
