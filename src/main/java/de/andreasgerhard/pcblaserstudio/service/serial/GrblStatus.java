package de.andreasgerhard.pcblaserstudio.service.serial;

import java.util.Locale;

/**
 * Parsed GRBL status report, e.g.
 * {@code <Idle|MPos:0.000,0.000,0.000|FS:0,0>}.
 *
 * @param state   machine state, e.g. {@code Idle}, {@code Run}, {@code Hold}, {@code Alarm}
 * @param mpos    machine position in mm (x, y, z), or {@code null} if absent
 * @param wpos    work position in mm (x, y, z), or {@code null} if absent
 * @param feed    current feed rate in mm/min, or {@link Double#NaN} if absent
 * @param spindle current spindle/laser value (S), or {@link Double#NaN} if absent
 * @param raw     the original line
 */
public record GrblStatus(
        String state,
        double[] mpos,
        double[] wpos,
        double feed,
        double spindle,
        String raw
) {

    /** @return a parsed status, or {@code null} if {@code line} is not a status report. */
    public static GrblStatus parse(String line) {
        if (line == null) return null;
        String s = line.trim();
        if (s.length() < 2 || s.charAt(0) != '<' || s.charAt(s.length() - 1) != '>') return null;
        s = s.substring(1, s.length() - 1);

        String[] parts = s.split("\\|");
        String state = parts.length > 0 && !parts[0].isBlank() ? parts[0] : "Unknown";
        double[] mpos = null;
        double[] wpos = null;
        double feed = Double.NaN;
        double spindle = Double.NaN;

        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (p.startsWith("MPos:")) {
                mpos = parseVector(p.substring(5));
            } else if (p.startsWith("WPos:")) {
                wpos = parseVector(p.substring(5));
            } else if (p.startsWith("FS:")) {
                String[] fs = p.substring(3).split(",");
                if (fs.length > 0) feed = parseDouble(fs[0]);
                if (fs.length > 1) spindle = parseDouble(fs[1]);
            }
        }
        return new GrblStatus(state, mpos, wpos, feed, spindle, line.trim());
    }

    private static double[] parseVector(String csv) {
        String[] v = csv.split(",");
        double[] out = new double[v.length];
        for (int i = 0; i < v.length; i++) out[i] = parseDouble(v[i]);
        return out;
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** @return a short human-readable position string, or {@code "-"} if unknown. */
    public String positionText() {
        double[] p = wpos != null ? wpos : mpos;
        if (p == null || p.length < 2) return "-";
        return String.format(Locale.ENGLISH, "X %.2f  Y %.2f", p[0], p[1]);
    }

    public String feedText() {
        if (Double.isNaN(feed)) return "-";
        return String.format(Locale.ENGLISH, "%.0f mm/min", feed);
    }

    public String laserText() {
        if (Double.isNaN(spindle)) return "-";
        return String.format(Locale.ENGLISH, "S%.0f", spindle);
    }
}
