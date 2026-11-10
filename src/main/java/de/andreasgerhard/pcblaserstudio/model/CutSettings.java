package de.andreasgerhard.pcblaserstudio.model;

/**
 * Parameters for cutting the PCB out along the SVG border.
 * Stored per project so the last-used values are remembered.
 */
public class CutSettings {
    /** Laser power for the cut contour, in percent (0-100). */
    private double powerPercent = 100.0;
    /** Feed rate for the cut contour, in mm/min. */
    private int feedRateMmPerMin = 300;
    /** Total number of times the cut contour is traced. */
    private int repeats = 1;

    public CutSettings() {}

    public CutSettings(CutSettings other) {
        this.powerPercent = other.powerPercent;
        this.feedRateMmPerMin = other.feedRateMmPerMin;
        this.repeats = other.repeats;
    }

    public double getPowerPercent() { return powerPercent; }
    public void setPowerPercent(double powerPercent) { this.powerPercent = powerPercent; }

    public int getFeedRateMmPerMin() { return feedRateMmPerMin; }
    public void setFeedRateMmPerMin(int feedRateMmPerMin) { this.feedRateMmPerMin = feedRateMmPerMin; }

    public int getRepeats() { return repeats; }
    public void setRepeats(int repeats) { this.repeats = repeats; }
}
