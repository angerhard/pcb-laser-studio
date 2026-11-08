package de.andreasgerhard.pcblaserstudio.view.panels;

import de.andreasgerhard.pcblaserstudio.model.Layer;
import de.andreasgerhard.pcblaserstudio.model.LayerSide;
import de.andreasgerhard.pcblaserstudio.model.LayerType;
import de.andreasgerhard.pcblaserstudio.model.ProcessingMode;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Panel for Layer metadata (name, type, side, enabled, processing mode).
 */
public class LayerSettingsPanel extends JPanel {

    private Layer layer;

    private final JCheckBox chkEnabled;
    private final JTextField txtName;
    private final JComboBox<LayerType> cmbType;
    private final JComboBox<LayerSide> cmbSide;
    private final JComboBox<ProcessingMode> cmbMode;

    private Consumer<Layer> onChange;

    public LayerSettingsPanel() {
        setLayout(new MigLayout("fillx, insets 8 8 8 8, gapy 4", "[130][grow]", ""));
        setOpaque(false);

        // Enabled
        chkEnabled = new JCheckBox("Enabled");
        chkEnabled.setFont(DarkTheme.FONT_NORMAL);
        chkEnabled.setToolTipText("Include this layer in GCODE generation");
        add(chkEnabled, "span 2, wrap");

        // Name
        add(DarkTheme.createLabel("Name:"), "");
        txtName = new JTextField();
        txtName.setFont(DarkTheme.FONT_NORMAL);
        txtName.setToolTipText("Layer display name");
        add(txtName, "growx, wrap");

        // Type
        add(DarkTheme.createLabel("Type:"), "");
        cmbType = new JComboBox<>(LayerType.values());
        cmbType.setFont(DarkTheme.FONT_NORMAL);
        cmbType.setToolTipText("Etch = engraving; Documentation = marking");
        add(cmbType, "growx, wrap");

        // Side
        add(DarkTheme.createLabel("Side:"), "");
        cmbSide = new JComboBox<>(LayerSide.values());
        cmbSide.setFont(DarkTheme.FONT_NORMAL);
        cmbSide.setToolTipText("PCB side (for reference)");
        add(cmbSide, "growx, wrap");

        // Mode
        add(DarkTheme.createLabel("Mode:"), "");
        cmbMode = new JComboBox<>(ProcessingMode.values());
        cmbMode.setFont(DarkTheme.FONT_NORMAL);
        cmbMode.setToolTipText("Which areas of the SVG to engrave");
        add(cmbMode, "growx, wrap");

        // Listeners
        chkEnabled.addActionListener(e -> { if (layer != null) { layer.setEnabled(chkEnabled.isSelected()); notify(layer); }});
        txtName.addActionListener(e -> { if (layer != null) { layer.setName(txtName.getText()); notify(layer); }});
        cmbType.addActionListener(e -> { if (layer != null) { layer.setType((LayerType) cmbType.getSelectedItem()); notify(layer); }});
        cmbSide.addActionListener(e -> { if (layer != null) { layer.setSide((LayerSide) cmbSide.getSelectedItem()); notify(layer); }});
        cmbMode.addActionListener(e -> { if (layer != null) { layer.setProcessingMode((ProcessingMode) cmbMode.getSelectedItem()); notify(layer); }});
    }

    public void setLayer(Layer layer) {
        this.layer = null; // prevent feedback
        if (layer != null) {
            chkEnabled.setSelected(layer.isEnabled());
            txtName.setText(layer.getName());
            cmbType.setSelectedItem(layer.getType());
            cmbSide.setSelectedItem(layer.getSide());
            cmbMode.setSelectedItem(layer.getProcessingMode());
        }
        this.layer = layer;
    }

    public void setOnChange(Consumer<Layer> onChange) { this.onChange = onChange; }

    private void notify(Layer l) { if (onChange != null) onChange.accept(l); }
}

