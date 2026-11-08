package de.andreasgerhard.pcblaserstudio.view;

import de.andreasgerhard.pcblaserstudio.model.Layer;
import de.andreasgerhard.pcblaserstudio.model.LayerType;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Custom tab header component: a small colored type dot, compact bold label,
 * enable checkbox, and a dirty indicator dot - a slim module-switcher pill
 * rather than a default wide JTabbedPane header.
 */
public class LayerTabComponent extends JPanel {

    private final JCheckBox chkEnabled;
    private final JLabel    lblDot;
    private final JLabel    lblName;
    private final JLabel    lblDirty;
    private Layer layer;
    private Consumer<Layer> onEnabledChanged;

    public LayerTabComponent(Layer layer) {
        super(new MigLayout("insets 1 2 1 2, gapx 4", "[][]8[]", "[]"));
        setOpaque(false);
        this.layer = layer;

        chkEnabled = new JCheckBox();
        chkEnabled.setOpaque(false);
        chkEnabled.setToolTipText("Include in GCODE generation");
        chkEnabled.addActionListener(e -> {
            if (this.layer != null) {
                this.layer.setEnabled(chkEnabled.isSelected());
                if (onEnabledChanged != null) onEnabledChanged.accept(this.layer);
            }
        });

        lblDot = new JLabel("●");
        lblDot.setFont(DarkTheme.FONT_SMALL);

        lblName = new JLabel(getTabTitle());
        lblName.setFont(DarkTheme.FONT_BOLD);
        lblName.setForeground(DarkTheme.TEXT_PRIMARY);

        lblDirty = new JLabel("");
        lblDirty.setFont(DarkTheme.FONT_BOLD);
        lblDirty.setForeground(DarkTheme.ACCENT);

        add(chkEnabled);
        add(lblDot);
        add(lblName);
        add(lblDirty);

        refresh();
    }

    public void refresh() {
        if (layer != null) {
            chkEnabled.setSelected(layer.isEnabled());
            lblName.setText(getTabTitle());
            lblDirty.setText(layer.isDirty() ? "●" : "");
            Color fg = layer.isEnabled() ? DarkTheme.TEXT_PRIMARY : DarkTheme.TEXT_DISABLED;
            lblName.setForeground(fg);
            lblDot.setForeground(layer.getType() == LayerType.ETCH ? DarkTheme.ACCENT : DarkTheme.WARNING);
        }
    }

    private String getTabTitle() {
        if (layer == null) return "Layer";
        String side = layer.getSide() != null ? layer.getSide().toString() : "";
        String type = layer.getType() != null ? layer.getType().toString() : "";
        return type + " · " + side;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
        refresh();
    }

    public void setOnEnabledChanged(Consumer<Layer> cb) { this.onEnabledChanged = cb; }
}
