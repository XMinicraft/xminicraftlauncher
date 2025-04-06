package org.xminicraft.xminicraftlauncher.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public enum OperatingSystem {
    WINDOWS("windows") {
        @Override
        protected String[] getURIOpenCommand(URI uri) {
            return new String[]{"rundll32", "url.dll,FileProtocolHandler", uri.toString()};
        }
    },
    LINUX("linux"),
    OSX("macos") {
        @Override
        protected String[] getURIOpenCommand(URI uri) {
            return new String[]{"open", uri.toString()};
        }
    },
    SOLARIS("solaris"),
    UNKNOWN("unknown");

    public final String name;

    OperatingSystem(String name) {
        this.name = name;
    }

    public String arch() {
        return System.getProperty("os.arch");
    }

    public String getJavaExecutableName() {
        if (this == WINDOWS) {
            return "javaw.exe";
        }
        return "java";
    }

    protected String[] getURIOpenCommand(URI uri) {
        String string = uri.toString();
        if ("file".equals(uri.getScheme())) {
            string = string.replace("file:", "file://");
        }
        return new String[]{"xdgopen", string};
    }

    public void open(URI uri) {
        CompletableFuture.runAsync(() -> {
            try {
                Process process = Runtime.getRuntime().exec(this.getURIOpenCommand(uri));
                process.getInputStream().close();
                process.getErrorStream().close();
                process.getOutputStream().close();
            } catch (IOException ignored) {
            }
        });
    }

    public void open(Path path) {
        this.open(path.toUri());
    }

    private static OperatingSystem operatingSystem;

    public static OperatingSystem get() {
        if (operatingSystem != null) {
            return operatingSystem;
        }

        String name = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        if (name.contains("win")) {
            operatingSystem = OperatingSystem.WINDOWS;
        } else if (name.contains("mac")) {
            operatingSystem = OperatingSystem.OSX;
        } else if (name.contains("solaris") || name.contains("sunos")) {
            operatingSystem = OperatingSystem.SOLARIS;
        } else if (name.contains("linux") || name.contains("unix")) {
            operatingSystem = OperatingSystem.LINUX;
        } else {
            operatingSystem = OperatingSystem.UNKNOWN;
        }

        return operatingSystem;
    }
}

