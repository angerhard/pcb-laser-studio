package de.andreasgerhard.pcblaserstudio.model;

public class Layer {
    private String id;
    private String name;
    private LayerType type;
    private LayerSide side;
    private boolean enabled = true;
    private String svgData;           // base64-encoded SVG
    private String svgFilePath;       // original file path (transient)
    private ProcessingMode processingMode = ProcessingMode.BLACK_AREA;
    private String gcodePrefix = "G00\nG17\nG40\nG21\nG54\nG90\nM4\nM9\n";
    private String gcodeSuffix = "G1\nS0\nM5\nG0\nX0Y0\nM2\n";
    private LaserSettings laserSettings;
    private boolean dirty = false;    // unsaved changes

    public Layer() {}

    public Layer(String id, String name, LayerType type, LayerSide side) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.side = side;
        this.laserSettings = new LaserSettings();
        if (type == LayerType.DOCUMENTATION) {
            this.laserSettings.setPowerPercent(50);
            this.processingMode = ProcessingMode.WHITE_AREA;
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.dirty = true;
    }

    public LayerType getType() { return type; }
    public void setType(LayerType type) { this.type = type; }

    public LayerSide getSide() { return side; }
    public void setSide(LayerSide side) { this.side = side; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.dirty = true;
    }

    public String getSvgData() { return svgData; }
    public void setSvgData(String svgData) {
        this.svgData = svgData;
        this.dirty = true;
    }

    public String getSvgFilePath() { return svgFilePath; }
    public void setSvgFilePath(String svgFilePath) { this.svgFilePath = svgFilePath; }

    public ProcessingMode getProcessingMode() { return processingMode; }
    public void setProcessingMode(ProcessingMode processingMode) {
        this.processingMode = processingMode;
        this.dirty = true;
    }

    public String getGcodePrefix() { return gcodePrefix; }
    public void setGcodePrefix(String gcodePrefix) {
        this.gcodePrefix = gcodePrefix;
        this.dirty = true;
    }

    public String getGcodeSuffix() { return gcodeSuffix; }
    public void setGcodeSuffix(String gcodeSuffix) {
        this.gcodeSuffix = gcodeSuffix;
        this.dirty = true;
    }

    public LaserSettings getLaserSettings() { return laserSettings; }
    public void setLaserSettings(LaserSettings laserSettings) { this.laserSettings = laserSettings; }

    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }

    public boolean hasSvg() { return svgData != null && !svgData.isEmpty(); }
}

