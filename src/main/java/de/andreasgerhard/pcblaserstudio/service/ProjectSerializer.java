package de.andreasgerhard.pcblaserstudio.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.andreasgerhard.pcblaserstudio.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Saves and loads PCB Laser projects as JSON (.pcblaser files).
 */
public class ProjectSerializer {

    private static final Logger log = LoggerFactory.getLogger(ProjectSerializer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static void save(Project project, File file) throws IOException {
        project.getMetadata().updateModified();
        MAPPER.writeValue(file, project);
        project.setFilePath(file.getAbsolutePath());
        project.clearDirty();
        log.info("Project saved to {}", file.getAbsolutePath());
    }

    public static Project load(File file) throws IOException {
        Project project = MAPPER.readValue(file, Project.class);
        project.setFilePath(file.getAbsolutePath());
        project.clearDirty();
        log.info("Project loaded from {}", file.getAbsolutePath());
        return project;
    }
}

