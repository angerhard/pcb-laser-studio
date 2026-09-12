package de.andreasgerhard.pcblaserstudio.service.serial;

import com.fazecast.jSerialComm.SerialPort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Enumerates serial ports that could host a laser cutter controller.
 *
 * <p>Uses jSerialComm so the same code works on Linux ({@code /dev/ttyACM*},
 * {@code /dev/ttyUSB*}) and Windows ({@code COMx}).</p>
 */
public final class LaserPortScanner {

    /** USB vendor ids commonly used by laser/3D-printer controllers. */
    private static final Set<Integer> KNOWN_VENDORS = Set.of(
            0x1A86, // QinHeng CH340/CH341
            0x10C4, // Silicon Labs CP210x
            0x0403, // FTDI
            0x303A, // Espressif (native USB-CDC)
            0x2341, // Arduino
            0x2A03, // Arduino LLC
            0x239A  // Adafruit
    );

    private LaserPortScanner() {}

    public static List<SerialPortInfo> scan() {
        List<SerialPortInfo> result = new ArrayList<>();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort p : ports) {
            int vid = safeVendor(p);
            int pid = safeProduct(p);
            String description = p.getDescriptivePortName();
            boolean likely = isLikelyLaser(p.getSystemPortName(), description, vid);
            result.add(new SerialPortInfo(p.getSystemPortName(), description, vid, pid, likely));
        }
        result.sort(Comparator
                .comparing(SerialPortInfo::likelyLaser).reversed()
                .thenComparing(SerialPortInfo::systemPortName));
        return result;
    }

    private static int safeVendor(SerialPort p) {
        try { return p.getVendorID(); } catch (Exception e) { return -1; }
    }

    private static int safeProduct(SerialPort p) {
        try { return p.getProductID(); } catch (Exception e) { return -1; }
    }

    private static boolean isLikelyLaser(String systemName, String description, int vid) {
        if (KNOWN_VENDORS.contains(vid)) return true;
        if (systemName != null) {
            String n = systemName.toLowerCase(Locale.ROOT);
            if (n.contains("ttyacm") || n.contains("ttyusb")) return true;
        }
        if (description != null) {
            String d = description.toLowerCase(Locale.ROOT);
            return d.contains("ch340") || d.contains("cp210") || d.contains("ftdi")
                    || d.contains("usb serial") || d.contains("usb-serial")
                    || d.contains("acm") || d.contains("uart");
        }
        return false;
    }
}
