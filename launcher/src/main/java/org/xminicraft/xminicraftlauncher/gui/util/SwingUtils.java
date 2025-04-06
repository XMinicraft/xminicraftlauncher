package org.xminicraft.xminicraftlauncher.gui.util;

import org.xminicraft.xminicraftlauncher.XLauncher;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SwingUtils {
    public static <T> SpinnerNumberModel createSpinnerNumberModel(Comparable<T> min, Comparable<T> max, Number step, Comparable<T> value) {
        SpinnerNumberModel model = new SpinnerNumberModel();
        model.setMinimum(min);
        model.setMaximum(max);
        model.setStepSize(step);
        model.setValue(value);
        return model;
    }

    public static void registerEscapeAction(JRootPane rootPane, Runnable runnable) {
        rootPane.registerKeyboardAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runnable.run();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public static <T> ListModel<T> createImmutListModel(List<T> list) {
        return new AbstractListModel<T>() {
            @Override
            public int getSize() {
                return list.size();
            }

            @Override
            public T getElementAt(int index) {
                return list.get(index);
            }
        };
    }

    public static <T> ComboBoxModel<T> createImmutComboBoxModel(Set<T> set) {
        List<T> list = new ArrayList<>(set);
        return createImmutComboBoxModel(list);
    }

    public static <T> ComboBoxModel<T> createImmutComboBoxModel(List<T> list) {
        return new ComboBoxModel<T>() {
            private T selected = list.isEmpty() ? null : list.get(0);

            @SuppressWarnings("unchecked")
            @Override
            public void setSelectedItem(Object item) {
                this.selected = (T) item;
            }

            @Override
            public Object getSelectedItem() {
                return this.selected;
            }

            @Override
            public int getSize() {
                return list.size();
            }

            @Override
            public T getElementAt(int index) {
                return list.get(index);
            }

            @Override
            public void addListDataListener(ListDataListener l) {
            }

            @Override
            public void removeListDataListener(ListDataListener l) {
            }
        };
    }
}
