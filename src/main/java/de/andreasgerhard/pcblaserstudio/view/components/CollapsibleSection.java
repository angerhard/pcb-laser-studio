package de.andreasgerhard.pcblaserstudio.view.components;

import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import de.andreasgerhard.pcblaserstudio.view.theme.Icons;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Lightroom-style collapsible section: a small flat header (chevron + title)
 * that expands/collapses a content panel. Several of these stacked in a
 * vertical column replace a tabbed sub-navigation, so every setting group is
 * reachable at a glance without switching tabs.
 */
public class CollapsibleSection extends JPanel {

    private final JPanel content;
    private final JLabel chevron;
    private boolean expanded;

    public CollapsibleSection(String title, JComponent content) {
        this(title, content, true);
    }

    public CollapsibleSection(String title, JComponent content, boolean startExpanded) {
        super(new BorderLayout());
        setOpaque(false);
        this.expanded = startExpanded;

        JPanel header = new JPanel(new MigLayout("insets 6 8 6 8, fillx, gapx 6", "[pref][grow]", ""));
        header.setBackground(DarkTheme.BG_MEDIUM);
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        chevron = new JLabel(Icons.of(expanded ? Icons.CHEVRON_DOWN : Icons.CHEVRON_RIGHT, 12));
        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(DarkTheme.FONT_BOLD);
        lblTitle.setForeground(DarkTheme.TEXT_DIM);

        header.add(chevron, "aligny center");
        header.add(lblTitle, "aligny center, growx");

        header.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggle(); }
        });

        this.content = new JPanel(new BorderLayout());
        this.content.setOpaque(false);
        this.content.add(content, BorderLayout.CENTER);
        this.content.setVisible(expanded);

        add(header, BorderLayout.NORTH);
        add(this.content, BorderLayout.CENTER);
    }

    public void toggle() { setExpanded(!expanded); }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        content.setVisible(expanded);
        chevron.setIcon(Icons.of(expanded ? Icons.CHEVRON_DOWN : Icons.CHEVRON_RIGHT, 12));
        revalidate();
        repaint();
    }

    public boolean isExpanded() { return expanded; }
}
