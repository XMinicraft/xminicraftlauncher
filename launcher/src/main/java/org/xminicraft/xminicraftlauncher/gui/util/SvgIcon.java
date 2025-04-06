package org.xminicraft.xminicraftlauncher.gui.util;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import java.util.HashMap;
import java.util.Map;

public class SvgIcon {
    private static final Map<String, FlatSVGIcon> icons = new HashMap<>();

    public static FlatSVGIcon get(String name) {
        if (SvgIcon.icons.containsKey(name)) {
            return SvgIcon.icons.get(name);
        }

        FlatSVGIcon icon = new FlatSVGIcon(SvgIcon.class.getResource("/" + name + ".svg"));
        SvgIcon.icons.put(name, icon);
        return icon;
    }
}
