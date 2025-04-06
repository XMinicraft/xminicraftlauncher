package org.xminicraft.xminicraftlauncher.util;

import org.xminicraft.xminicraftlauncher.Instance;
import org.xminicraft.xminicraftlauncher.Language;
import org.xminicraft.xminicraftlauncher.XLauncher;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InstanceDeleteTask extends Task {
    private final Instance instance;
    private final Runnable onFinished;

    public InstanceDeleteTask(Instance instance, Runnable onFinished) {
        this.instance = instance;
        this.onFinished = onFinished;
    }

    @Override
    public String getTitle() {
        return Language.translate("Deleting Instance");
    }

    @Override
    public void run() {
        try (Stream<Path> paths = Files.walk(this.instance.path)) {
            List<Path> list = paths.collect(Collectors.toList());
            int done = 0;
            this.setStatus(done + "/" + list.size());
            this.setProgress(0, list.size());
            for (int i = list.size() - 1; i >= 0; --i) {
                Path p = list.get(i);
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    ++done;
                    this.setStatus(done + "/" + list.size() + " " + this.instance.path.relativize(p));
                    this.setProgress(done, list.size());
                }
            }
            Files.deleteIfExists(this.instance.path);
            SwingUtilities.invokeLater(() -> {
                XLauncher.getInstance().getInstanceManager().getInstances().remove(this.instance);
                this.onFinished.run();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.finished.emit();
    }
}
