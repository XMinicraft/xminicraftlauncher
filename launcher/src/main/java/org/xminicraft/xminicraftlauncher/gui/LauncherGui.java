package org.xminicraft.xminicraftlauncher.gui;

import com.formdev.flatlaf.FlatLaf;
import org.xminicraft.xminicraftlauncher.Language;
import org.xminicraft.xminicraftlauncher.util.DeleteCacheTask;
import org.xminicraft.xminicraftlauncher.util.OperatingSystem;
import org.xminicraft.xminicraftlauncher.util.Task;
import org.xminicraft.xminicraftlauncher.XLauncher;
import org.xminicraft.xminicraftlauncher.gui.theme.DarkLauncherLaf;
import org.xminicraft.xminicraftlauncher.gui.theme.LightLauncherLaf;
import org.xminicraft.xminicraftlauncher.gui.util.GridBagContrs;
import org.xminicraft.xminicraftlauncher.gui.util.SvgIcon;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class LauncherGui {
    private static LauncherGui instance;
    private final JFrame frame;
    private final InstancesView instancesView;

    public static LauncherGui getInstance() {
        return instance;
    }

    public LauncherGui() {
        instance = this;
        JDialog.setDefaultLookAndFeelDecorated(true);
        JFrame.setDefaultLookAndFeelDecorated(true);

        UIManager.put("ScrollBar.showButtons", true);
        FlatLaf.registerCustomDefaultsSource("themes");
        this.applyTheme();
        Language.getInstance().load(XLauncher.getInstance().getSettings().language);

        this.frame = new JFrame("XMinicraft Launcher");
        try {
            this.frame.setIconImage(new ImageIcon(ImageIO.read(LauncherGui.class.getResourceAsStream("/icon.png"))).getImage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                LauncherGui.this.frame.dispose();
                XLauncher.getInstance().quit();
            }
        });

        this.frame.setJMenuBar(this.buildMenuBar());

        this.instancesView = new InstancesView(this.frame);
        this.frame.getContentPane().add(this.instancesView, BorderLayout.CENTER);

        this.frame.getContentPane().setPreferredSize(new Dimension(732, 600));
        this.frame.setMinimumSize(new Dimension(510, 480));
        this.frame.pack();
        this.frame.setLocationRelativeTo(null);
        this.frame.setVisible(true);
    }

    public void updateTranslations() {
        for (int i = 0; i < this.frame.getJMenuBar().getMenuCount(); ++i) {
            JMenu menu = this.frame.getJMenuBar().getMenu(i);
            if (menu == null) continue;
            menu.setText(Language.translate((String) menu.getClientProperty("translation_key")));

            for (int j = 0; j < menu.getItemCount(); ++j) {
                JMenuItem menuItem = menu.getItem(j);
                if (menuItem == null) continue;
                menuItem.setText(Language.translate((String) menuItem.getClientProperty("translation_key")));
            }
        }

        this.instancesView.updateTranslations();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = menuBar.add(new JMenu(Language.translate("File")));
        fileMenu.putClientProperty("translation_key", "File");

        JMenuItem patchNotesMenuItem = fileMenu.add(new JMenuItem(Language.translate("Patch Notes")));
        patchNotesMenuItem.putClientProperty("translation_key", "Patch Notes");
        patchNotesMenuItem.setIcon(SvgIcon.get("icons/patch_notes").derive(16, 16));
        patchNotesMenuItem.addActionListener(e -> new MinicraftPatchNotesDialog(this.frame));
        patchNotesMenuItem.setVisible(false);

        JMenuItem settingsMenuItem = fileMenu.add(new JMenuItem(Language.translate("Settings")));
        settingsMenuItem.putClientProperty("translation_key", "Settings");
        settingsMenuItem.setIcon(SvgIcon.get("icons/general").derive(16, 16));
        settingsMenuItem.addActionListener(e -> new SettingsDialog(this.frame));

        JMenuItem deleteCacheMenuItem = fileMenu.add(new JMenuItem(Language.translate("Delete Cache")));
        deleteCacheMenuItem.putClientProperty("translation_key", "Delete Cache");
        deleteCacheMenuItem.addActionListener(e -> this.startTask(this.frame, new DeleteCacheTask()));

        fileMenu.addSeparator();

        JMenuItem quitMenuItem = fileMenu.add(new JMenuItem(Language.translate("Quit")));
        quitMenuItem.putClientProperty("translation_key", "Quit");
        quitMenuItem.addActionListener(e -> this.frame.dispose());

        JMenu foldersMenu = menuBar.add(new JMenu(Language.translate("Folders")));
        foldersMenu.putClientProperty("translation_key", "Folders");

        JMenuItem launcherFolderMenuItem = foldersMenu.add(new JMenuItem(Language.translate("Launcher")));
        launcherFolderMenuItem.putClientProperty("translation_key", "Launcher");
        launcherFolderMenuItem.addActionListener(e -> OperatingSystem.get().open(XLauncher.getInstance().getWorkingPath()));

        JMenuItem instancesMenuItem = foldersMenu.add(new JMenuItem(Language.translate("Instances")));
        instancesMenuItem.putClientProperty("translation_key", "Instances");
        instancesMenuItem.addActionListener(e -> OperatingSystem.get().open(XLauncher.getInstance().getInstancesPath()));

        JMenuItem cacheMenuItem = foldersMenu.add(new JMenuItem(Language.translate("Cache")));
        cacheMenuItem.putClientProperty("translation_key", "Cache");
        cacheMenuItem.addActionListener(e -> OperatingSystem.get().open(XLauncher.getInstance().getCachePath()));

        JMenu helpMenu = menuBar.add(new JMenu(Language.translate("Help")));
        helpMenu.putClientProperty("translation_key", "Help");

        JMenuItem aboutMenuItem = helpMenu.add(new JMenuItem(Language.translate("About")));
        aboutMenuItem.putClientProperty("translation_key", "About");
        aboutMenuItem.setIcon(SvgIcon.get("icons/about").derive(16, 16));
        aboutMenuItem.addActionListener(e -> new AboutDialog(this.frame));

        return menuBar;
    }

    public void startTask(Window owner, Task task) {
        JDialog dialog = new JDialog(owner == null ? this.frame : owner, task.getTitle());
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel label = new JLabel(task.getStatus().isEmpty() ? "Waiting..." : task.getStatus());
        label.setHorizontalAlignment(SwingConstants.LEFT);
        JProgressBar bar = new JProgressBar(0, task.getTotalProgress());
        bar.setMinimum(0);
        bar.setMaximum(task.getTotalProgress());
        bar.setValue(task.getCurrentProgress());

        task.status.connect(text -> {
            SwingUtilities.invokeLater(() -> label.setText(text));
        });

        task.progress.connect(data -> {
            SwingUtilities.invokeLater(() -> {
                bar.setMinimum(0);
                bar.setMaximum(data.total);
                bar.setValue(data.current);
            });
        });

        CompletableFuture.runAsync(() -> {
            task.run();
        });

        task.abortFinished.connect(dialog, () -> {
            SwingUtilities.invokeLater(() -> dialog.dispose());
        });

        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                task.abort();
            }
        });

        panel.add(label, new GridBagContrs().anchor(GridBagContrs.NORTHWEST));
        panel.add(bar, new GridBagContrs().fill(true, false).pos(0, 1).pad(10, 10).insetsV(8, 0).weight(1, 0));

        JButton abortButton = new JButton("Abort");
        abortButton.addActionListener(e -> task.abort());
        panel.add(abortButton, new GridBagContrs().fill(true, false).pos(0, 2).insetsV(8, 0).weight(1, 0));

        task.finished.connect(dialog, dialog::dispose);

        dialog.add(panel, BorderLayout.CENTER);

        Dimension dim = dialog.getContentPane().getPreferredSize();
        dim.width = dim.width < 300 ? 300 : dim.width;
        dialog.getContentPane().setPreferredSize(dim);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private void applyTheme() {
        if ("Dark".equals(XLauncher.getInstance().getSettings().theme)) {
            DarkLauncherLaf.setup();
        } else {
            LightLauncherLaf.setup();
        }
    }
}
