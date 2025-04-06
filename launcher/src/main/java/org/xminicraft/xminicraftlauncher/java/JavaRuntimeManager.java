package org.xminicraft.xminicraftlauncher.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xminicraft.xminicraftlauncher.Main;
import org.xminicraft.xminicraftlauncher.XLauncher;
import org.xminicraft.xminicraftlauncher.util.DownloadUtils;
import org.xminicraft.xminicraftlauncher.util.LocateJavaRuntimesTask;
import org.xminicraft.xminicraftlauncher.util.OperatingSystem;
import org.xminicraft.xminicraftlauncher.util.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JavaRuntimeManager {
    public final Map<String, JavaRuntimeSource> sources = new HashMap<>();
    private final List<LocalJavaRuntime> runtimes = new ArrayList<>();

    public List<LocalJavaRuntime> getDownloaded() {
        return this.runtimes;
    }

    public Task refresh(boolean forceCompleteRefresh) {
        System.out.println("Locating java runtimes...");

        return new LocateJavaRuntimesTask(forceCompleteRefresh, list -> {
            this.runtimes.clear();
            this.runtimes.addAll(list);
            this.runtimes.sort((a, b) -> {
                if (a.path.startsWith(XLauncher.getInstance().getJavaInstallationsPath().toAbsolutePath())) return -1;
                return 1;
            });
        });
    }

    public void load() {
        Path javaCheckerJarPath = XLauncher.getInstance().getLibrariesPath().resolve("JavaCheck.jar");

        if (!Files.exists(javaCheckerJarPath)) {
            try {
                InputStream is = Main.class.getResourceAsStream("/META-INF/JavaCheck.jar");
                if (is != null) {
                    Files.copy(is, javaCheckerJarPath);
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        DownloadUtils.download(URI.create("https://cdn.jsdelivr.net/gh/XMinicraft/xmeta@master/java_runtimes.json"), Paths.get("meta/java_runtimes.json"));

        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get("meta/java_runtimes.json"), StandardCharsets.UTF_8)) {
            JsonNode rootNode = mapper.readTree(reader);
            JsonNode sourcesNode = rootNode.get("sources");

            String[] sourceNames = {"Azul Zulu", "Adoptium", "Bell Soft"};
            String[] sourceIds = {"com.azul.zulu", "net.adoptium", "bellsoft.liberica"};

            int i = 0;
            for (String sourceName : sourceIds) {
                JsonNode sourceNode = sourcesNode.get(sourceName);

                Map<Integer, List<JavaRuntime>> runtimes = new HashMap<>();
                List<String> availableReleases = new ArrayList<>();

                for (JsonNode sourceVersionsNode : sourceNode) {
                    String versionStr = sourceVersionsNode.get("version").asText();
                    int version = Integer.parseInt(versionStr.substring(4));

                    availableReleases.add(String.valueOf(version));
                    List<JavaRuntime> runtimeList = new ArrayList<>();

                    for (JsonNode runtimeNode : sourceVersionsNode.get("runtimes")) {
                        String packageType = runtimeNode.path("package_type").asText();
                        JsonNode releaseTime = runtimeNode.path("release_time");
                        String url = runtimeNode.path("url").asText();
                        int size = runtimeNode.path("size").asInt(0);
                        String jVersion = runtimeNode.path("version").asText();

                        String[] runtimeOs = runtimeNode.path("runtime_os").asText().split("-");
                        String arch = OperatingSystem.get().arch();

                        if (Objects.equals(runtimeOs[0], OperatingSystem.get().name)) {
                            boolean add = false;
                            if ("i686".equals(runtimeOs[1])) {
                                if ("x86".equals(arch) || "arm".equals(arch)) {
                                    add = true;
                                }
                            } else if ("aarch64".equals(runtimeOs[1])) {
                                if ("aarch64".equals(arch)) {
                                    add = true;
                                }
                            } else if ("x64".equals(runtimeOs[1])) {
                                if (!"aarch64".equals(arch) && !"x86".equals(arch) && !"arm".equals(arch)) {
                                    add = true;
                                }
                            }

                            if (add) {
                                String name = ("com.azul.zulu".equals(sourceName) ? "azul_zulu_" : "net.adoptium".equals(sourceName) ? "eclipse_temurin_" : "bell_soft_") + "jre" + jVersion;
                                runtimeList.add(new JavaRuntime(name, releaseTime.isMissingNode() ? null : ZonedDateTime.parse(releaseTime.asText()), url, size, String.join("-", runtimeOs), jVersion));
                            }
                        }
                    }

                    runtimes.put(version, runtimeList);
                }

                availableReleases.sort(Comparator.comparingInt(Integer::parseInt));
                JavaRuntimeSource source = new JavaRuntimeSource(sourceName, sourceNames[i], runtimes, availableReleases);
                this.sources.put(sourceName, source);
                ++i;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
