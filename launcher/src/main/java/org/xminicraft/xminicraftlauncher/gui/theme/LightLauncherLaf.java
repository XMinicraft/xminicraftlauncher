package org.xminicraft.xminicraftlauncher.gui.theme;

import com.formdev.flatlaf.FlatIntelliJLaf;

public class LightLauncherLaf extends FlatIntelliJLaf {
    public static boolean setup() {
        return setup(new LightLauncherLaf());
    }

    @Override
    public String getName() {
        return "LightLauncherLaf";
    }
}
