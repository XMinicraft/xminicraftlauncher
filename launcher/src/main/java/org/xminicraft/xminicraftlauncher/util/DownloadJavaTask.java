package org.xminicraft.xminicraftlauncher.util;

import org.xminicraft.xminicraftlauncher.Language;
import org.xminicraft.xminicraftlauncher.gui.util.SwingUtils;
import org.xminicraft.xminicraftlauncher.java.JavaRuntime;

import javax.swing.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadJavaTask extends Task {
    private final JavaRuntime runtime;
    private final URI uri;
    private final int size;

    public DownloadJavaTask(JavaRuntime runtime) {
        this.runtime = runtime;
        this.uri = URI.create(runtime.url);
        this.size = runtime.size;
    }

    @Override
    public String getTitle() {
        return Language.translate("Downloading Java");
    }

    @Override
    public void run() {
        try {
            URL url = this.uri.toURL();

            InputStream inputStream;
            try {
                inputStream = url.openStream();
            } catch (IOException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Failed to open connection"));
                this.abortFinished.emit();
                return;
            }

            this.setStatus("Download progress: 0% (" + formatBytes(0) + "/" + formatBytes(this.size) + ")");
            this.setProgress(0, 100);

            String name = this.runtime.name;

            if (this.aborted) {
                this.abortFinished.emit();
                return;
            }

            Path outP = Paths.get("cache/" + name + ".zip").toAbsolutePath();
            if (!Files.exists(outP)) {
                BufferedOutputStream outputStream = new BufferedOutputStream(
                        Files.newOutputStream(outP)
                );

                // Buffer for downloading
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;

                // Read and write file while updating progress
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (this.aborted) {
                        outputStream.close();
                        Files.deleteIfExists(outP);
                        this.abortFinished.emit();
                        return;
                    }

                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    // Calculate and display progress in console
                    int percent = (int) ((totalBytesRead * 100) / this.size);
                    this.setStatus("Download progress: " + percent + "% (" + formatBytes(totalBytesRead) + " / " + formatBytes(this.size) + ")");
                    this.setProgress(percent, 100);
                }

                outputStream.close();
            } else {
                System.out.println("Already downloaded");
            }

            if (this.aborted) {
                Files.deleteIfExists(outP);
                this.abortFinished.emit();
                return;
            }

            this.setStatus("Unzipping");
            this.setProgress(0, 100);

            Path p = Paths.get("java/" + name);
            if (!Files.exists(p)) {
                FileUtils.ensureDirectory(p);
            }

            try (FileInputStream fis = new FileInputStream(outP.toString());
                 ZipInputStream zis = new ZipInputStream(fis)) {

                ZipEntry entry;
                byte[] buffer = new byte[2048];

                while ((entry = zis.getNextEntry()) != null) {
                    File newFile = new File(p.toAbsolutePath().toString(), entry.getName());

                    this.setStatus("Unzipping " + entry.getName());
                    this.setProgress(0, 100);

                    // Create directories if the entry is a directory
                    if (entry.isDirectory()) {
                        newFile.mkdirs();
                        continue;
                    }

                    // Ensure parent directories exist for files
                    newFile.getParentFile().mkdirs();

                    // Write file content
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    zis.closeEntry();
                }
            }

            if (!Files.exists(p.resolve("bin"))) {
                List<Path> paths = Files.list(p).collect(Collectors.toList());
                if (!paths.isEmpty()) {
                    Files.move(paths.get(0), p.getParent().resolve(p.getFileName() + ".rn"));
                    Files.deleteIfExists(p);
                    Files.move(p.getParent().resolve(p.getFileName() + ".rn"), p);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.setStatus("Download failed");
            this.setProgress(100, 100);
        }

        this.finished.emit();
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
