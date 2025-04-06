package org.xminicraft.xminicraftlauncher.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public final class FileUtils {
    public static void ensureDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    public static void writeString(Path path, String content, Charset charset) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, charset)) {
            writer.write(content);
        }
    }

    public static String readLines(Path path, Charset charset) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(path, charset)) {
            String line;
            while ((line = reader.readLine()) != null) {;
                if (builder.length() > 0) builder.append('\n');
                builder.append(line);
            }
        } catch (IOException ignored) {
        }
        return builder.toString();
    }

    public static String toString(InputStream inputStream, Charset charset) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            String line;
            while ((line = reader.readLine()) != null) {;
                if (builder.length() > 0) builder.append('\n');
                builder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    private static Path jarResourcesPath;

    public static Path getJarResourcesPath() {
        if (jarResourcesPath == null) {
            try {
                URI uri = Objects.requireNonNull(FileUtils.class.getResource("/.root")).toURI();

                try {
                    jarResourcesPath = Paths.get(uri).getParent();
                } catch (FileSystemNotFoundException | IllegalArgumentException | SecurityException ignored) {
                    try {
                        FileSystems.newFileSystem(uri, Collections.emptyMap());
                    } catch (FileSystemAlreadyExistsException | IOException ignored2) {
                    }

                    jarResourcesPath = Paths.get(uri).getParent();
                }
            } catch (URISyntaxException ignored) {
            }
        }
        return jarResourcesPath;
    }

    private FileUtils() {
        throw new UnsupportedOperationException();
    }
}
