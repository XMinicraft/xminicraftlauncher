package org.xminicraft.xminicraftlauncher.gui.util;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class SimpleDocumentListener implements DocumentListener {
    private final Runnable listener;

    public SimpleDocumentListener(Runnable listener) {
        this.listener = listener;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        this.listener.run();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        this.listener.run();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        this.listener.run();
    }
}
