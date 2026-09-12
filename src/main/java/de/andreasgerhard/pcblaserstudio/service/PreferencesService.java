package de.andreasgerhard.pcblaserstudio.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.andreasgerhard.pcblaserstudio.model.ConnectionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Persists application-wide preferences (currently the laser connection
 * settings) as JSON in the user config directory:
 *
 * <pre>
 *   ${XDG_CONFIG_HOME:-~/.config}/pcb-laser-studio/settings.json
 * </pre>
 *
 * <p>Reads are fail-safe: a missing or corrupt file simply yields defaults.</p>
 */
public class PreferencesService {

    private static final Logger log = LoggerFactory.getLogger(PreferencesService.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final Path settingsFile;

    public PreferencesService() {
        this(defaultSettingsFile());
    }

    public PreferencesService(Path settingsFile) {
        this.settingsFile = settingsFile;
    }

    public static Path defaultSettingsFile() {
        String configHome = System.getenv("XDG_CONFIG_HOME");
        Path base;
        if (configHome != null && !configHome.isBlank()) {
            base = Paths.get(configHome);
        } else {
            base = Paths.get(System.getProperty("user.home", "."), ".config");
        }
        return base.resolve("pcb-laser-studio").resolve("settings.json");
    }

    public Path getSettingsFile() { return settingsFile; }

    /** @return the stored settings, or fresh defaults if none/invalid. */
    public ConnectionSettings load() {
        if (!Files.isRegularFile(settingsFile)) return new ConnectionSettings();
        try {
            return MAPPER.readValue(settingsFile.toFile(), ConnectionSettings.class);
        } catch (Exception ex) {
            log.warn("Could not read preferences from {}: {}", settingsFile, ex.getMessage());
            return new ConnectionSettings();
        }
    }

    public void save(ConnectionSettings settings) {
        try {
            Files.createDirectories(settingsFile.getParent());
            MAPPER.writeValue(settingsFile.toFile(), settings);
        } catch (IOException ex) {
            log.warn("Could not write preferences to {}: {}", settingsFile, ex.getMessage());
        }
    }

    /** Convenience for callers that already have a File (tests). */
    public void save(ConnectionSettings settings, File file) throws IOException {
        MAPPER.writeValue(file, settings);
    }
}
