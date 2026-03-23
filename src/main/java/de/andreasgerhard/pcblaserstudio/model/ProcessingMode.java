package de.andreasgerhard.pcblaserstudio.model;

public enum ProcessingMode {
    BLACK_AREA("Remove Black"),
    WHITE_AREA("Remove White");

    private final String displayName;

    ProcessingMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}

