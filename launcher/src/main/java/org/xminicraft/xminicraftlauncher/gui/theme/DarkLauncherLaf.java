package org.xminicraft.xminicraftlauncher.gui.theme;

import com.formdev.flatlaf.FlatDarculaLaf;

public class DarkLauncherLaf extends FlatDarculaLaf {
    public static boolean setup() {
        return setup(new DarkLauncherLaf());
    }

    @Override
    public String getName() {
        return "DarkLauncherLaf";
    }
}
