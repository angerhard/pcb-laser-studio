package de.andreasgerhard.pcblaserstudio.view.panels;

import de.andreasgerhard.pcblaserstudio.model.LaserSettings;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/**
 * Panel for laser engraving parameters with compact, Lightroom-style sliders.
 * Each slider has a trailing, click-to-edit value field (type an exact value
 * and press Enter) instead of a passive read-only label.
 * Fill-pass and contour-pass parameters are visually separated.
 */
public class LaserParametersPanel extends JPanel {

    private LaserSettings settings;
    private Consumer<LaserSettings> onChange;
    private Consumer<LaserSettings> onApplyOffsetToAll;

    // Radius X/Y: 0 – 0.1 mm  |  100 ticks, 0.001 mm/tick
    private static final double RADIUS_TICK_MM = 0.001;
    private static final int    RADIUS_TICKS   = 100;
    private final JSlider sldRadiusX;
    private final JTextField txtRadiusXVal;
    private final JSlider sldRadiusY;
    private final JTextField txtRadiusYVal;

    // Stepover: 0.01 – 1.0 mm (steps of 0.01 → 100 ticks)
    private final JSlider sldStepover;
    private final JTextField txtStepoverVal;

    // Fill feed rate: 100 – 10000 mm/min (steps of 100)
    private final JSlider sldFeed;
    private final JTextField txtFeedVal;

    // Fill power: 0 – 100 %
    private final JSlider sldPower;
    private final JTextField txtPowerVal;

    // Contour feed rate: 100 – 10000 mm/min
    private final JSlider sldContourFeed;
    private final JTextField txtContourFeedVal;

    // Contour power: 0 – 100 %
    private final JSlider sldContourPower;
    private final JTextField txtContourPowerVal;

    // Offset: lower-left corner in mm
    private final JSpinner spnOffsetX;
    private final JSpinner spnOffsetY;

    // Contour pass toggle
    private final JCheckBox chkContour;

    private boolean updating = false;

