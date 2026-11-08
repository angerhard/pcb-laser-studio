package de.andreasgerhard.pcblaserstudio.model;

import java.util.ArrayList;
import java.util.List;

public class Project {
    private String version = "1.0";
    private ProjectMetadata metadata;
    private LaserSettings laserSettings;
    private BoardSettings boardSettings;
    private List<Layer> layers;
    private GlobalGcode globalGcode;
    private String filePath; // transient - not serialized

    public Project() {
        this.metadata = new ProjectMetadata();
        this.laserSettings = new LaserSettings();
        this.boardSettings = new BoardSettings();
        this.globalGcode = new GlobalGcode();
        this.layers = new ArrayList<>();
        initDefaultLayers();
    }

    private void initDefaultLayers() {
        layers.add(new Layer("etch_front", "Etching - Front Side", LayerType.ETCH, LayerSide.FRONT));
        layers.add(new Layer("etch_back", "Etching - Back Side", LayerType.ETCH, LayerSide.BACK));
        layers.add(new Layer("doc_front", "Documentation - Front Side", LayerType.DOCUMENTATION, LayerSide.FRONT));
        layers.add(new Layer("doc_back", "Documentation - Back Side", LayerType.DOCUMENTATION, LayerSide.BACK));
        layers.get(2).setEnabled(false);
        layers.get(3).setEnabled(false);
    }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public ProjectMetadata getMetadata() { return metadata; }
    public void setMetadata(ProjectMetadata metadata) { this.metadata = metadata; }

    public LaserSettings getLaserSettings() { return laserSettings; }
    public void setLaserSettings(LaserSettings laserSettings) { this.laserSettings = laserSettings; }

    public BoardSettings getBoardSettings() { return boardSettings; }
    public void setBoardSettings(BoardSettings boardSettings) { this.boardSettings = boardSettings; }

    public List<Layer> getLayers() { return layers; }
    public void setLayers(List<Layer> layers) { this.layers = layers; }

    public GlobalGcode getGlobalGcode() { return globalGcode; }
    public void setGlobalGcode(GlobalGcode globalGcode) { this.globalGcode = globalGcode; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public boolean isDirty() {
        return layers.stream().anyMatch(Layer::isDirty);
    }

    public void clearDirty() {
        layers.forEach(l -> l.setDirty(false));
    }

    public String getDisplayName() {
        String base = metadata != null ? metadata.getName() : "New Project";
        return filePath != null ? base : base + " *";
    }
}

