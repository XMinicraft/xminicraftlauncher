package org.xminicraft.xminicraftlauncher.java;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JavaRuntimeSource {
    public final String id;
    public final String name;
    public final Map<Integer, List<JavaRuntime>> runtimes;
    public final List<String> availableReleases;

    public JavaRuntimeSource(String id, String name, Map<Integer, List<JavaRuntime>> runtimes, List<String> availableReleases) {
        this.id = id;
        this.name = name;
        this.runtimes = Collections.unmodifiableMap(runtimes);
        this.availableReleases = Collections.unmodifiableList(availableReleases);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
