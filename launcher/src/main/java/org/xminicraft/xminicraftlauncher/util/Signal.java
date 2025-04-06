package org.xminicraft.xminicraftlauncher.util;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Signal<T> {
    private final List<Consumer<T>> listeners = new ArrayList<>();

    public void emit() {
        this.listeners.forEach(listener -> listener.accept(null));
    }

    public void emit(T data) {
        this.listeners.forEach(listener -> listener.accept(data));
    }

    public void connect(Runnable listener) {
        this.listeners.add(data -> listener.run());
    }

    public Consumer<T> connect(Consumer<T> listener) {
        this.listeners.add(listener);
        return listener;
    }

    public void connect(Component context, Runnable listener) {
        this.connect(context, data -> listener.run());
    }

    public void connect(Component context, Consumer<T> listener) {
        this.listeners.add(listener);
        context.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                if (!e.getComponent().isDisplayable()) {
                    for (Consumer<T> listenerItem : this.listeners) {
                        if (listenerItem == listener) {
                            this.listeners.remove(listenerItem);
                            break;
                        }
                    }
                }
            }
        });
    }
}
