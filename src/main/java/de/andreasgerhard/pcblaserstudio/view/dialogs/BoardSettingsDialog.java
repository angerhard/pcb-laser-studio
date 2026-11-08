package de.andreasgerhard.pcblaserstudio.view.dialogs;

import de.andreasgerhard.pcblaserstudio.model.BoardSettings;
import de.andreasgerhard.pcblaserstudio.model.FlipAxis;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import de.andreasgerhard.pcblaserstudio.view.theme.Icons;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Non-modal dialog for configuring the physical board (size, margin, flip
 * axis) shared by all layers, plus the mirror/border toggles. Also surfaces
 * a live warning when front and back side SVG sizes don't match.
 */
public class BoardSettingsDialog extends JDialog {

    private final JSpinner spnWidth;
    private final JSpinner spnHeight;
    private final JSpinner spnMargin;
    private final JSpinner spnPositionX;
    private final JSpinner spnPositionY;
    private final JComboBox<FlipAxis> cmbFlipAxis;
    private final JLabel lblFlipIcon;
    private final JCheckBox chkMirrorBack;
    private final JCheckBox chkBorder;
    private final JTextArea warningArea;

    private BoardSettings settings;
    private boolean updating = false;

    private Consumer<BoardSettings> onChange;
    private final Supplier<List<String>> mismatchSupplier;

    public BoardSettingsDialog(JFrame owner, Supplier<List<String>> mismatchSupplier) {
        super(owner, "Board Settings", false);
        this.mismatchSupplier = mismatchSupplier;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(DarkTheme.BG_DARK);
        getContentPane().setLayout(new MigLayout("fill, insets 10, gapy 6", "[grow]", "[][grow]"));

        JPanel form = new JPanel(new MigLayout("fillx, insets 8, gapy 6", "[130][grow]", ""));
        form.setBackground(DarkTheme.BG_DARK);
        form.setBorder(DarkTheme.createSectionBorder("Physical Board"));

        form.add(DarkTheme.createLabel("Width:"), "");
        spnWidth = createMmSpinner(100.0);
        spnWidth.setToolTipText("Physical PCB width in mm");
        form.add(spnWidth, "split 2, w 100!");
        form.add(DarkTheme.createDimLabel("mm"), "wrap");

        form.add(DarkTheme.createLabel("Height:"), "");
        spnHeight = createMmSpinner(80.0);
        spnHeight.setToolTipText("Physical PCB height in mm");
        form.add(spnHeight, "split 2, w 100!");
        form.add(DarkTheme.createDimLabel("mm"), "wrap");

        form.add(DarkTheme.createLabel("Margin:"), "");
        spnMargin = createMmSpinner(5.0);
        spnMargin.setToolTipText("Distance from the board edge to the SVG content on the anchored sides");
        form.add(spnMargin, "split 2, w 100!");
        form.add(DarkTheme.createDimLabel("mm"), "wrap");

        form.add(DarkTheme.createLabel("Position:"), "");
        spnPositionX = createPositionSpinner(0.0);
        spnPositionX.setToolTipText(
                "<html>X position of the board's bottom-left corner on the machine bed (mm).<br>" +
                "Shifts the whole board - and everything burned on it - together.</html>");
        spnPositionY = createPositionSpinner(0.0);
        spnPositionY.setToolTipText(
                "<html>Y position of the board's bottom-left corner on the machine bed (mm).<br>" +
                "Shifts the whole board - and everything burned on it - together.</html>");
        JPanel positionRow = new JPanel(new MigLayout("insets 0, fillx, gapx 4", "[16][grow][16][grow]", ""));
        positionRow.setOpaque(false);
        positionRow.add(DarkTheme.createDimLabel("X:"));
        positionRow.add(spnPositionX, "growx, w 80!");
        positionRow.add(DarkTheme.createDimLabel("Y:"), "gapleft 6");
        positionRow.add(spnPositionY, "growx, w 80!");
        form.add(positionRow, "growx, wrap");

        form.add(DarkTheme.createLabel("Flip axis:"), "");
        lblFlipIcon = new JLabel(Icons.of(Icons.FLIP_X, 14, DarkTheme.ACCENT));
        cmbFlipAxis = new JComboBox<>(FlipAxis.values());
        cmbFlipAxis.setFont(DarkTheme.FONT_NORMAL);
        cmbFlipAxis.setToolTipText(
                "<html>Which axis the board is turned over around when flipping to the back side.<br>" +
                "The board is assumed to flip in place (its bounding box doesn't move).</html>");
        cmbFlipAxis.addActionListener(e -> updateFlipIcon());
        form.add(lblFlipIcon, "split 2");
        form.add(cmbFlipAxis, "growx, wrap");

        chkMirrorBack = new JCheckBox("Mirror back-side artwork before burning");
        chkMirrorBack.setFont(DarkTheme.FONT_NORMAL);
        chkMirrorBack.setForeground(DarkTheme.TEXT_PRIMARY);
        chkMirrorBack.setBackground(DarkTheme.BG_DARK);
        chkMirrorBack.setToolTipText(
                "<html>Enable if your back-side SVG (etch_back / doc_back) is drawn as seen<br>" +
                "from the top. Leave off if your CAD/CAM tool already exports it pre-mirrored.</html>");
        form.add(chkMirrorBack, "span 2, wrap");

        chkBorder = new JCheckBox("Burn a border at the exact SVG size/position");
        chkBorder.setFont(DarkTheme.FONT_NORMAL);
        chkBorder.setForeground(DarkTheme.TEXT_PRIMARY);
        chkBorder.setBackground(DarkTheme.BG_DARK);
        chkBorder.setToolTipText(
                "<html>Adds one extra contour pass tracing the SVG's exact bounding box,<br>" +
                "attached to each side's Etch layer (reuses its contour power/feed).</html>");
        form.add(chkBorder, "span 2, wrap");

        add(form, "growx, wrap");

        JPanel warnPanel = new JPanel(new BorderLayout());
        warnPanel.setBackground(DarkTheme.BG_DARK);
        warnPanel.setBorder(DarkTheme.createSectionBorder("Alignment Check"));
        warningArea = new JTextArea();
        warningArea.setEditable(false);
        warningArea.setLineWrap(true);
        warningArea.setWrapStyleWord(true);
        warningArea.setFont(DarkTheme.FONT_SMALL);
        warningArea.setBackground(DarkTheme.BG_DARKEST);
        warningArea.setForeground(DarkTheme.DANGER);
        JScrollPane warnScroll = new JScrollPane(warningArea);
        warnScroll.setBorder(BorderFactory.createEmptyBorder());
        warnScroll.setPreferredSize(new Dimension(360, 90));
        warnPanel.add(warnScroll, BorderLayout.CENTER);

        JButton btnRefresh = DarkTheme.createButton("Refresh Check", "Re-check front/back SVG sizes");
        btnRefresh.setIcon(Icons.of(Icons.REFRESH, 13));
        btnRefresh.addActionListener(e -> refreshWarnings());
        warnPanel.add(btnRefresh, BorderLayout.SOUTH);

        add(warnPanel, "grow, push, wrap");

        wireListeners();

        pack();
        setMinimumSize(new Dimension(380, 420));
        setLocationRelativeTo(owner);
    }

