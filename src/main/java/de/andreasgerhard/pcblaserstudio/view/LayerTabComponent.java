package de.andreasgerhard.pcblaserstudio.view;

import de.andreasgerhard.pcblaserstudio.model.Layer;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Custom tab header component showing enable checkbox, layer name, and dirty indicator.
 */
public class LayerTabComponent extends JPanel {

    private final JCheckBox chkEnabled;
    private final JLabel    lblName;
    private final JLabel    lblDirty;
    private Layer layer;
    private Consumer<Layer> onEnabledChanged;

    public LayerTabComponent(Layer layer) {
        super(new FlowLayout(FlowLayout.LEFT, 2, 0));
        setOpaque(false);
        this.layer = layer;

        chkEnabled = new JCheckBox();
        chkEnabled.setSelected(layer.isEnabled());
        chkEnabled.setOpaque(false);
        chkEnabled.setToolTipText("Include in GCODE generation");
        chkEnabled.addActionListener(e -> {
            if (this.layer != null) {
                this.layer.setEnabled(chkEnabled.isSelected());
                if (onEnabledChanged != null) onEnabledChanged.accept(this.layer);
            }
        });

        lblName = new JLabel(getTabTitle());
        lblName.setFont(DarkTheme.FONT_NORMAL);
        lblName.setForeground(DarkTheme.TEXT_PRIMARY);

        lblDirty = new JLabel("");
        lblDirty.setFont(DarkTheme.FONT_BOLD);
        lblDirty.setForeground(DarkTheme.ACCENT);

        add(chkEnabled);
        add(lblName);
        add(lblDirty);
    }

    public void refresh() {
        if (layer != null) {
            chkEnabled.setSelected(layer.isEnabled());
            lblName.setText(getTabTitle());
            lblDirty.setText(layer.isDirty() ? " *" : "");
            lblName.setForeground(layer.isEnabled() ? DarkTheme.TEXT_PRIMARY : DarkTheme.TEXT_DIM);
        }
    }

    private String getTabTitle() {
        if (layer == null) return "Layer";
        // Short title
        String side = layer.getSide() != null ? layer.getSide().toString() : "";
        String type = layer.getType() != null ? layer.getType().toString() : "";
        return type + " " + side;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
        refresh();
    }

}

