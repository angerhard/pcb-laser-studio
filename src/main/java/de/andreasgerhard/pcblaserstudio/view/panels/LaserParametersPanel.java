package de.andreasgerhard.pcblaserstudio.view.panels;

import de.andreasgerhard.pcblaserstudio.model.LaserSettings;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Panel for laser engraving parameters with labeled sliders.
 * Fill-pass and contour-pass parameters are visually separated.
 */
public class LaserParametersPanel extends JPanel {

    private LaserSettings settings;
    private Consumer<LaserSettings> onChange;
    private Consumer<LaserSettings> onApplyOffsetToAll;

    // Radius: −1.0 – +1.0 mm  |  200 ticks, 0.005 mm/tick
    private static final int RADIUS_SCALE     = 200;
    private static final int RADIUS_SNAP_ZONE = 0;
    private final JSlider sldRadius;
    private final JLabel  lblRadiusVal;

    // Stepover: 0.01 – 1.0 mm (steps of 0.01 → 100 ticks)
    private final JSlider sldStepover;
    private final JLabel  lblStepoverVal;

    // Fill feed rate: 100 – 10000 mm/min (steps of 100)
    private final JSlider sldFeed;
    private final JLabel  lblFeedVal;

    // Fill power: 0 – 100 %
    private final JSlider sldPower;
    private final JLabel  lblPowerVal;

    // Contour feed rate: 100 – 10000 mm/min
    private final JSlider sldContourFeed;
    private final JLabel  lblContourFeedVal;

    // Contour power: 0 – 100 %
    private final JSlider sldContourPower;
    private final JLabel  lblContourPowerVal;

    // Offset: lower-left corner in mm
    private final JSpinner spnOffsetX;
    private final JSpinner spnOffsetY;

    // Contour pass toggle
    private final JCheckBox chkContour;

    private boolean updating = false;

