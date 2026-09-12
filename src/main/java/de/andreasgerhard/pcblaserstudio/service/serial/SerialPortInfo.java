package de.andreasgerhard.pcblaserstudio.service.serial;

/**
 * Describes a serial port available on the system.
 *
 * @param systemPortName    OS port name, e.g. {@code /dev/ttyACM0} or {@code COM3}
 * @param descriptivePortName human-readable description reported by the OS/driver
 * @param vendorId          USB vendor id, or {@code -1} if unknown
 * @param productId         USB product id, or {@code -1} if unknown
 * @param likelyLaser       heuristic flag for known USB-serial chips used by lasers
 */
public record SerialPortInfo(
        String systemPortName,
        String descriptivePortName,
        int vendorId,
        int productId,
        boolean likelyLaser
) {
    /** A compact one-line label for combo boxes. */
    public String display() {
        StringBuilder sb = new StringBuilder(systemPortName);
        if (descriptivePortName != null && !descriptivePortName.isBlank()) {
            sb.append("  –  ").append(descriptivePortName);
        }
        if (likelyLaser) sb.append("  (laser?)");
        return sb.toString();
    }
}
