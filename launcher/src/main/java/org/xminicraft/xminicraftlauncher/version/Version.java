package org.xminicraft.xminicraftlauncher.version;

import java.time.ZonedDateTime;
import java.util.Objects;

public class Version {
    public String id;
    public Type type;
    public String url;
    public ZonedDateTime releaseTime;

    public Version(String id, Type type, String url, ZonedDateTime releaseTime) {
        this.id = id;
        this.type = type;
        this.url = url;
        this.releaseTime = releaseTime;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof Version)) return false;
        Version version = (Version) o;
        return Objects.equals(this.id, version.id) && this.type == version.type && Objects.equals(this.url, version.url) && Objects.equals(this.releaseTime, version.releaseTime);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(this.id);
        result = 31 * result + Objects.hashCode(this.type);
        result = 31 * result + Objects.hashCode(this.url);
        result = 31 * result + Objects.hashCode(this.releaseTime);
        return result;
    }

    @Override
    public String toString() {
        return this.id;
    }

    public enum Type {
        JAVA("Java"),
        EXECUTABLE("Executable");

        public final String name;

        Type(String name) {
            this.name = name;
        }
    }
}