    private void wireListeners() {
        spnWidth.addChangeListener(e -> apply());
        spnHeight.addChangeListener(e -> apply());
        spnMargin.addChangeListener(e -> apply());
        spnPositionX.addChangeListener(e -> apply());
        spnPositionY.addChangeListener(e -> apply());
        cmbFlipAxis.addActionListener(e -> apply());
        chkMirrorBack.addActionListener(e -> apply());
        chkBorder.addActionListener(e -> apply());
    }

    private void apply() {
        if (updating || settings == null) return;
        settings.setWidthMm((Double) spnWidth.getValue());
        settings.setHeightMm((Double) spnHeight.getValue());
        settings.setMarginMm((Double) spnMargin.getValue());
        settings.setPositionXMm((Double) spnPositionX.getValue());
        settings.setPositionYMm((Double) spnPositionY.getValue());
        settings.setFlipAxis((FlipAxis) cmbFlipAxis.getSelectedItem());
        settings.setMirrorBackArtwork(chkMirrorBack.isSelected());
        settings.setGenerateBorder(chkBorder.isSelected());
        if (onChange != null) onChange.accept(settings);
        refreshWarnings();
    }

    public void setBoardSettings(BoardSettings settings) {
        this.settings = null;
        updating = true;
        try {
            spnWidth.setValue(settings.getWidthMm());
            spnHeight.setValue(settings.getHeightMm());
            spnMargin.setValue(settings.getMarginMm());
            spnPositionX.setValue(settings.getPositionXMm());
            spnPositionY.setValue(settings.getPositionYMm());
            cmbFlipAxis.setSelectedItem(settings.getFlipAxis());
            chkMirrorBack.setSelected(settings.isMirrorBackArtwork());
            chkBorder.setSelected(settings.isGenerateBorder());
            updateFlipIcon();
        } finally {
            updating = false;
            this.settings = settings;
        }
        refreshWarnings();
    }

    private void updateFlipIcon() {
        FlipAxis axis = (FlipAxis) cmbFlipAxis.getSelectedItem();
        String name = axis == FlipAxis.HORIZONTAL ? Icons.FLIP_Y : Icons.FLIP_X;
        lblFlipIcon.setIcon(Icons.of(name, 14, DarkTheme.ACCENT));
    }

    public void setOnChange(Consumer<BoardSettings> cb) { this.onChange = cb; }

    public void refreshWarnings() {
        List<String> warnings = mismatchSupplier != null ? mismatchSupplier.get() : List.of();
        if (warnings.isEmpty()) {
            warningArea.setForeground(DarkTheme.SUCCESS);
            warningArea.setText("✓ No size mismatch detected.");
        } else {
            // Advisory, not blocking - amber rather than red.
            warningArea.setForeground(DarkTheme.WARNING);
            StringBuilder sb = new StringBuilder();
            for (String w : warnings) sb.append("⚠ ").append(w).append("\n\n");
            warningArea.setText(sb.toString().trim());
        }
        warningArea.setCaretPosition(0);
    }

    private static JSpinner createMmSpinner(double initial) {
        return createSpinner(initial, 0.0, 2000.0, 0.5, "0.0");
    }

    /** Position can be negative (e.g. a bed origin not at the board's corner). */
    private static JSpinner createPositionSpinner(double initial) {
        return createSpinner(initial, -9999.0, 9999.0, 0.1, "0.00");
    }

    private static JSpinner createSpinner(double initial, double min, double max, double step, String format) {
        SpinnerNumberModel model = new SpinnerNumberModel(initial, min, max, step);
        JSpinner spinner = new JSpinner(model);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, format);
        spinner.setEditor(editor);
        spinner.setBackground(DarkTheme.BG_DARKEST);
        spinner.setForeground(DarkTheme.TEXT_PRIMARY);
        editor.getTextField().setBackground(DarkTheme.BG_DARKEST);
        editor.getTextField().setForeground(DarkTheme.TEXT_PRIMARY);
        editor.getTextField().setCaretColor(DarkTheme.TEXT_PRIMARY);
        return spinner;
    }
}
