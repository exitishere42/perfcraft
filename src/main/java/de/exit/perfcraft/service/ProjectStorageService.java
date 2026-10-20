package de.exit.perfcraft.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.exit.perfcraft.model.PerfProject;

import java.io.File;
import java.io.IOException;

public class ProjectStorageService {

    private final ObjectMapper mapper;

    public ProjectStorageService() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void save(PerfProject project, File file) throws IOException {
        if (project == null || file == null) {
            throw new IllegalArgumentException("Project and file must not be null");
        }
        mapper.writeValue(file, project);
    }

    public PerfProject load(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("File does not exist: " + file);
        }
        return mapper.readValue(file, PerfProject.class);
    }

    public String toJsonString(PerfProject project) throws IOException {
        return mapper.writeValueAsString(project);
    }

    public PerfProject fromJsonString(String json) throws IOException {
        return mapper.readValue(json, PerfProject.class);
    }
}
