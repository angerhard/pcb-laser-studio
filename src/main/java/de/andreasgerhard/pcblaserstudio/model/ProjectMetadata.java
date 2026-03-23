package de.andreasgerhard.pcblaserstudio.model;

import java.time.LocalDateTime;

public class ProjectMetadata {
    private String name = "New Project";
    private String created;
    private String modified;
    private String author = System.getProperty("user.name", "Unknown");

    public ProjectMetadata() {
        String now = LocalDateTime.now().toString();
        this.created = now;
        this.modified = now;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCreated() { return created; }
    public void setCreated(String created) { this.created = created; }

    public String getModified() { return modified; }
    public void setModified(String modified) { this.modified = modified; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public void updateModified() {
        this.modified = LocalDateTime.now().toString();
    }
}

