package org.xminicraft.xminicraftlauncher;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Path workingPath = Paths.get("");
        for (int i = 0; i < args.length; ++i) {
            if (i + 1 < args.length && "--workingDir".equals(args[0])) {
                workingPath = Paths.get(args[++i]);
                break;
            }
        }

        new XLauncher(workingPath, args);
    }
}