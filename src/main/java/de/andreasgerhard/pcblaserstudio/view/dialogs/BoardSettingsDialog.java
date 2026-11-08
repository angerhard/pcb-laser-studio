package de.andreasgerhard.pcblaserstudio.view.dialogs;

import de.andreasgerhard.pcblaserstudio.model.BoardSettings;
import de.andreasgerhard.pcblaserstudio.model.FlipAxis;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
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
    private final JComboBox<FlipAxis> cmbFlipAxis;
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

        form.add(DarkTheme.createLabel("Flip axis:"), "");
        cmbFlipAxis = new JComboBox<>(FlipAxis.values());
        cmbFlipAxis.setFont(DarkTheme.FONT_NORMAL);
        cmbFlipAxis.setToolTipText(
                "<html>Which axis the board is turned over around when flipping to the back side.<br>" +
                "The board is assumed to flip in place (its bounding box doesn't move).</html>");
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
        cmbFlipAxis.addActionListener(e -> apply());
        chkMirrorBack.addActionListener(e -> apply());
        chkBorder.addActionListener(e -> apply());
    }

    private void apply() {
        if (updating || settings == null) return;
        settings.setWidthMm((Double) spnWidth.getValue());
        settings.setHeightMm((Double) spnHeight.getValue());
        settings.setMarginMm((Double) spnMargin.getValue());
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
            cmbFlipAxis.setSelectedItem(settings.getFlipAxis());
            chkMirrorBack.setSelected(settings.isMirrorBackArtwork());
            chkBorder.setSelected(settings.isGenerateBorder());
        } finally {
            updating = false;
            this.settings = settings;
        }
        refreshWarnings();
    }

    public void setOnChange(Consumer<BoardSettings> cb) { this.onChange = cb; }

    public void refreshWarnings() {
        List<String> warnings = mismatchSupplier != null ? mismatchSupplier.get() : List.of();
        if (warnings.isEmpty()) {
            warningArea.setForeground(DarkTheme.SUCCESS);
            warningArea.setText("No size mismatch detected.");
        } else {
            warningArea.setForeground(DarkTheme.DANGER);
            StringBuilder sb = new StringBuilder();
            for (String w : warnings) sb.append("⚠ ").append(w).append("\n\n");
            warningArea.setText(sb.toString().trim());
        }
        warningArea.setCaretPosition(0);
    }

    private static JSpinner createMmSpinner(double initial) {
        SpinnerNumberModel model = new SpinnerNumberModel(initial, 0.0, 2000.0, 0.5);
        JSpinner spinner = new JSpinner(model);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0.0");
        spinner.setEditor(editor);
        spinner.setBackground(DarkTheme.BG_DARKEST);
        spinner.setForeground(DarkTheme.TEXT_PRIMARY);
        editor.getTextField().setBackground(DarkTheme.BG_DARKEST);
        editor.getTextField().setForeground(DarkTheme.TEXT_PRIMARY);
        editor.getTextField().setCaretColor(DarkTheme.TEXT_PRIMARY);
        return spinner;
    }
}
