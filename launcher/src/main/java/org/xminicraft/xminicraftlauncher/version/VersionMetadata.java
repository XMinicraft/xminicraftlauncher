package org.xminicraft.xminicraftlauncher.version;

import java.net.URI;
import java.time.ZonedDateTime;

public class VersionMetadata {
    public final Version.Type type;
    public final String id;
    public final int minimumJavaVersion;
    public final String mainClass;
    public final ZonedDateTime releaseTime;
    public final URI clientJarUri;

    public VersionMetadata(Version.Type type, String id, ZonedDateTime releaseTime, int minimumJavaVersion, String mainClass, URI clientJarUri) {
        this.type = type;
        this.id = id;
        this.releaseTime = releaseTime;
        this.minimumJavaVersion = minimumJavaVersion;
        this.mainClass = mainClass;
        this.clientJarUri = clientJarUri;
    }
}
