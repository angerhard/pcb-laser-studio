package de.andreasgerhard.pcblaserstudio.model;

/**
 * Serial connection settings for sending GCODE to a laser cutter.
 *
 * <p>These are application-wide (machine specific), not part of a project.
 * The {@code protocol} / {@code profileName} fields are deliberately generic so
 * additional machine families can be supported later without changing the
 * connection architecture.</p>
 */
public class ConnectionSettings {

    /** Serial port system name, e.g. {@code /dev/ttyACM0} or {@code COM3}. */
    private String portName = null;
    /** Baud rate; ignored by native USB-CDC controllers but required by USB-serial bridges. */
    private int baudRate = 115200;
    /** Whether to raise DTR on connect (some boards reset on DTR toggle). */
    private boolean dtr = false;
    /** Use the built-in simulator instead of a real serial port (for testing). */
    private boolean simulator = false;
    /** Streaming protocol/profile identifier, e.g. {@code GRBL}. */
    private String protocol = "GRBL";
    /** Human-readable device profile name, e.g. {@code Creality Falcon A1}. */
    private String profileName = "Creality Falcon A1";
    /** Work area width in mm (used for sanity checks). */
    private double workAreaWidthMm = 381.0;
    /** Work area height in mm (used for sanity checks). */
    private double workAreaHeightMm = 305.0;

    public ConnectionSettings() {}

    public ConnectionSettings(ConnectionSettings other) {
        this.portName = other.portName;
        this.baudRate = other.baudRate;
        this.dtr = other.dtr;
        this.simulator = other.simulator;
        this.protocol = other.protocol;
        this.profileName = other.profileName;
        this.workAreaWidthMm = other.workAreaWidthMm;
        this.workAreaHeightMm = other.workAreaHeightMm;
    }

    public String getPortName() { return portName; }
    public void setPortName(String portName) { this.portName = portName; }

    public int getBaudRate() { return baudRate; }
    public void setBaudRate(int baudRate) { this.baudRate = baudRate; }

    public boolean isDtr() { return dtr; }
    public void setDtr(boolean dtr) { this.dtr = dtr; }

    public boolean isSimulator() { return simulator; }
    public void setSimulator(boolean simulator) { this.simulator = simulator; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public double getWorkAreaWidthMm() { return workAreaWidthMm; }
    public void setWorkAreaWidthMm(double workAreaWidthMm) { this.workAreaWidthMm = workAreaWidthMm; }

    public double getWorkAreaHeightMm() { return workAreaHeightMm; }
    public void setWorkAreaHeightMm(double workAreaHeightMm) { this.workAreaHeightMm = workAreaHeightMm; }
}
