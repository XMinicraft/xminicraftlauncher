package org.xminicraft.xminicraftlauncher;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xminicraft.xminicraftlauncher.util.FileUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Settings {
    public String theme = "Dark";
    public String language = "en_us";

    @JsonProperty("instances_folder")
    public String instancesFolder = "";
    @JsonProperty("java_folder")
    public String javaFolder = "";
    @JsonProperty("icons_folder")
    public String iconsFolder = "";

    @JsonProperty("java_memory")
    public boolean javaMemory;

    @JsonProperty("java_minimum_allocation")
    public int javaMinimumAlloc = 512;
    @JsonProperty("java_maximum_allocation")
    public int javaMaximumAlloc = 2048;

    @JsonProperty("environment_variables")
    public List<String[]> envVars = new ArrayList<>();

    public static Settings load(Path path) {
        if (!Files.exists(path)) {
            return new Settings();
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            Settings settings = mapper.readValue(FileUtils.readLines(path, StandardCharsets.UTF_8), Settings.class);

            if (!"en_us".equals(settings.language) && !"pt_pt".equals(settings.language)) {
                settings.language = "en_us";
            }

            if (!"Dark".equals(settings.theme) && !"Light".equals(settings.theme)) {
                settings.theme = "Dark";
            }

            if (settings.javaMaximumAlloc < settings.javaMinimumAlloc) {
                settings.javaMaximumAlloc = settings.javaMinimumAlloc;
            }

            if (settings.javaMaximumAlloc == 0) {
                settings.javaMaximumAlloc = 2048;
            }

            if (!settings.instancesFolder.isEmpty() && !Files.exists(Paths.get(settings.instancesFolder))) {
                settings.instancesFolder = "";
            }

            if (!settings.javaFolder.isEmpty() && !Files.exists(Paths.get(settings.javaFolder))) {
                settings.javaFolder = "";
            }

            if (!settings.iconsFolder.isEmpty() && !Files.exists(Paths.get(settings.iconsFolder))) {
                settings.iconsFolder = "";
            }

            return settings;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Settings();
    }

    public void save(Path path) {
        ObjectMapper mapper = new ObjectMapper();

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(mapper.writeValueAsString(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
