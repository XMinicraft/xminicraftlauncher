package org.xminicraft.xminicraftlauncher.gui.util;

import java.awt.*;

public class GridBagContrs extends GridBagConstraints {
    public GridBagContrs fillH() {
        this.weight(1, 0);
        return this.fill(true, false);
    }

    public GridBagContrs fillV() {
        this.weight(0, 1);
        return this.fill(false, true);
    }

    public GridBagContrs fill() {
        this.weight(1, 1);
        return this.fill(true, true);
    }

    public GridBagContrs fill(boolean h, boolean v) {
        this.fill = h & v ? BOTH : h ? HORIZONTAL : VERTICAL;
        return this;
    }

    public GridBagContrs pos(int x, int y) {
        this.gridx = x;
        this.gridy = y;
        return this;
    }

    public GridBagContrs size(int w, int h) {
        this.gridwidth = w;
        this.gridheight = h;
        return this;
    }

    public GridBagContrs pad(int x, int y) {
        this.ipadx = x;
        this.ipady = y;
        return this;
    }

    public GridBagContrs weight(int x, int y) {
        this.weightx = x;
        this.weighty = y;
        return this;
    }

    public GridBagContrs anchor(int a) {
        this.anchor = a;
        return this;
    }

    public GridBagContrs insets(int inset) {
        this.insets.top = this.insets.bottom = this.insets.left = this.insets.right = inset;
        return this;
    }

    public GridBagContrs insets(int tb, int lr) {
        this.insets.top = this.insets.bottom = tb;
        this.insets.left = this.insets.right = lr;
        return this;
    }

    public GridBagContrs insetsV(int t, int b) {
        this.insets.top = t;
        this.insets.bottom = b;
        return this;
    }

    public GridBagContrs insetsH(int l, int r) {
        this.insets.left = l;
        this.insets.right = r;
        return this;
    }
}
