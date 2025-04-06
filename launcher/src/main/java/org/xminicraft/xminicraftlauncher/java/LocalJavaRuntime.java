package org.xminicraft.xminicraftlauncher.java;

import java.nio.file.Path;

public class LocalJavaRuntime {
    public String version;
    public String architecture;
    public Path path;

    @Override
    public String toString() {
        return "LocalJavaRuntime{" +
                "path='" + this.path + '\'' +
                ", name='" + this.version + '\'' +
                ", architecture='" + this.architecture + '\'' +
                '}';
    }
}