    public LaserParametersPanel() {
        // Column: label | slider (grows) | value (right-aligned, editable)
        setLayout(new MigLayout("fillx, insets 8 8 8 8, gapy 3", "[86][grow][62]", ""));
        setOpaque(false);

        // ═══════════════ FILL PASS ═══════════════
        add(createSubHeader("Fill Pass"), "span 3, growx, wrap 4");

        // Spot Radius X
        add(DarkTheme.createLabel("Spot Radius X:"), "aligny center");
        sldRadiusX = createSlider(0, RADIUS_TICKS, (int) (0.025 / RADIUS_TICK_MM));
        sldRadiusX.setToolTipText(
                "<html><b>Laser spot radius on the X axis</b> (0 … 0.1 mm, 0.001 mm steps)<br>" +
                "Half of the spot width in X direction (0.05 mm spot → 0.025 mm).<br>" +
                "Erosion keeps the laser centre this far from shape boundaries.</html>");
        txtRadiusXVal = createValueField();
        wireSlider(sldRadiusX, txtRadiusXVal,
                tick -> radiusLabel(tick * RADIUS_TICK_MM),
                text -> (int) Math.round(parseMm(text) / RADIUS_TICK_MM),
                val -> { if (settings != null) { settings.setRadiusXMm(val * RADIUS_TICK_MM); notifyChange(); } });
        add(sldRadiusX, "growx, aligny center");
        add(txtRadiusXVal, "wrap, aligny center");

        // Spot Radius Y
        add(DarkTheme.createLabel("Spot Radius Y:"), "aligny center");
        sldRadiusY = createSlider(0, RADIUS_TICKS, (int) (0.04 / RADIUS_TICK_MM));
        sldRadiusY.setToolTipText(
                "<html><b>Laser spot radius on the Y axis</b> (0 … 0.1 mm, 0.001 mm steps)<br>" +
                "Half of the spot width in Y direction (0.08 mm spot → 0.04 mm).<br>" +
                "Erosion keeps the laser centre this far from shape boundaries.</html>");
        txtRadiusYVal = createValueField();
        wireSlider(sldRadiusY, txtRadiusYVal,
                tick -> radiusLabel(tick * RADIUS_TICK_MM),
                text -> (int) Math.round(parseMm(text) / RADIUS_TICK_MM),
                val -> { if (settings != null) { settings.setRadiusYMm(val * RADIUS_TICK_MM); notifyChange(); } });
        add(sldRadiusY, "growx, aligny center");
        add(txtRadiusYVal, "wrap, aligny center");

        // Stepover
        add(DarkTheme.createLabel("Stepover:"), "aligny center");
        sldStepover = createSlider(1, 100, 2);
        sldStepover.setToolTipText("Line spacing between passes (0.01 – 1.0 mm)");
        txtStepoverVal = createValueField();
        wireSlider(sldStepover, txtStepoverVal,
                tick -> String.format(Locale.ENGLISH, "%.2f mm", tick / 100.0),
                text -> (int) Math.round(parseMm(text) * 100),
                val -> { if (settings != null) { settings.setStepoverMm(val / 100.0); notifyChange(); } });
        add(sldStepover, "growx, aligny center");
        add(txtStepoverVal, "wrap, aligny center");

        // Fill Feed Rate
        add(DarkTheme.createLabel("Feed Rate:"), "aligny center");
        sldFeed = createSlider(1, 100, 30);
        sldFeed.setToolTipText("Fill pass movement speed (100 – 10000 mm/min)");
        txtFeedVal = createValueField();
        wireSlider(sldFeed, txtFeedVal,
                tick -> (tick * 100) + " mm/min",
                text -> (int) Math.round(parseNumber(text) / 100.0),
                val -> { if (settings != null) { settings.setFeedRateMmPerMin(val * 100); notifyChange(); } });
        add(sldFeed, "growx, aligny center");
        add(txtFeedVal, "wrap, aligny center");

        // Fill Power
        add(DarkTheme.createLabel("Power:"), "aligny center");
        sldPower = createSlider(0, 100, 75);
        sldPower.setToolTipText("Fill pass laser power level (0 – 100 %)");
        txtPowerVal = createValueField();
        wireSlider(sldPower, txtPowerVal,
                tick -> tick + " %",
                text -> (int) Math.round(parseNumber(text)),
                val -> { if (settings != null) { settings.setPowerPercent(val); notifyChange(); } });
        add(sldPower, "growx, aligny center");
        add(txtPowerVal, "wrap 10, aligny center");

        // ═══════════════ CONTOUR PASS ═══════════════
        add(createSubHeader("Contour Pass"), "span 3, growx, wrap 4");

        chkContour = new JCheckBox("Generate Contour Pass", true);
        chkContour.setFont(DarkTheme.FONT_NORMAL);
        chkContour.setToolTipText(
                "<html><b>Contour pass</b><br>" +
                "Traces the exact outline before the raster fill.<br>" +
                "Uses its own feed rate and power settings below.</html>");
        add(chkContour, "span 3, wrap 4");

        // Contour Feed Rate
        add(DarkTheme.createLabel("Feed Rate:"), "aligny center");
        sldContourFeed = createSlider(1, 100, 30);
        sldContourFeed.setToolTipText("Contour pass movement speed (100 – 10000 mm/min)");
        txtContourFeedVal = createValueField();
        wireSlider(sldContourFeed, txtContourFeedVal,
                tick -> (tick * 100) + " mm/min",
                text -> (int) Math.round(parseNumber(text) / 100.0),
                val -> { if (settings != null) { settings.setContourFeedRateMmPerMin(val * 100); notifyChange(); } });
        add(sldContourFeed, "growx, aligny center");
        add(txtContourFeedVal, "wrap, aligny center");

        // Contour Power
        add(DarkTheme.createLabel("Power:"), "aligny center");
        sldContourPower = createSlider(0, 100, 75);
        sldContourPower.setToolTipText("Contour pass laser power level (0 – 100 %)");
        txtContourPowerVal = createValueField();
        wireSlider(sldContourPower, txtContourPowerVal,
                tick -> tick + " %",
                text -> (int) Math.round(parseNumber(text)),
                val -> { if (settings != null) { settings.setContourPowerPercent(val); notifyChange(); } });
        add(sldContourPower, "growx, aligny center");
        add(txtContourPowerVal, "wrap 10, aligny center");

        // ═══════════════ POSITION ═══════════════
        add(createSubHeader("Position"), "span 3, growx, wrap 4");

        spnOffsetX = createOffsetSpinner(0.0);
        spnOffsetX.setToolTipText(
                "<html>Manual fine-tune offset added on top of the board-aligned position (mm, X axis).<br>" +
                "Leave at 0 unless you need to nudge the burn position (e.g. kerf correction).</html>");
        spnOffsetY = createOffsetSpinner(0.0);
        spnOffsetY.setToolTipText(
                "<html>Manual fine-tune offset added on top of the board-aligned position (mm, Y axis).<br>" +
                "Leave at 0 unless you need to nudge the burn position (e.g. kerf correction).</html>");

        JPanel offsetRow = new JPanel(new MigLayout("insets 0, fillx, gapx 4", "[24][grow][28][grow]", ""));
        offsetRow.setOpaque(false);
        offsetRow.add(DarkTheme.createDimLabel("X:"));
        offsetRow.add(spnOffsetX, "growx");
        offsetRow.add(DarkTheme.createDimLabel("Y:"), "gapleft 6");
        offsetRow.add(spnOffsetY, "growx");
        add(offsetRow, "span 3, growx, wrap 2");

        JButton btnApplyOffset = DarkTheme.createButton("Apply Offset to All Layers",
                "Copy X/Y fine-tune offset to all other layers");
        add(btnApplyOffset, "span 3, growx, h 22!, wrap");

        // ─── Change listeners ─────────────────────────────────────────────────
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
        // (value fields, buttons) is never clipped, even in a narrow tool panel.
        Dimension pref = getPreferredSize();
        setPreferredSize(new Dimension(280, pref.height));
    }

