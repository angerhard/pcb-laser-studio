package de.andreasgerhard.pcblaserstudio.view.panels;

import de.andreasgerhard.pcblaserstudio.model.Layer;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Panel for GCODE prefix/suffix scripting per layer.
 */
public class GCodePrePostPanel extends JPanel {

    private Layer layer;
    private Consumer<Layer> onChange;
    private Consumer<Void> onApplyToAll;

    private final JTextArea txtPrefix;
    private final JTextArea txtSuffix;
    private boolean updating = false;

    public GCodePrePostPanel() {
        setLayout(new MigLayout("fillx, insets 8 8 8 8, gapy 4", "[grow]", ""));
        setOpaque(false);

        add(DarkTheme.createDimLabel("Prefix (Before Laser):"), "wrap 2");
        txtPrefix = createTextArea("G00\nG17\nG40\nG21\nG54\nG90\nM4\nM9\n");
        txtPrefix.setToolTipText("Commands executed before laser starts");
        add(new JScrollPane(txtPrefix), "growx, h 90:90:90, wrap 6");

        add(DarkTheme.createDimLabel("Suffix (After Laser):"), "wrap 2");
        txtSuffix = createTextArea("G1\nS0\nM5\nG0\nX0Y0\nM2\n");
        txtSuffix.setToolTipText("Commands executed after laser stops");
        add(new JScrollPane(txtSuffix), "growx, h 60:60:60, wrap 6");

        JButton btnApplyAll = DarkTheme.createButton("Apply to All Layers", "Copy prefix/suffix to all other layers");
        add(btnApplyAll, "growx, h 22!, wrap");
        btnApplyAll.addActionListener(e -> { syncToLayer(); if (onApplyToAll != null) onApplyToAll.accept(null); });

        // Listeners for text changes
        txtPrefix.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { syncToLayer(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { syncToLayer(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { syncToLayer(); }
        });
        txtSuffix.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { syncToLayer(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { syncToLayer(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { syncToLayer(); }
        });
    }

    public void setLayer(Layer l) {
        this.layer = null;
        updating = true;
        try {
            if (l != null) {
                txtPrefix.setText(l.getGcodePrefix() != null ? l.getGcodePrefix() : "");
                txtSuffix.setText(l.getGcodeSuffix() != null ? l.getGcodeSuffix() : "");
            }
        } finally {
            updating = false;
            this.layer = l;
        }
    }

    public String getPrefix() { return txtPrefix.getText(); }
    public String getSuffix() { return txtSuffix.getText(); }

    private void syncToLayer() {
        if (updating || layer == null) return;
        layer.setGcodePrefix(txtPrefix.getText());
        layer.setGcodeSuffix(txtSuffix.getText());
        if (onChange != null) onChange.accept(layer);
    }

    public void setOnChange(Consumer<Layer> cb) { this.onChange = cb; }
    public void setOnApplyToAll(Consumer<Void> cb) { this.onApplyToAll = cb; }

    private static JTextArea createTextArea(String defaultText) {
        JTextArea ta = new JTextArea(defaultText);
        ta.setFont(DarkTheme.FONT_MONO);
        ta.setBackground(DarkTheme.BG_DARKEST);
        ta.setForeground(DarkTheme.TEXT_PRIMARY);
        ta.setCaretColor(DarkTheme.TEXT_PRIMARY);
        ta.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        ta.setLineWrap(false);
        return ta;
    }
}

