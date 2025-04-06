package org.xminicraft.xminicraftlauncher.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xminicraft.xminicraftlauncher.Instance;
import org.xminicraft.xminicraftlauncher.Language;
import org.xminicraft.xminicraftlauncher.XLauncher;
import org.xminicraft.xminicraftlauncher.version.Version;
import org.xminicraft.xminicraftlauncher.version.VersionMetadata;

import javax.swing.*;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Optional;

public class InstanceCreateTask extends Task {
    private final String name;
    private final String groupName;
    private final Version version;

    public InstanceCreateTask(String name, String groupName, Version version) {
        this.name = name;
        this.groupName = groupName;
        this.version = version;
    }

    @Override
    public String getTitle() {
        return Language.translate("Creating Instance");
    }

    @Override
    public void run() {
        try {
            this.setStatus("Creating folder");
            this.setProgress(0, 100);
            Path instancePath = XLauncher.getInstance().getInstancesPath().resolve(this.name);
            while (Files.exists(instancePath)) {
                instancePath = instancePath.getParent().resolve(instancePath.getFileName().toString() + " (1)");
            }

            FileUtils.ensureDirectory(instancePath.resolve("save_data"));

            if (this.aborted) {
                Files.deleteIfExists(instancePath);
                this.abortFinished.emit();
                return;
            }

            this.setStatus("Ensuring game version metadata");
            this.setProgress(25, 100);
            Path versionPath = XLauncher.getInstance().getVersionsPath().resolve(this.version.id);
            Path versionJarPath = versionPath.resolve("client.jar");
            Path versionInfoPath = versionPath.resolve("metadata.json");

            FileUtils.ensureDirectory(versionPath);
            DownloadUtils.download(URI.create(this.version.url), versionInfoPath);

            Optional<VersionMetadata> metadata = XLauncher.getInstance().getVersionManager().getManifest(this.version);
            if (this.aborted) {
                this.abortFinished.emit();
                return;
            }

            if (metadata.isPresent()) {
                if (this.version.type == Version.Type.JAVA) {
                    this.setStatus("Ensuring game version jar");
                    this.setProgress(50, 100);
                    if (!Files.exists(versionJarPath)) {
                        this.setProgress(0, 100);
                        this.setStatus("Downloading game jar...");
                        DownloadUtils.download(metadata.get().clientJarUri, -1, versionJarPath, () -> !this.aborted, (prog, current, total) -> {
                            this.setStatus("Downloading game jar: " + formatBytes(current) + "/" + formatBytes(total));
                            this.setProgress((int) (prog * 100), 100);
                        });

                        if (this.aborted) {
                            if (Files.exists(versionJarPath)) {
                                Files.deleteIfExists(versionJarPath);
                            }
                            this.abortFinished.emit();
                            return;
                        }
                    }
                }

                if (this.aborted) {
                    this.abortFinished.emit();
                    return;
                }

                this.setStatus("Saving config");
                this.setProgress(50, 100);

                try {
                    Instance instance = new Instance();
                    instance.name = this.name;
                    instance.version = this.version.id;
                    instance.groupName = this.groupName;

                    XLauncher.getInstance().getInstanceManager().getInstances().add(instance);
                    XLauncher.getInstance().getInstanceManager().groupNames.add(this.groupName);
                    XLauncher.getInstance().getInstanceManager().saveGroupsOnly();

                    FileUtils.writeString(instancePath.resolve("instance.json"), instance.toJson(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (this.aborted) {
                    Files.deleteIfExists(instancePath.resolve("instance.json"));
                    this.abortFinished.emit();
                    return;
                }
            }

            this.setStatus("Finished");
            this.setProgress(100, 100);

            SwingUtilities.invokeLater(() -> XLauncher.getInstance().getInstanceManager().refresh());
            this.finished.emit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 10000) {
            return bytes + "B";
        } else if (bytes < 1000000) {
            DecimalFormat formatter = new DecimalFormat("#.## KiB");
            return formatter.format(bytes / 1024d);
        }
        DecimalFormat formatter = new DecimalFormat("#.## MiB");
        return formatter.format(bytes / 1024d / 1024d);
    }
}
