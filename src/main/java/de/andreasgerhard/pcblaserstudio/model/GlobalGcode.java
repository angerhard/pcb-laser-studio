package de.andreasgerhard.pcblaserstudio.model;

public class GlobalGcode {
    private String prefix = "G00\nG17\nG40\nG21\nG54\nG90\nM4\nM9\n";
    private String suffix = "G1\nS0\nM5\nG0\nX0Y0\nM2\n";

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
}

