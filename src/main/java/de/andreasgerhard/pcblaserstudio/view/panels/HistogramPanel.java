package de.andreasgerhard.pcblaserstudio.view.panels;

import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Histogram panel showing laser power distribution across all generated points.
 */
public class HistogramPanel extends JPanel {

    private int[] histogram = null;
    private int totalPoints = 0;
    private int maxBin = 0;
    private int peakPower = 0;

    public HistogramPanel() {
        setLayout(new MigLayout("fillx, insets 8 8 8 8", "[grow]", "[grow][][]"));
        setBorder(DarkTheme.createSectionBorder("Laser Point Distribution"));
        setBackground(DarkTheme.BG_DARK);
    }

    public void setHistogram(int[] histogram) {
        this.histogram = histogram;
        totalPoints = 0;
        maxBin = 0;
        peakPower = 0;
        if (histogram != null) {
            for (int i = 0; i < histogram.length; i++) {
                totalPoints += histogram[i];
                if (histogram[i] > maxBin) {
                    maxBin = histogram[i];
                    peakPower = i;
                }
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth() - 20;
        int h = getHeight() - 60;
        int xOff = 10;
        int yOff = 20;

        if (h < 20 || w < 20) { g2.dispose(); return; }

        // Background
        g2.setColor(DarkTheme.BG_DARKEST);
        g2.fillRect(xOff, yOff, w, h);
        g2.setColor(DarkTheme.BG_MEDIUM);
        g2.drawRect(xOff, yOff, w, h);

        if (histogram == null || maxBin == 0) {
            g2.setColor(DarkTheme.TEXT_DIM);
            g2.setFont(DarkTheme.FONT_SMALL);
            String msg = "No data – generate GCODE first";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, xOff + (w - fm.stringWidth(msg)) / 2, yOff + h / 2);
            g2.dispose();
            return;
        }

        // Draw bars
        int bins = histogram.length; // 101
        double barW = (double) w / bins;
        for (int i = 0; i < bins; i++) {
            if (histogram[i] == 0) continue;
            double ratio = (double) histogram[i] / maxBin;
            int barH = (int) (h * ratio);
            int bx = xOff + (int)(i * barW);
            int by = yOff + h - barH;

            // Color gradient: cyan (low power) → amber (high power)
            float hue = 0.49f - (float) (i / 100.0 * 0.41f);
            Color barColor = Color.getHSBColor(hue, 0.65f, 0.88f);
            g2.setColor(barColor);
            g2.fillRect(bx, by, Math.max(1, (int) barW - 1), barH);
        }

        // Peak line
        if (peakPower > 0) {
            int px = xOff + (int)(peakPower * barW);
            g2.setColor(DarkTheme.ACCENT);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    1f, new float[]{3f, 2f}, 0f));
            g2.drawLine(px, yOff, px, yOff + h);
        }

        // Axis labels
        g2.setFont(DarkTheme.FONT_SMALL);
        g2.setColor(DarkTheme.TEXT_DIM);
        g2.setStroke(new BasicStroke(1f));
        g2.drawString("0%", xOff, yOff + h + 14);
        g2.drawString("50%", xOff + w / 2 - 10, yOff + h + 14);
        g2.drawString("100%", xOff + w - 25, yOff + h + 14);

        // Stats line
        g2.setColor(DarkTheme.TEXT_DIM);
        g2.setFont(DarkTheme.FONT_SMALL);
        String stats = String.format("Points: %,d  |  Peak at: %d%%", totalPoints, peakPower);
        g2.drawString(stats, xOff, yOff + h + 28);

        g2.dispose();
    }
}

