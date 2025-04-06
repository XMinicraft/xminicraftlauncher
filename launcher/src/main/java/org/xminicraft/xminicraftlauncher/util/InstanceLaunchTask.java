package org.xminicraft.xminicraftlauncher.util;

import org.xminicraft.xminicraftlauncher.Instance;
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

public class InstanceLaunchTask extends Task {
    private final Instance instance;

    public InstanceLaunchTask(Instance instance) {
        this.instance = instance;
    }

    @Override
    public void run() {
        try {
            this.setStatus("Ensuring folder");
            this.setProgress(0, 100);
            Path instancePath = this.instance.path;
            if (!Files.exists(instancePath)) {
                FileUtils.ensureDirectory(instancePath);
            }

            FileUtils.ensureDirectory(instancePath.resolve("save_data"));

            if (this.aborted) {
                Files.deleteIfExists(instancePath);
                this.abortFinished.emit();
                return;
            }

            this.setStatus("Ensuring game version metadata");
            this.setProgress(25, 100);

            Version version = XLauncher.getInstance().getVersionManager().findVersion(this.instance.version).get();

            Path versionPath = XLauncher.getInstance().getVersionsPath().resolve(this.instance.version);
            Path versionJarPath = versionPath.resolve("client.jar");
            Path versionInfoPath = versionPath.resolve("metadata.json");

            FileUtils.ensureDirectory(versionPath);

            if (!DownloadUtils.download(URI.create(version.url), versionInfoPath)) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Failed to open connection"));
                this.abortFinished.emit();
                return;
            }

            Optional<VersionMetadata> metadata = XLauncher.getInstance().getVersionManager().getManifest(version);
            if (this.aborted) {
                this.abortFinished.emit();
                return;
            }

            if (metadata.isPresent()) {
                if (version.type == Version.Type.JAVA) {
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
            }

            this.setStatus("Finished");
            this.setProgress(100, 100);

            SwingUtilities.invokeLater(() -> XLauncher.getInstance().getInstanceManager().launch(this.instance));
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
