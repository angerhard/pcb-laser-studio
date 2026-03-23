package de.andreasgerhard.pcblaserstudio;

import de.andreasgerhard.pcblaserstudio.controller.MainController;
import org.pushingpixels.radiance.theming.api.skin.RadianceRavenLookAndFeel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Entry point for PCB Laser Studio.
 */
public class MainApplication {

    private static final Logger log = LoggerFactory.getLogger(MainApplication.class);

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel as base, then override with our dark theme
                try {
                    UIManager.setLookAndFeel(new RadianceRavenLookAndFeel());
                } catch (Exception e) {
                    log.warn("Could not set cross-platform L&F, using default: {}", e.getMessage());
                }
                log.info("Starting PCB Laser Studio...");
                MainController controller = new MainController();
                controller.initialize();
            } catch (Exception e) {
                log.error("Fatal startup error", e);
                JOptionPane.showMessageDialog(null,
                        "Fatal startup error:\n" + e.getMessage(),
                        "PCB Laser Studio – Startup Error",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}

