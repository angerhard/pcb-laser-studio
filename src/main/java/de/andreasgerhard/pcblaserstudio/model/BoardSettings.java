package de.andreasgerhard.pcblaserstudio.model;

/**
 * Physical PCB board description shared by all layers of a project.
 * The board is the single source of truth for where each layer's SVG
 * content gets burned, so that front and back side engravings line up
 * exactly once the board is physically flipped.
 */
public class BoardSettings {
    private double widthMm = 100.0;
    private double heightMm = 80.0;
    /** Distance from the board edge to the SVG content on the anchored sides. */
    private double marginMm = 5.0;
    /** Axis the board is turned over around when switching from front to back. */
    private FlipAxis flipAxis = FlipAxis.VERTICAL;
    /** Whether back-side artwork should be mirrored by the app before burning. */
    private boolean mirrorBackArtwork = false;
    /** Whether a border matching the SVG's exact size/position should be lasered. */
    private boolean generateBorder = false;

    public BoardSettings() {}

    public BoardSettings(BoardSettings other) {
        this.widthMm = other.widthMm;
        this.heightMm = other.heightMm;
        this.marginMm = other.marginMm;
        this.flipAxis = other.flipAxis;
        this.mirrorBackArtwork = other.mirrorBackArtwork;
        this.generateBorder = other.generateBorder;
    }

    public double getWidthMm() { return widthMm; }
    public void setWidthMm(double widthMm) { this.widthMm = widthMm; }

    public double getHeightMm() { return heightMm; }
    public void setHeightMm(double heightMm) { this.heightMm = heightMm; }

    public double getMarginMm() { return marginMm; }
    public void setMarginMm(double marginMm) { this.marginMm = marginMm; }

    public FlipAxis getFlipAxis() { return flipAxis; }
    public void setFlipAxis(FlipAxis flipAxis) { this.flipAxis = flipAxis; }

    public boolean isMirrorBackArtwork() { return mirrorBackArtwork; }
    public void setMirrorBackArtwork(boolean mirrorBackArtwork) { this.mirrorBackArtwork = mirrorBackArtwork; }

    public boolean isGenerateBorder() { return generateBorder; }
    public void setGenerateBorder(boolean generateBorder) { this.generateBorder = generateBorder; }
}
