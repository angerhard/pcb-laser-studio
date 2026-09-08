package de.andreasgerhard.pcblaserstudio.view.theme;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class DarkTheme {

    public static final Color BG_DARKEST   = new Color(0x1a, 0x1a, 0x1a);
    public static final Color BG_DARK      = new Color(23, 23, 23);
    public static final Color BG_MEDIUM    = new Color(41, 41, 41);
    public static final Color BG_LIGHT     = new Color(69, 69, 69);
    public static final Color TEXT_PRIMARY = new Color(0xff, 0xff, 0xff);
    public static final Color TEXT_DIM     = new Color(0xe0, 0xe0, 0xe0);
    public static final Color ACCENT       = new Color(244, 200, 255);
    public static final Color ACCENT_HOVER = new Color(31, 128, 255);
    public static final Color DANGER       = new Color(0xd9, 0x5b, 0x5b);
    public static final Color SUCCESS      = new Color(0x5b, 0xd9, 0x7b);

    // Laser path colors
    public static final Color COLOR_OUTLINE = new Color(0xff, 0x44, 0x44, 0xe0);
    public static final Color COLOR_FILL    = new Color(0x44, 0x88, 0xff, 0xe0);

    public static final Font FONT_SMALL  = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_NORMAL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_BOLD   = new Font("SansSerif", Font.BOLD, 12);
    public static final Font FONT_TITLE  = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_MONO   = new Font("Monospaced", Font.PLAIN, 11);

    public static void apply() {
        UIManager.put("Panel.background",                  BG_DARK);
        UIManager.put("Panel.foreground",                  TEXT_PRIMARY);
        UIManager.put("Label.foreground",                  TEXT_PRIMARY);
        UIManager.put("Label.background",                  BG_DARK);

        UIManager.put("Button.background",                 BG_MEDIUM);
        UIManager.put("Button.foreground",                 TEXT_PRIMARY);
        UIManager.put("Button.select",                     BG_LIGHT);
        UIManager.put("Button.focus",                      ACCENT);
        UIManager.put("Button.border",                     BorderFactory.createLineBorder(BG_LIGHT, 1));

        UIManager.put("TextField.background",              BG_MEDIUM);
        UIManager.put("TextField.foreground",              TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground",         TEXT_PRIMARY);
        UIManager.put("TextField.border",                  BorderFactory.createLineBorder(BG_LIGHT, 1));
        UIManager.put("TextField.selectionBackground",     ACCENT);

        UIManager.put("TextArea.background",               BG_DARKEST);
        UIManager.put("TextArea.foreground",               TEXT_PRIMARY);
        UIManager.put("TextArea.caretForeground",          TEXT_PRIMARY);
        UIManager.put("TextArea.selectionBackground",      ACCENT);

        UIManager.put("ComboBox.background",               BG_MEDIUM);
        UIManager.put("ComboBox.foreground",               TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground",      ACCENT);
        UIManager.put("ComboBox.selectionForeground",      Color.WHITE);
        UIManager.put("ComboBox.border",                   BorderFactory.createLineBorder(BG_LIGHT, 1));

        UIManager.put("CheckBox.background",               BG_DARK);
        UIManager.put("CheckBox.foreground",               TEXT_PRIMARY);

        UIManager.put("Slider.background",                 BG_DARK);
        UIManager.put("Slider.foreground",                 TEXT_PRIMARY);
        UIManager.put("Slider.thumbColor",                 ACCENT);

        UIManager.put("ScrollPane.background",             BG_DARK);
        UIManager.put("ScrollPane.border",                 BorderFactory.createEmptyBorder());
        UIManager.put("ScrollBar.background",              BG_DARK);
        UIManager.put("ScrollBar.thumb",                   BG_MEDIUM);
        UIManager.put("ScrollBar.thumbShadow",             BG_DARK);
        UIManager.put("ScrollBar.thumbHighlight",          BG_LIGHT);
        UIManager.put("ScrollBar.track",                   BG_DARK);
        UIManager.put("ScrollBar.width",                   8);

        UIManager.put("TabbedPane.background",             BG_DARKEST);
        UIManager.put("TabbedPane.foreground",             TEXT_PRIMARY);
        UIManager.put("TabbedPane.selected",               BG_DARK);
        UIManager.put("TabbedPane.selectedForeground",     TEXT_PRIMARY);
        UIManager.put("TabbedPane.tabAreaBackground",      BG_DARKEST);
        UIManager.put("TabbedPane.contentBorderInsets",    new Insets(0, 0, 0, 0));
        UIManager.put("TabbedPane.borderHightlightColor",  BG_MEDIUM);
        UIManager.put("TabbedPane.darkShadow",             BG_DARKEST);
        UIManager.put("TabbedPane.shadow",                 BG_DARKEST);
        UIManager.put("TabbedPane.light",                  BG_MEDIUM);
        UIManager.put("TabbedPane.focus",                  ACCENT);

        UIManager.put("SplitPane.background",              BG_DARKEST);
        UIManager.put("SplitPane.dividerSize",             5);
        UIManager.put("SplitPaneDivider.background",       BG_MEDIUM);
        UIManager.put("SplitPaneDivider.draggingColor",    ACCENT);

        UIManager.put("PopupMenu.background",              BG_MEDIUM);
        UIManager.put("PopupMenu.foreground",              TEXT_PRIMARY);
        UIManager.put("PopupMenu.border",                  BorderFactory.createLineBorder(BG_LIGHT, 1));
        UIManager.put("MenuItem.background",               BG_MEDIUM);
        UIManager.put("MenuItem.foreground",               TEXT_PRIMARY);
        UIManager.put("MenuItem.selectionBackground",      ACCENT);
        UIManager.put("MenuItem.selectionForeground",      Color.WHITE);
        UIManager.put("MenuBar.background",                BG_DARKEST);
        UIManager.put("MenuBar.foreground",                TEXT_PRIMARY);
        UIManager.put("Menu.background",                   BG_DARKEST);
        UIManager.put("Menu.foreground",                   TEXT_PRIMARY);
        UIManager.put("Menu.selectionBackground",          BG_MEDIUM);
        UIManager.put("Menu.selectionForeground",          TEXT_PRIMARY);

        UIManager.put("ToolTip.background",                BG_LIGHT);
        UIManager.put("ToolTip.foreground",                TEXT_PRIMARY);
        UIManager.put("ToolTip.border",                    BorderFactory.createLineBorder(ACCENT, 1));

        UIManager.put("ProgressBar.background",            BG_MEDIUM);
        UIManager.put("ProgressBar.foreground",            ACCENT);
        UIManager.put("ProgressBar.border",                BorderFactory.createLineBorder(BG_LIGHT, 1));

        UIManager.put("FileChooser.background",            BG_DARK);
        UIManager.put("OptionPane.background",             BG_DARK);
        UIManager.put("OptionPane.foreground",             TEXT_PRIMARY);
        UIManager.put("OptionPane.messageForeground",      TEXT_PRIMARY);
    }

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

    /** Corner radius for the compact, rounded buttons. */
    private static final int BTN_RADIUS = 8;

    public static JButton createButton(String text, String tooltip) {
        return createButton(text, tooltip, BG_MEDIUM, TEXT_PRIMARY, BG_LIGHT, BG_MEDIUM);
    }

    public static JButton createAccentButton(String text, String tooltip) {
        return createButton(text, tooltip, ACCENT, Color.WHITE, ACCENT_HOVER, ACCENT);
    }

    /** Compact, rounded button factory shared by the create*Button helpers. */
    private static JButton createButton(String text, String tooltip,
                                        Color bg, Color fg, Color hoverBg, Color normalBg) {
        JButton btn = new RoundedButton(text, BTN_RADIUS, BG_LIGHT);
        btn.setFont(FONT_SMALL);
        btn.setToolTipText(tooltip);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(hoverBg);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(normalBg);
            }
        });
        return btn;
    }

    /**
     * Compact button that paints a rounded background in {@code paintComponent}
     * (i.e. <em>before</em> the label text is drawn), so the text stays visible.
     */
    private static final class RoundedButton extends JButton {
        private final int radius;
        private final Color outline;

        RoundedButton(String text, int radius, Color outline) {
            super(text);
            this.radius = radius;
            this.outline = outline;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.setColor(outline);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
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
}

