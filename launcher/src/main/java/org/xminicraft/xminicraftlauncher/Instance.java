package org.xminicraft.xminicraftlauncher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xminicraft.xminicraftlauncher.util.Signal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Instance {
    public String name = "";
    public String version = "";
    public String notes = "";

    public boolean javaInstallation;
    public Path javaInstallationPath;

    public boolean overrideEnvironmentVariables;
    public List<String[]> environmentVariables = new ArrayList<>();

    public boolean overrideJvmArguments;
    public String jvmArgs = "";

    public boolean overrideJavaMemory;
    public int javaMinimumMemoryAllocation;
    public int javaMaximumMemoryAllocation;

    public long lastLaunchTime;
    public long totalTimePlayed;

    public Path path;
    public String groupName;
    public final Property<Boolean> isRunning = new Property<>(false);
    public volatile Process runningProcess;

    public Instance fromJson(JsonNode rootNode) {
        this.name = rootNode.path("name").asText();
        this.version = rootNode.path("version").asText("minicraft_original");
        this.overrideEnvironmentVariables = rootNode.path("override_environment_variables").asBoolean(false);
        this.overrideJvmArguments = rootNode.path("override_jvm_arguments").asBoolean(false);
        this.jvmArgs = rootNode.path("jvm_args").asText("");
        this.overrideJavaMemory = rootNode.path("override_java_memory").asBoolean(false);
        this.javaMinimumMemoryAllocation = rootNode.path("java_minimum_memory_allocation").asInt(512);
        this.javaMaximumMemoryAllocation = rootNode.path("java_maximum_memory_allocation").asInt(2048);

        this.javaInstallation = rootNode.path("java_installation").asBoolean(false);
        if (rootNode.has("java_installation_path")) {
            this.javaInstallationPath = Paths.get(rootNode.path("java_installation_path").asText());
            if (!Files.exists(this.javaInstallationPath)) {
                this.javaInstallationPath = null;
            }
        }

        JsonNode envVarsNode = rootNode.path("environment_variables");
        if (envVarsNode.isArray()) {
            for (JsonNode node : envVarsNode) {
                if (node.isArray() && node.size() == 2) {
                    this.environmentVariables.add(new String[]{node.get(0).asText(), node.get(1).asText()});
                }
            }
        }
        return this;
    }

    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("format_version", 1);

        rootNode.put("name", this.name);
        rootNode.put("version", this.version);
        rootNode.put("notes", this.notes);

        rootNode.put("override_environment_variables", this.overrideEnvironmentVariables);
        rootNode.set("environment_variables", mapper.convertValue(this.environmentVariables, ArrayNode.class));

        rootNode.put("override_jvm_arguments", this.overrideJvmArguments);
        rootNode.put("jvm_arguments", this.jvmArgs);

        rootNode.put("java_installation", this.javaInstallation);
        if (this.javaInstallationPath != null) {
            rootNode.put("java_installation_path", this.javaInstallationPath.toString());
        }

        rootNode.put("last_launch_time", this.lastLaunchTime);
        rootNode.put("total_time_played", this.totalTimePlayed);
        return rootNode.toString();
    }

    public static class Property<T> {
        public final Signal<T> signal = new Signal<>();
        private T value;

        public Property(T value) {
            this.value = value;
        }

        public T get() {
            return this.value;
        }

        public void set(T value) {
            T oldValue = this.value;
            this.value = value;
            if (value != oldValue) this.signal.emit(value);
        }
    }
}
