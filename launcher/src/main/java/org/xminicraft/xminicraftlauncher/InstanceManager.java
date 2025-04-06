package org.xminicraft.xminicraftlauncher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xminicraft.xminicraftlauncher.util.FileUtils;
import org.xminicraft.xminicraftlauncher.util.Signal;
import org.xminicraft.xminicraftlauncher.version.Version;
import org.xminicraft.xminicraftlauncher.version.VersionManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class InstanceManager {
    private final List<Instance> instances = new ArrayList<>();
    public final Signal<Void> loaded = new Signal<>();
    public final Set<String> groupNames = new HashSet<>();

    public void remove(Instance instance) {
        this.instances.remove(instance);
    }

    public List<Instance> getInstances() {
        return this.instances;
    }

    public List<String> getGroupList() {
        List<String> list = new ArrayList<>(this.groupNames);
        list.sort((a, b) -> {
            if ("<default>".equals(a)) return -1;
            return a.compareTo(b);
        });
        return list;
    }

    public List<Instance> getInstancesForGroup(String groupName) {
        return this.instances.stream().filter(it -> it.groupName.equals(groupName)).collect(Collectors.toList());
    }

    public void refresh() {
        this.instances.clear();
        this.load(XLauncher.getInstance().getInstancesPath(), XLauncher.getInstance().getVersionManager());
        this.loaded.emit();
    }

    public void save(Instance instance) {
        try {
            FileUtils.writeString(instance.path.resolve("instance.json"), instance.toJson(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveGroupsOnly() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("format_version", 1);

        ObjectNode groupsNode = mapper.createObjectNode();
        for (String groupName : this.groupNames) {
            if ("<default>".equals(groupName)) continue;

            ObjectNode groupNode = mapper.createObjectNode();
            ArrayNode instancesNode = mapper.createArrayNode();
            List<Instance> instances = this.getInstancesForGroup(groupName);

            for (Instance instance : instances) {
                instancesNode.add(instance.name);
            }

            if (!instances.isEmpty()) {
                groupNode.set("instances", instancesNode);
                groupsNode.set(groupName, groupNode);
            }
        }

        rootNode.set("groups", groupsNode);

        try {
            FileUtils.writeString(XLauncher.getInstance().getInstancesPath().resolve("instance_groups.json"), rootNode.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveGroups() {
        this.instances.forEach(this::save);

       this.saveGroupsOnly();
    }

    public Optional<Instance> findByName(String name) {
        if (name == null || name.isEmpty()) return Optional.empty();
        return this.instances.stream().filter(it -> it.name.equals(name)).findFirst();
    }

    public void launch(Instance instance) {
        this.launch(instance, true);
    }

    public void launch(Instance instance, boolean alone) {
        instance.isRunning.set(true);
        VersionManager versionManager = XLauncher.getInstance().getVersionManager();
        Optional<Version> version = versionManager.findVersion(instance.version);
        if (!version.isPresent()) return;

        ProcessBuilder builder = new ProcessBuilder();

        String javaPath = "java";
        if (instance.javaInstallation && instance.javaInstallationPath != null) {
            javaPath = instance.javaInstallationPath.toString();
        }

        int minAlloc = XLauncher.getInstance().getSettings().javaMinimumAlloc;
        int maxAlloc = XLauncher.getInstance().getSettings().javaMaximumAlloc;

        if (instance.overrideJavaMemory) {
            minAlloc = instance.javaMinimumMemoryAllocation;
            maxAlloc = instance.javaMaximumMemoryAllocation;
        }

        builder.command(
                javaPath,
                "-Xms" + minAlloc + "m",
                "-Xmx" + maxAlloc + "m",
                "-cp",
                Paths.get("versions/" + version.get().id + "/client.jar").toAbsolutePath().toString(),
                versionManager.getMainClass(version.get()), "--savedir",
                XLauncher.getInstance().getInstancesPath().resolve(instance.name).resolve("save_data").toAbsolutePath().toString());
        builder.directory(XLauncher.getInstance().getInstancesPath().resolve(instance.name).resolve("save_data").toFile());
        if (!alone) {
            builder.inheritIO();
        }
        try {
            if (alone) {
                new Thread(()->{
                    try {
                        Process process = builder.start();
                        instance.runningProcess = process;
                        instance.lastLaunchTime = Instant.now().getEpochSecond();

                        while (process.isAlive()) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        instance.isRunning.set(false);
                        instance.runningProcess = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                Process process = builder.start();
                instance.runningProcess = process;
                instance.lastLaunchTime = Instant.now().getEpochSecond();
                process.waitFor();
                instance.isRunning.set(false);
                instance.runningProcess = null;
            }
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void load(Path path, VersionManager versionManager) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            Path instancesGroupPath = path.resolve("instance_groups.json");
            Map<String, Set<String>> groups = new HashMap<>();

            this.groupNames.clear();
            this.groupNames.add("<default>");

            if (Files.exists(instancesGroupPath)) {
                JsonNode rootNode = mapper.readTree(FileUtils.readLines(instancesGroupPath, StandardCharsets.UTF_8));
                JsonNode groupsNode = rootNode.path("groups");

                for (Map.Entry<String, JsonNode> entry : groupsNode.properties()) {
                    this.groupNames.add(entry.getKey());

                    Set<String> instances = new HashSet<>();
                    JsonNode instancesNode = entry.getValue().path("instances");
                    for (JsonNode instanceNode : instancesNode) {
                        instances.add(instanceNode.asText());
                    }
                    groups.put(entry.getKey(), instances);
                }
            }

            Files.list(path).forEach(it -> {
                if (Files.isDirectory(it) && Files.exists(it.resolve("instance.json"))) {
                    try {
                        JsonNode rootNode = mapper.readTree(Files.newBufferedReader(it.resolve("instance.json"), StandardCharsets.UTF_8));
                        Instance instance = new Instance();
                        instance.path = it;

                        instance.fromJson(rootNode);

                        instance.groupName = "<default>";
                        for (Map.Entry<String, Set<String>> entry : groups.entrySet()) {
                            if (entry.getValue().contains(instance.name)) {
                                instance.groupName = entry.getKey();
                                break;
                            }
                        }
                        this.instances.add(instance);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