    public LaserParametersPanel() {
        // Column: label | slider (grows) | value (right-aligned)
        setLayout(new MigLayout("fillx, insets 8 8 8 8, gapy 3", "[90][grow][76]", ""));
        setBorder(DarkTheme.createSectionBorder("Laser Parameters"));
        setBackground(DarkTheme.BG_DARK);

        // ═══════════════ FILL PASS ═══════════════
        add(createSubHeader("Fill Pass"), "span 3, growx, wrap 4");

        // Spot Radius
        add(DarkTheme.createLabel("Spot Radius:"), "aligny center");
        sldRadius = createSlider(-RADIUS_SCALE, RADIUS_SCALE, (int)(0.20 * RADIUS_SCALE));
        sldRadius.setToolTipText(
                "<html><b>Laser spot radius compensation</b> (−1.0 … +1.0 mm, 0.005 mm steps)<br>" +
                "<b>Positive:</b> erosion – keeps distance from boundary<br>" +
                "<b>Zero:</b> no compensation (snaps automatically)<br>" +
                "<b>Negative:</b> dilation – expands fine features</html>");
        lblRadiusVal = createValueLabel(radiusLabel(0.20));
        add(sldRadius, "growx, aligny center");
        add(lblRadiusVal, "wrap, aligny center");

        // Stepover
        add(DarkTheme.createLabel("Stepover:"), "aligny center");
        sldStepover = createSlider(1, 100, 2);
        sldStepover.setToolTipText("Line spacing between passes (0.01 – 1.0 mm)");
        lblStepoverVal = createValueLabel("0.10 mm");
        add(sldStepover, "growx, aligny center");
        add(lblStepoverVal, "wrap, aligny center");

        // Fill Feed Rate
        add(DarkTheme.createLabel("Feed Rate:"), "aligny center");
        sldFeed = createSlider(1, 100, 30);
        sldFeed.setToolTipText("Fill pass movement speed (100 – 10000 mm/min)");
        lblFeedVal = createValueLabel("3000 mm/min");
        add(sldFeed, "growx, aligny center");
        add(lblFeedVal, "wrap, aligny center");

        // Fill Power
        add(DarkTheme.createLabel("Power:"), "aligny center");
        sldPower = createSlider(0, 100, 75);
        sldPower.setToolTipText("Fill pass laser power level (0 – 100 %)");
        lblPowerVal = createValueLabel("75 %");
        add(sldPower, "growx, aligny center");
        add(lblPowerVal, "wrap 10, aligny center");

        // ═══════════════ CONTOUR PASS ═══════════════
        add(createSubHeader("Contour Pass"), "span 3, growx, wrap 4");

        chkContour = new JCheckBox("Generate Contour Pass", true);
        chkContour.setFont(DarkTheme.FONT_NORMAL);
        chkContour.setForeground(DarkTheme.TEXT_PRIMARY);
        chkContour.setBackground(DarkTheme.BG_DARK);
        chkContour.setToolTipText(
                "<html><b>Contour pass</b><br>" +
                "Traces the exact outline before the raster fill.<br>" +
                "Uses its own feed rate and power settings below.</html>");
        add(chkContour, "span 3, wrap 4");

        // Contour Feed Rate
        add(DarkTheme.createLabel("Feed Rate:"), "aligny center");
        sldContourFeed = createSlider(1, 100, 30);
        sldContourFeed.setToolTipText("Contour pass movement speed (100 – 10000 mm/min)");
        lblContourFeedVal = createValueLabel("3000 mm/min");
        add(sldContourFeed, "growx, aligny center");
        add(lblContourFeedVal, "wrap, aligny center");

        // Contour Power
        add(DarkTheme.createLabel("Power:"), "aligny center");
        sldContourPower = createSlider(0, 100, 75);
        sldContourPower.setToolTipText("Contour pass laser power level (0 – 100 %)");
        lblContourPowerVal = createValueLabel("75 %");
        add(sldContourPower, "growx, aligny center");
        add(lblContourPowerVal, "wrap 10, aligny center");

        // ═══════════════ POSITION ═══════════════
        add(createSubHeader("Position"), "span 3, growx, wrap 4");

        spnOffsetX = createOffsetSpinner(0.0);
        spnOffsetX.setToolTipText("X offset of the lower-left corner in mm");
        spnOffsetY = createOffsetSpinner(0.0);
        spnOffsetY.setToolTipText("Y offset of the lower-left corner in mm");

        JPanel offsetRow = new JPanel(new MigLayout("insets 0, fillx, gapx 4", "[24][grow][28][grow]", ""));
        offsetRow.setBackground(DarkTheme.BG_DARK);
        offsetRow.add(DarkTheme.createDimLabel("X:"));
        offsetRow.add(spnOffsetX, "growx");
        offsetRow.add(DarkTheme.createDimLabel("Y:"), "gapleft 6");
        offsetRow.add(spnOffsetY, "growx");
        add(offsetRow, "span 3, growx, wrap 2");

        JButton btnApplyOffset = DarkTheme.createButton("Apply Offset to All Layers",
                "Copy X/Y offset to all other layers");
        add(btnApplyOffset, "span 3, growx, h 22!, wrap");

        // ─── Snap-to-zero on mouse release ───────────────────────────────────
        sldRadius.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                if (Math.abs(sldRadius.getValue()) <= RADIUS_SNAP_ZONE) sldRadius.setValue(0);
            }
        });

        // ─── Change listeners ─────────────────────────────────────────────────
        sldRadius.addChangeListener(e -> {
            if (updating) return;
            double val = sldRadius.getValue() / (double) RADIUS_SCALE;
            lblRadiusVal.setText(radiusLabel(val));
            if (settings != null) { settings.setRadiusMm(val); notifyChange(); }
        });
        sldStepover.addChangeListener(e -> {
            if (updating) return;
            double val = sldStepover.getValue() / 100.0;
            lblStepoverVal.setText(String.format("%.2f mm", val));
            if (settings != null) { settings.setStepoverMm(val); notifyChange(); }
        });
        sldFeed.addChangeListener(e -> {
            if (updating) return;
            int val = sldFeed.getValue() * 100;
            lblFeedVal.setText(val + " mm/min");
            if (settings != null) { settings.setFeedRateMmPerMin(val); notifyChange(); }
        });
        sldPower.addChangeListener(e -> {
            if (updating) return;
            int val = sldPower.getValue();
            lblPowerVal.setText(val + " %");
            if (settings != null) { settings.setPowerPercent(val); notifyChange(); }
        });
        sldContourFeed.addChangeListener(e -> {
            if (updating) return;
            int val = sldContourFeed.getValue() * 100;
            lblContourFeedVal.setText(val + " mm/min");
            if (settings != null) { settings.setContourFeedRateMmPerMin(val); notifyChange(); }
        });
        sldContourPower.addChangeListener(e -> {
            if (updating) return;
            int val = sldContourPower.getValue();
            lblContourPowerVal.setText(val + " %");
            if (settings != null) { settings.setContourPowerPercent(val); notifyChange(); }
        });
        spnOffsetX.addChangeListener(e -> {
            if (updating || settings == null) return;
            settings.setOffsetXMm((Double) spnOffsetX.getValue());
            notifyChange();
        });
        spnOffsetY.addChangeListener(e -> {
            if (updating || settings == null) return;
            settings.setOffsetYMm((Double) spnOffsetY.getValue());
            notifyChange();
        });
        btnApplyOffset.addActionListener(e -> {
            if (settings == null) return;
            if (onApplyOffsetToAll != null) onApplyOffsetToAll.accept(settings);
        });
        chkContour.addActionListener(e -> {
            if (updating || settings == null) return;
            settings.setGenerateContour(chkContour.isSelected());
            notifyChange();
        });

        // Keep the preferred width inside the scroll viewport so the right edge
        // (value labels, buttons) is never clipped, even in a narrow tool panel.
        Dimension pref = getPreferredSize();
        setPreferredSize(new Dimension(280, pref.height));
    }

    /** Small accent-coloured sub-header with a trailing separator line. */
    private static JPanel createSubHeader(String title) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setBackground(DarkTheme.BG_DARK);
        JLabel lbl = new JLabel(title);
        lbl.setFont(DarkTheme.FONT_BOLD);
        lbl.setForeground(DarkTheme.ACCENT);
        JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        sep.setForeground(DarkTheme.BG_MEDIUM);
        p.add(lbl, BorderLayout.WEST);
        p.add(sep, BorderLayout.CENTER);
        return p;
    }

    /** Formats the radius value with sign. The sign (＋/−) already conveys the action. */
    private static String radiusLabel(double mm) {
        if (mm == 0.0) return " 0.000 mm";
        if (mm < 0)    return String.format("%.3f mm", mm);
        return             String.format("+%.3f mm", mm);
    }

    /** Right-aligned, small value label that never clips its column. */
    private static JLabel createValueLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(DarkTheme.FONT_SMALL);
        lbl.setForeground(DarkTheme.TEXT_PRIMARY);
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);
        return lbl;
    }

    public void setSettings(LaserSettings s) {
        this.settings = null;
        updating = true;
        try {
            if (s != null) {
                sldRadius.setValue((int) Math.round(s.getRadiusMm() * RADIUS_SCALE));
                lblRadiusVal.setText(radiusLabel(s.getRadiusMm()));

                sldStepover.setValue((int) Math.round(s.getStepoverMm() * 100));
                lblStepoverVal.setText(String.format("%.2f mm", s.getStepoverMm()));

                sldFeed.setValue((int) Math.round(s.getFeedRateMmPerMin() / 100));
                lblFeedVal.setText((int) s.getFeedRateMmPerMin() + " mm/min");

                sldPower.setValue((int) Math.round(s.getPowerPercent()));
                lblPowerVal.setText((int) s.getPowerPercent() + " %");

                sldContourFeed.setValue((int) Math.round(s.getContourFeedRateMmPerMin() / 100));
                lblContourFeedVal.setText((int) s.getContourFeedRateMmPerMin() + " mm/min");

                sldContourPower.setValue((int) Math.round(s.getContourPowerPercent()));
                lblContourPowerVal.setText((int) s.getContourPowerPercent() + " %");

                spnOffsetX.setValue(s.getOffsetXMm());
                spnOffsetY.setValue(s.getOffsetYMm());

                chkContour.setSelected(s.isGenerateContour());
            }
        } finally {
            updating = false;
            this.settings = s;
        }
    }

    public void setOnChange(Consumer<LaserSettings> cb) { this.onChange = cb; }
    public void setOnApplyOffsetToAll(Consumer<LaserSettings> cb) { this.onApplyOffsetToAll = cb; }

    private void notifyChange() { if (onChange != null) onChange.accept(settings); }

    private static JSlider createSlider(int min, int max, int val) {
        JSlider s = new JSlider(min, max, val);
        s.setBackground(DarkTheme.BG_DARK);
        s.setForeground(DarkTheme.TEXT_DIM);
        s.setPaintTicks(false);
        s.setPaintLabels(false);
        s.setBorder(BorderFactory.createEmptyBorder());
        // Keep the panel's preferred width inside the scroll viewport; the [grow]
        // column stretches the slider to fill the actual available width anyway.
        s.setPreferredSize(new Dimension(80, s.getPreferredSize().height));
        return s;
    }

    private static JSpinner createOffsetSpinner(double initial) {
        SpinnerNumberModel model = new SpinnerNumberModel(initial, -9999.0, 9999.0, 0.1);
        JSpinner spinner = new JSpinner(model);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0.00");
        spinner.setEditor(editor);
        spinner.setBackground(DarkTheme.BG_DARKEST);
        spinner.setForeground(DarkTheme.TEXT_PRIMARY);
        editor.getTextField().setBackground(DarkTheme.BG_DARKEST);
        editor.getTextField().setForeground(DarkTheme.TEXT_PRIMARY);
        editor.getTextField().setCaretColor(DarkTheme.TEXT_PRIMARY);
        return spinner;
    }
}
