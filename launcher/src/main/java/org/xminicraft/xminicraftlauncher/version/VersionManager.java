package org.xminicraft.xminicraftlauncher.version;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xminicraft.xminicraftlauncher.XLauncher;
import org.xminicraft.xminicraftlauncher.util.DownloadUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VersionManager {
    public List<Version> versions;
    public Map<Version, VersionMetadata> manifests = new HashMap<>();

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_MAIN_CLASS = "com.mojang.ld22.Game";

    public VersionManager() {
        this.versions = getVersions(XLauncher.getInstance().getMetaPath().resolve("version_manifest.json"));
    }

    public Optional<Version> findVersion(String id) {
        return this.versions.stream().filter(it -> it.id.equals(id)).findFirst();
    }

    public Optional<VersionMetadata> getManifest(Version version) {
        this.loadManifest(version);
        return Optional.ofNullable(this.manifests.get(version));
    }

    public void loadManifest(Version version) {
        if (this.manifests.containsKey(version)) return;
        Path metadataPath = XLauncher.getInstance().getVersionsPath().resolve(version.id).resolve("metadata.json");
        try {
            JsonNode rootNode = MAPPER.readTree(Files.newBufferedReader(metadataPath));
            Version.Type type = "java".equals(rootNode.path("type").asText("")) ? Version.Type.JAVA : Version.Type.EXECUTABLE;
            this.manifests.put(version, new VersionMetadata(
                    type,
                    rootNode.path("id").asText(),
                    ZonedDateTime.parse(rootNode.path("release_time").asText()),
                    rootNode.at("/java/version").asInt(8),
                    rootNode.path("main_class").asText(),
                    URI.create(rootNode.path("downloads").path("client").path("url").asText())));
        } catch (IOException e) {
            System.err.println("Failed to read metadata for version " + version.id + ": " + e.getMessage());
        }
    }

    public String getMainClass(Version version) {
        return this.getManifest(version).map(manifest -> manifest.mainClass).orElse(DEFAULT_MAIN_CLASS);
    }

    public static List<Version> getVersions(Path manifestPath) {
        if (!DownloadUtils.download(URI.create("https://cdn.jsdelivr.net/gh/XMinicraft/xmeta@master/version_manifest.json"), XLauncher.getInstance().getMetaPath().resolve("version_manifest.json"))) {
        }

        List<Version> versions = new ArrayList<>();
        try {
            JsonNode rootNode = MAPPER.readTree(Files.newBufferedReader(manifestPath));
            JsonNode versionsNode = rootNode.path("versions");

            if (versionsNode.isMissingNode()) {
                System.err.println("No 'versions' node found in version manifest");
                return versions;
            }

            for (JsonNode versionNode : versionsNode) {
                try {
                    Version.Type type = "java".equals(versionNode.path("type").asText("")) ? Version.Type.JAVA : Version.Type.EXECUTABLE;
                    versions.add(new Version(
                            versionNode.path("id").asText(),
                            type,
                            versionNode.path("url").asText(),
                            ZonedDateTime.parse(versionNode.path("release_time").asText())
                    ));
                } catch (Exception e) {
                    System.err.println("Failed to parse version: " + versionNode + " - " + e.getMessage());
                }
            }

            versions.sort((a, b) -> b.releaseTime.compareTo(a.releaseTime));
        } catch (IOException e) {
            System.err.println("Failed to read version manifest: " + e.getMessage());
        }
        return versions;
    }
}
