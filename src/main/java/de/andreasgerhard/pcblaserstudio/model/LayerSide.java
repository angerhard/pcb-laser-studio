package de.andreasgerhard.pcblaserstudio.model;

public enum LayerSide {
    FRONT("Front"),
    BACK("Back");

    private final String displayName;

    LayerSide(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}

