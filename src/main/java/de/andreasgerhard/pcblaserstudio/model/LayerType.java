package de.andreasgerhard.pcblaserstudio.model;

public enum LayerType {
    ETCH("Etch"),
    DOCUMENTATION("Documentation");

    private final String displayName;

    LayerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}

