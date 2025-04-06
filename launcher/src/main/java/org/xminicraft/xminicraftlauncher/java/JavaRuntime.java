package org.xminicraft.xminicraftlauncher.java;

import java.time.ZonedDateTime;

public class JavaRuntime {
    public String name;
    public ZonedDateTime releaseTime;
    public String url;
    public int size;
    public String runtimeOs;
    public String architecture;
    public String version;

    public JavaRuntime(String name, ZonedDateTime releaseTime, String url, int size, String runtimeOs, String version) {
        this.name = name;
        this.releaseTime = releaseTime;
        this.url = url;
        this.size = size;
        this.runtimeOs = runtimeOs;

        String[] runtimeInfo = runtimeOs.split("-");
        if (runtimeInfo.length > 1) {
            this.architecture = runtimeInfo[1];
        } else {
            this.architecture = System.getProperty("os.arch");
        }

        this.version = version;
    }
}
