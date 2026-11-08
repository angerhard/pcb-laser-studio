package de.andreasgerhard.pcblaserstudio.model;

/**
 * Describes which axis the physical PCB is flipped around when turning it
 * over to engrave the back side. The flip is assumed to happen in place
 * (the board's bounding box does not move), i.e. rotating 180° around the
 * board's own centerline.
 */
public enum FlipAxis {
    /** Flip around a vertical centerline (mirrors X / left-right, like turning a book page). */
    VERTICAL("Vertical (left/right mirror)"),
    /** Flip around a horizontal centerline (mirrors Y / top-bottom, like flipping a pancake). */
    HORIZONTAL("Horizontal (top/bottom mirror)");

    private final String displayName;

    FlipAxis(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}
