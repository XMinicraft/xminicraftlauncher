package org.xminicraft.xminicraftlauncher.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;

public final class DownloadUtils {
    public static boolean download(URI uri, Path outputPath) {
        return download(uri, outputPath, 60);
    }

    public static boolean download(URI uri, Path outputPath, int minutesThreshold) {
        try {
            if (Files.exists(outputPath)) {
                Instant fileInstant = Files.getLastModifiedTime(outputPath).toInstant();
                Instant now = Instant.now();

                if (Duration.between(fileInstant, now).toMinutes() < minutesThreshold) {
                    return true;
                }
            }

            System.out.println("Downloading " + uri.toString());
            Files.copy(uri.toURL().openStream(), outputPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return Files.exists(outputPath);
        }
        return true;
    }

    public static boolean downloadWithUserAgent(URI uri, Path outputPath, int minutesThreshold) {
        try {
            if (Files.exists(outputPath)) {
                Instant fileInstant = Files.getLastModifiedTime(outputPath).toInstant();
                Instant now = Instant.now();

                if (Duration.between(fileInstant, now).toMinutes() < minutesThreshold) {
                    return true;
                }
            }

            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.addRequestProperty("User-Agent", "XMinicraftLauncher");
            int status = conn.getResponseCode();

            if (status != 200) {
                return false;
            }

            Files.copy(conn.getInputStream(), outputPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ignored) {
            ignored.printStackTrace();
            return false;
        }
    }

    public static void download(URI uri, long size, Path destinationPath, BooleanSupplier continueSupplier, DownloadProgressListener progressCallback) throws IOException {
        URL url = uri.toURL();

        long fileSize;
        try {
            fileSize = url.openConnection().getContentLengthLong();
        } catch (Exception e) {
            fileSize = size;
        }

        try (ReadableByteChannel channel = Channels.newChannel(url.openStream()); OutputStream out = Files.newOutputStream(destinationPath)) {
            byte[] buffer = new byte[1024 * 8];
            long bytesDownloaded = 0;

            int bytesRead;
            while ((bytesRead = channel.read(ByteBuffer.wrap(buffer))) != -1) {
                if (!continueSupplier.getAsBoolean()) break;

                out.write(buffer, 0, bytesRead);
                bytesDownloaded += bytesRead;

                if (fileSize > 0) {
                    double progress = (double) bytesDownloaded / fileSize;
                    progressCallback.accept(Math.min(progress, 1.0), bytesDownloaded, fileSize);
                }
            }

            progressCallback.accept(1.0, bytesDownloaded, fileSize);
        }
    }

    @FunctionalInterface
    public interface DownloadProgressListener {
        void accept(double progress, long bytesDownloaded, long bytesTotal);
    }

    private DownloadUtils() {
        throw new UnsupportedOperationException();
    }
}
