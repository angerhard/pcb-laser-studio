package de.andreasgerhard.pcblaserstudio.view.theme;

import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Custom dark theme for PCB Laser Studio ("Graphite &amp; Cyan"), built on
 * top of FlatLaf's dark base theme. The actual palette and component sizing
 * (sliders, arcs, tabs, scrollbars, ...) live in {@code PcbLaserTheme.properties}
 * next to this class - see FlatLaf's "New theme" customization mechanism.
 */
public class PcbLaserTheme extends FlatDarkLaf {

    public static final String NAME = "PcbLaserTheme";

    public static boolean setup() {
        return setup(new PcbLaserTheme());
    }

    @Override
    public String getName() {
        return NAME;
    }
}
