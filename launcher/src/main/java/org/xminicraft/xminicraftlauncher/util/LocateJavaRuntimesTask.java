package org.xminicraft.xminicraftlauncher.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xminicraft.xminicraftlauncher.XLauncher;
import org.xminicraft.xminicraftlauncher.java.LocalJavaRuntime;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class LocateJavaRuntimesTask extends Task {
    private final boolean forceCompleteRefresh;
    private final Consumer<List<LocalJavaRuntime>> onFinished;

    public LocateJavaRuntimesTask(boolean forceCompleteRefresh, Consumer<List<LocalJavaRuntime>> onFinished) {
        this.forceCompleteRefresh = forceCompleteRefresh;
        this.onFinished = onFinished;
    }

    @Override
    public String getTitle() {
        return "Locating Local Java Runtimes";
    }

    @Override
    public void run() {
        List<LocalJavaRuntime> list = new ArrayList<>();

        Set<Path> paths = new HashSet<>();
        paths.add(Paths.get(System.getProperty("java.home"), "bin", OperatingSystem.get().getJavaExecutableName()));
        paths.addAll(getJavaInFolders( System.getenv("PATH").split(File.pathSeparator), OperatingSystem.get().getJavaExecutableName(), Files::exists));
        paths.addAll(findJavaInFolder("C:/Program Files/Java", OperatingSystem.get().getJavaExecutableName()));

        String home = System.getenv("USERPROFILE");
        paths.addAll(findJavaInFolder(home + "/.jdks", OperatingSystem.get().getJavaExecutableName()));
        paths.addAll(findJavaInFolder(home + "/.gradle/jdks", OperatingSystem.get().getJavaExecutableName()));
        paths.addAll(findJavaInFolder(XLauncher.getInstance().getJavaInstallationsPath().toAbsolutePath().toString(), OperatingSystem.get().getJavaExecutableName()));

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode rootNode = null;
        Path cachedJavaRuntimesInfo = XLauncher.getInstance().getCachePath().resolve("java_runtimes_info.json");
        if (Files.exists(cachedJavaRuntimesInfo)) {
            if (this.forceCompleteRefresh) {
                try {
                    Files.deleteIfExists(cachedJavaRuntimesInfo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    rootNode = (ObjectNode) objectMapper.readTree(FileUtils.readLines(cachedJavaRuntimesInfo, StandardCharsets.UTF_8));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }

        if (rootNode == null) {
            rootNode = objectMapper.createObjectNode();
        }

        int i = 0;
        for (Path p : paths) {
            this.setStatus("Found " + p);
            this.setProgress((int) (i / (double) paths.size() * 100), 100);

            if (rootNode.has(p.toAbsolutePath().toString())) {
                JsonNode infoNode = rootNode.path(p.toAbsolutePath().toString());
                LocalJavaRuntime runtime = new LocalJavaRuntime();
                runtime.version = infoNode.path("java.version").asText();
                runtime.architecture = infoNode.path("os.arch").asText();
                runtime.path = p;
                list.add(runtime);
                ++i;
                continue;
            }

            ProcessBuilder builder = new ProcessBuilder();
            builder.command(p.toAbsolutePath().toString(), "-jar", XLauncher.getInstance().getLibrariesPath().resolve("JavaCheck.jar").toAbsolutePath().toString());
            try {
                Process process = builder.start();
                String result = FileUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
                LocalJavaRuntime runtime = new LocalJavaRuntime();
                runtime.path = p;

                for (String line : result.split("\n")) {
                    String[] prop = line.split("=");
                    if (prop.length < 2) continue;

                    if ("java.version".equals(prop[0])) {
                        runtime.version = prop[1];
                    } else if ("os.arch".equals(prop[0])) {
                        runtime.architecture = prop[1];
                    }
                }

                ObjectNode infoNode = objectMapper.createObjectNode();
                infoNode.put("java.version", runtime.version);
                infoNode.put("os.arch", runtime.architecture);
                rootNode.set(p.toAbsolutePath().toString(), infoNode);

                list.add(runtime);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ++i;
        }

        try {
            FileUtils.writeString(cachedJavaRuntimesInfo, rootNode.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.onFinished.accept(list);
        this.finished.emit();
    }

    public static List<Path> findJavaInFolder(String folder, String exec) {
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> stream = Files.list(Paths.get(folder))) {
            stream.forEach(it -> {
                if (Files.exists(it.resolve("bin").resolve(exec))) {
                    paths.add(it.resolve("bin").resolve(exec));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return paths;
    }

    public static List<Path> getJavaInFolders(String[] folders, String exec, Predicate<Path> verifier) {
        List<Path> paths = new ArrayList<>();

        for (String bin : folders) {
            Path path;

            try {
                path = Paths.get(bin, exec);
            } catch (InvalidPathException e) {
                e.printStackTrace();
                continue;
            }

            if (verifier.test(path)) {
                paths.add(path);
            }
        }

        return paths;
    }
}
