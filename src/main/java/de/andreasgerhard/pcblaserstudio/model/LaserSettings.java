package de.andreasgerhard.pcblaserstudio.model;

public class LaserSettings {
    private double radiusMm = 0.2;
    private double stepoverMm = 0.1;
    private double feedRateMmPerMin = 3000;
    private double powerPercent = 75;
    private int defaultDpi = 1000;
    /** Offset of the lower-left corner of the engraving in mm (X axis). */
    private double offsetXMm = 0.0;
    /** Offset of the lower-left corner of the engraving in mm (Y axis). */
    private double offsetYMm = 0.0;
    /** Whether to generate a contour (outline) laser pass before the fill pass. */
    private boolean generateContour = true;
    /** Feed rate for the contour pass (mm/min); independent of the fill feed rate. */
    private double contourFeedRateMmPerMin = 3000;
    /** Laser power for the contour pass (0–100 %); independent of the fill power. */
    private double contourPowerPercent = 75;

    public LaserSettings() {}

    public LaserSettings(LaserSettings other) {
        this.radiusMm = other.radiusMm;
        this.stepoverMm = other.stepoverMm;
        this.feedRateMmPerMin = other.feedRateMmPerMin;
        this.powerPercent = other.powerPercent;
        this.defaultDpi = other.defaultDpi;
        this.offsetXMm = other.offsetXMm;
        this.offsetYMm = other.offsetYMm;
        this.generateContour = other.generateContour;
        this.contourFeedRateMmPerMin = other.contourFeedRateMmPerMin;
        this.contourPowerPercent = other.contourPowerPercent;
    }

    public double getRadiusMm() { return radiusMm; }
    public void setRadiusMm(double radiusMm) { this.radiusMm = radiusMm; }

    public double getStepoverMm() { return stepoverMm; }
    public void setStepoverMm(double stepoverMm) { this.stepoverMm = stepoverMm; }

    public double getFeedRateMmPerMin() { return feedRateMmPerMin; }
    public void setFeedRateMmPerMin(double feedRateMmPerMin) { this.feedRateMmPerMin = feedRateMmPerMin; }

    public double getPowerPercent() { return powerPercent; }
    public void setPowerPercent(double powerPercent) { this.powerPercent = powerPercent; }

    public int getDefaultDpi() { return defaultDpi; }
    public void setDefaultDpi(int defaultDpi) { this.defaultDpi = defaultDpi; }

    public double getOffsetXMm() { return offsetXMm; }
    public void setOffsetXMm(double offsetXMm) { this.offsetXMm = offsetXMm; }

    public double getOffsetYMm() { return offsetYMm; }
    public void setOffsetYMm(double offsetYMm) { this.offsetYMm = offsetYMm; }

    public boolean isGenerateContour() { return generateContour; }
    public void setGenerateContour(boolean generateContour) { this.generateContour = generateContour; }

    public double getContourFeedRateMmPerMin() { return contourFeedRateMmPerMin; }
    public void setContourFeedRateMmPerMin(double contourFeedRateMmPerMin) { this.contourFeedRateMmPerMin = contourFeedRateMmPerMin; }

    public double getContourPowerPercent() { return contourPowerPercent; }
    public void setContourPowerPercent(double contourPowerPercent) { this.contourPowerPercent = contourPowerPercent; }
}

