package org.xminicraft.xminicraftlauncher;

import org.xminicraft.xminicraftlauncher.gui.LauncherGui;
import org.xminicraft.xminicraftlauncher.java.JavaRuntimeManager;
import org.xminicraft.xminicraftlauncher.util.FileUtils;
import org.xminicraft.xminicraftlauncher.version.VersionManager;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Properties;

public class XLauncher {
    private static XLauncher instance;

    private final Path workingPath;
    private final Path logsPath;
    private final Path cachePath;
    private final Path metaPath;
    private final Path javaInstallationsPath;
    private final Path librariesPath;
    private final Path instancesPath;
    private final Path versionsPath;

    private final VersionManager versionManager;
    private final InstanceManager instanceManager;
    private final JavaRuntimeManager javaRuntimeManager;
    private final Settings settings;

    private final Build build;

    public XLauncher(Path workingPath, String[] args) {
        instance = this;

        Build build = new Build("?.?.?", ZonedDateTime.now());
        try {
            Properties properties = new Properties();
            properties.load(XLauncher.class.getResourceAsStream("/build.properties"));
            build = new Build(properties.getProperty("version"), ZonedDateTime.parse(properties.getProperty("build.time")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.build = Build.load();

        this.workingPath = workingPath;

        this.settings = Settings.load(this.workingPath.resolve("settings.json"));

        this.logsPath = this.workingPath.resolve("logs");
        this.cachePath = this.workingPath.resolve("cache");
        this.metaPath = this.workingPath.resolve("meta");
        this.javaInstallationsPath = this.workingPath.resolve("java");
        this.librariesPath = this.workingPath.resolve("libraries");
        this.instancesPath = this.workingPath.resolve("instances");
        this.versionsPath = this.workingPath.resolve("versions");

        this.ensureLauncherDirectories();

        this.versionManager = new VersionManager();
        this.instanceManager = new InstanceManager();
        this.instanceManager.load(this.instancesPath, this.versionManager);
        this.javaRuntimeManager = new JavaRuntimeManager();
        this.javaRuntimeManager.load();

        String launchName = null;
        for (int i = 0; i < args.length; ++i) {
            if ("--launch".equals(args[0]) && i + 1 < args.length) {
                launchName = args[++i];
                break;
            }
        }

        if (launchName == null) {
            SwingUtilities.invokeLater(() -> {
                new LauncherGui().startTask(null, this.javaRuntimeManager.refresh(false));
            });
        } else {
            this.javaRuntimeManager.refresh(false).run();
            this.instanceManager.findByName(launchName).ifPresent(instance -> this.instanceManager.launch(instance, false));
        }
    }

    private void ensureLauncherDirectories() {
        try {
            FileUtils.ensureDirectory(this.workingPath);
            FileUtils.ensureDirectory(this.logsPath);
            FileUtils.ensureDirectory(this.cachePath);
            FileUtils.ensureDirectory(this.metaPath);
            FileUtils.ensureDirectory(this.javaInstallationsPath);
            FileUtils.ensureDirectory(this.librariesPath);
            FileUtils.ensureDirectory(this.instancesPath);
            FileUtils.ensureDirectory(this.versionsPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void quit() {
        this.instanceManager.saveGroups();
        this.settings.save(this.workingPath.resolve("settings.json"));
    }

    public static XLauncher getInstance() {
        return instance;
    }

    public Path getWorkingPath() {
        return this.workingPath;
    }

    public VersionManager getVersionManager() {
        return this.versionManager;
    }

    public Path getMetaPath() {
        return this.metaPath;
    }

    public Path getInstancesPath() {
        return this.instancesPath;
    }

    public Path getLibrariesPath() {
        return this.librariesPath;
    }

    public Path getVersionsPath() {
        return this.versionsPath;
    }

    public Path getCachePath() {
        return this.cachePath;
    }

    public Path getJavaInstallationsPath() {
        return this.javaInstallationsPath;
    }

    public Settings getSettings() {
        return this.settings;
    }

    public InstanceManager getInstanceManager() {
        return this.instanceManager;
    }

    public JavaRuntimeManager getJavaRuntimeManager() {
        return this.javaRuntimeManager;
    }

    public Build getBuild() {
        return this.build;
    }

    public static class Build {
        public final String version;
        public final ZonedDateTime buildTime;

        public Build(String version, ZonedDateTime buildTime) {
            this.version = version;
            this.buildTime = buildTime;
        }

        public static Build load() {
            try {
                Properties properties = new Properties();
                properties.load(XLauncher.class.getResourceAsStream("/build.properties"));
                return new Build(properties.getProperty("version"), ZonedDateTime.parse(properties.getProperty("build.time")));
            } catch (IOException e) {
                e.printStackTrace();
                return new Build("?.?.?", ZonedDateTime.now());
            }
        }
    }
}
