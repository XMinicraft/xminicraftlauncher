package org.xminicraft.xminicraftlauncher.util;

import org.xminicraft.xminicraftlauncher.Language;
import org.xminicraft.xminicraftlauncher.XLauncher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeleteCacheTask extends Task {
    @Override
    public String getTitle() {
        return Language.translate("Deleting Cache");
    }

    @Override
    public void run() {
        Path rootPath = XLauncher.getInstance().getCachePath();

        try (Stream<Path> paths = Files.walk(rootPath)) {
            List<Path> list = paths.collect(Collectors.toList());
            int done = 0;
            this.setStatus(done + "/" + list.size());
            this.setProgress(0, list.size());
            for (int i = list.size() - 1; i >= 0; --i) {
                Path p = list.get(i);
                if (p.equals(rootPath)) continue;

                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    ++done;
                    this.setStatus(done + "/" + list.size() + " " + rootPath.relativize(p));
                    this.setProgress(done, list.size());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.finished.emit();
    }
}