    /**
     * Wires a slider to its trailing editable value field: slider changes
     * update the field text (via {@code formatter}); typing a value and
     * pressing Enter / losing focus parses it (via {@code parser}) and moves
     * the slider (clamped to its min/max by Swing), which re-fires the
     * commit callback.
     */
    private void wireSlider(JSlider slider, JTextField field,
                             IntFunction<String> formatter, ToIntFunction<String> parser,
                             java.util.function.IntConsumer onCommit) {
        slider.addChangeListener(e -> {
            if (updating) return;
            field.setText(formatter.apply(slider.getValue()));
            onCommit.accept(slider.getValue());
        });
        Runnable commitText = () -> {
            try {
                int tick = parser.applyAsInt(field.getText());
                slider.setValue(tick); // Swing clamps to [min,max]; fires the listener above
            } catch (NumberFormatException ignored) {
                field.setText(formatter.apply(slider.getValue())); // revert on bad input
            }
        };
        field.addActionListener(e -> commitText.run());
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { commitText.run(); }
        });
    }

    private static double parseMm(String text) {
        return parseNumber(text.replaceAll("(?i)\\s*mm\\s*$", ""));
    }

    private static double parseNumber(String text) {
        String cleaned = text.replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty()) throw new NumberFormatException("empty");
        return Double.parseDouble(cleaned);
    }

    /** Small accent-coloured sub-header with a trailing separator line. */
    private static JPanel createSubHeader(String title) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        JLabel lbl = new JLabel(title.toUpperCase());
        lbl.setFont(DarkTheme.FONT_BOLD);
        lbl.setForeground(DarkTheme.ACCENT);
        JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        sep.setForeground(DarkTheme.BG_MEDIUM);
        p.add(lbl, BorderLayout.WEST);
        p.add(sep, BorderLayout.CENTER);
        return p;
    }

    /** Formats the radius value in mm (0 … 0.1 mm range). */
    private static String radiusLabel(double mm) {
        return String.format(Locale.ENGLISH, "%.3f mm", mm);
    }

    /** Compact, right-aligned, click-to-edit value field (Lightroom-style). */
    private static JTextField createValueField() {
        JTextField field = new JTextField();
        field.setFont(DarkTheme.FONT_SMALL);
        field.setHorizontalAlignment(SwingConstants.RIGHT);
        field.putClientProperty("JComponent.roundRect", true);
        field.setToolTipText("Click to type an exact value");
        return field;
    }

    public void setSettings(LaserSettings s) {
        this.settings = null;
        updating = true;
        try {
            if (s != null) {
                sldRadiusX.setValue((int) Math.round(s.getRadiusXMm() / RADIUS_TICK_MM));
                txtRadiusXVal.setText(radiusLabel(s.getRadiusXMm()));

                sldRadiusY.setValue((int) Math.round(s.getRadiusYMm() / RADIUS_TICK_MM));
                txtRadiusYVal.setText(radiusLabel(s.getRadiusYMm()));

                sldStepover.setValue((int) Math.round(s.getStepoverMm() * 100));
                txtStepoverVal.setText(String.format(Locale.ENGLISH, "%.2f mm", s.getStepoverMm()));

                sldFeed.setValue((int) Math.round(s.getFeedRateMmPerMin() / 100));
                txtFeedVal.setText((int) s.getFeedRateMmPerMin() + " mm/min");

                sldPower.setValue((int) Math.round(s.getPowerPercent()));
                txtPowerVal.setText((int) s.getPowerPercent() + " %");

                sldContourFeed.setValue((int) Math.round(s.getContourFeedRateMmPerMin() / 100));
                txtContourFeedVal.setText((int) s.getContourFeedRateMmPerMin() + " mm/min");

                sldContourPower.setValue((int) Math.round(s.getContourPowerPercent()));
                txtContourPowerVal.setText((int) s.getContourPowerPercent() + " %");

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
        s.setOpaque(false);
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
        return spinner;
    }
}
