package de.andreasgerhard.pcblaserstudio.view.dialogs;

import de.andreasgerhard.pcblaserstudio.model.CutSettings;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Modal dialog for the board cut-out parameters. The cut geometry and side
 * are taken from the active Etch tab; this dialog only asks for the laser
 * parameters and pass count.
 */
public class BoardCutDialog extends JDialog {

    private final JSpinner spnPower;
    private final JSpinner spnFeed;
    private final JSpinner spnRepeats;
    private boolean confirmed;

    public BoardCutDialog(JFrame owner, String sourceText, String rectangleText,
                          CutSettings settings) {
        super(owner, "Cut Out Board", true);

        CutSettings initial = settings != null ? settings : new CutSettings();

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(DarkTheme.BG_DARK);
        getContentPane().setLayout(new MigLayout("fill, insets 10, gapy 6", "[grow]", ""));

        JPanel info = new JPanel(new MigLayout("fillx, insets 8, gapy 4", "[grow]", ""));
        info.setBackground(DarkTheme.BG_DARK);
        info.setBorder(DarkTheme.createSectionBorder("Cut Job"));
        info.add(DarkTheme.createDimLabel("Source:"), "split 2");
        info.add(DarkTheme.createLabel(sourceText), "wrap");
        info.add(DarkTheme.createDimLabel("Rectangle:"), "split 2");
        info.add(DarkTheme.createLabel(rectangleText), "wrap");
        info.add(DarkTheme.createDimLabel(
                "<html>Cut side follows the active tab. Uses the existing SVG-border geometry.<br>" +
                "No kerf compensation is applied.</html>"),
                "span, wrap");
        add(info, "growx, wrap");

        JPanel form = new JPanel(new MigLayout("fillx, insets 8, gapy 6", "[130][grow]", ""));
        form.setBackground(DarkTheme.BG_DARK);
        form.setBorder(DarkTheme.createSectionBorder("Cut Parameters"));

        form.add(DarkTheme.createLabel("Power:"), "");
        spnPower = createDoubleSpinner(initial.getPowerPercent(), 0.0, 100.0, 1.0, "0.0");
        spnPower.setToolTipText("Laser power for the cut contour (0-100 %)");
        form.add(spnPower, "split 2, w 100!");
        form.add(DarkTheme.createDimLabel("%"), "wrap");

        form.add(DarkTheme.createLabel("Feed rate:"), "");
        spnFeed = createIntSpinner(initial.getFeedRateMmPerMin(), 1, 100_000, 50);
        spnFeed.setToolTipText("Cut feed rate in mm/min");
        form.add(spnFeed, "split 2, w 100!");
        form.add(DarkTheme.createDimLabel("mm/min"), "wrap");

        form.add(DarkTheme.createLabel("Repeats:"), "");
        spnRepeats = createIntSpinner(initial.getRepeats(), 1, 100, 1);
        spnRepeats.setToolTipText("Total number of times the cut contour is traced");
        form.add(spnRepeats, "split 2, w 100!");
        form.add(DarkTheme.createDimLabel("passes"), "wrap");

        add(form, "growx, wrap");

        JButton btnCut = DarkTheme.createAccentButton("Cut",
                "Generate the cutting GCODE and send it to the laser");
        JButton btnCancel = DarkTheme.createButton("Cancel", "Close without cutting");
        btnCut.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        btnCancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new MigLayout("insets 0, fillx", "[grow][pref][pref]", ""));
        buttons.setOpaque(false);
        buttons.add(btnCancel, "w 100!, h 26!");
        buttons.add(btnCut, "w 110!, h 26!");
        add(buttons, "growx, wrap");

        getRootPane().setDefaultButton(btnCut);
        pack();
        setMinimumSize(new Dimension(420, 300));
        setLocationRelativeTo(owner);
    }

    /** Shows the dialog and returns true only when the user pressed Cut. */
    public boolean showDialog() {
        setVisible(true);
        return confirmed;
    }

    public double getPowerPercent() {
        return ((Number) spnPower.getValue()).doubleValue();
    }

    public int getFeedRateMmPerMin() {
        return ((Number) spnFeed.getValue()).intValue();
    }

    public int getRepeats() {
        return ((Number) spnRepeats.getValue()).intValue();
    }

    private static JSpinner createDoubleSpinner(double initial, double min, double max,
                                                double step, String format) {
        return createSpinner(new SpinnerNumberModel(initial, min, max, step), format);
    }

    private static JSpinner createIntSpinner(int initial, int min, int max, int step) {
        return createSpinner(new SpinnerNumberModel(initial, min, max, step), "0");
    }

    private static JSpinner createSpinner(SpinnerNumberModel model, String format) {
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
