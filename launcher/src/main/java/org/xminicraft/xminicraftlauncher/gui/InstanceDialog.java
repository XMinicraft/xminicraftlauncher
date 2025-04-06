package org.xminicraft.xminicraftlauncher.gui;

import com.formdev.flatlaf.util.SystemFileChooser;
import org.xminicraft.xminicraftlauncher.Instance;
import org.xminicraft.xminicraftlauncher.Language;
import org.xminicraft.xminicraftlauncher.XLauncher;
import org.xminicraft.xminicraftlauncher.gui.util.GridBagContrs;
import org.xminicraft.xminicraftlauncher.gui.util.SimpleDocumentListener;
import org.xminicraft.xminicraftlauncher.gui.util.SvgIcon;
import org.xminicraft.xminicraftlauncher.gui.util.SwingUtils;
import org.xminicraft.xminicraftlauncher.java.LocalJavaRuntime;
import org.xminicraft.xminicraftlauncher.util.InstanceLaunchTask;
import org.xminicraft.xminicraftlauncher.util.OperatingSystem;
import org.xminicraft.xminicraftlauncher.version.Version;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class InstanceDialog extends JDialog {
    private final Instance instance;
    private final Runnable onClose;

    public InstanceDialog(JFrame owner, Instance instance, Runnable onClose) {
        super(owner, Language.translate("Instance"));
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.onClose = onClose;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                InstanceDialog.this.close();
            }
        });
        this.instance = instance;

        JTabbedPane viewTabbedPane = new JTabbedPane();
        viewTabbedPane.setTabPlacement(JTabbedPane.LEFT);
        viewTabbedPane.putClientProperty("JTabbedPane.tabInsets", new Insets(8, 4, 8, 12));
        viewTabbedPane.putClientProperty("JTabbedPane.tabAlignment", "leading");

        viewTabbedPane.add(Language.translate("General"), new GeneralView());
//        viewTabbedPane.add("Jar Mods", new JarModsView());
//        viewTabbedPane.add("Java Agents", new JavaAgentsView());
        viewTabbedPane.add(Language.translate("Notes"), new NotesView());
        viewTabbedPane.add(Language.translate("Settings"), new SettingsView());

        viewTabbedPane.setIconAt(0, SvgIcon.get("icons/general"));
//        viewTabbedPane.setIconAt(1, SvgIcon.get("icons/java"));
//        viewTabbedPane.setIconAt(2, SvgIcon.get("icons/java"));
        viewTabbedPane.setIconAt(1, SvgIcon.get("icons/patch_notes"));
        viewTabbedPane.setIconAt(2, SvgIcon.get("icons/general"));

        this.add(viewTabbedPane, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new GridBagLayout());
        footerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton openFolderButton = new JButton(Language.translate("Open Folder"));
        openFolderButton.addActionListener(e -> OperatingSystem.get().open(this.instance.path));

        JButton launchButton = new JButton(Language.translate("Launch"));

        launchButton.addActionListener(e -> {
            if (this.instance.isRunning.get())
                return;

            SwingUtilities.invokeLater(() -> LauncherGui.getInstance().startTask(null, new InstanceLaunchTask(this.instance)));
        });

        JButton killButton = new JButton(Language.translate("Kill"));
        killButton.addActionListener(e -> {
            if (instance.runningProcess.isAlive()) {
                instance.runningProcess.destroyForcibly();
            }
        });

        this.instance.isRunning.signal.connect(this, () -> {
            launchButton.setEnabled(!this.instance.isRunning.get());
            killButton.setEnabled(this.instance.isRunning.get());
        });

        launchButton.setEnabled(!instance.isRunning.get());
        killButton.setEnabled(instance.isRunning.get());

        JButton closeButton = new JButton(Language.translate("Close"));
        closeButton.addActionListener(e -> this.close());

        footerPanel.add(openFolderButton);
        footerPanel.add(new JPanel(), new GridBagContrs().fillH().pos(1, 0));
        footerPanel.add(launchButton, new GridBagContrs().pos(2, 0).insetsH(0, 8));
        footerPanel.add(killButton, new GridBagContrs().pos(3, 0).insetsH(0, 8));
        footerPanel.add(closeButton);

        this.add(footerPanel, BorderLayout.SOUTH);

        this.getContentPane().setPreferredSize(new Dimension(600, 500));
        SwingUtils.registerEscapeAction(this.getRootPane(), this::dispose);

        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }

    private void close() {
        this.dispose();
        XLauncher.getInstance().getInstanceManager().save(this.instance);
        this.onClose.run();
    }

    class GeneralView extends JPanel {
        public GeneralView() {
            super(new GridBagLayout());
            this.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            JPanel contentPanel = new JPanel(new GridBagLayout());

            JLabel nameLabel = new JLabel(Language.translate("Name"));
            JTextField nameField = new JTextField();
            nameField.setText(InstanceDialog.this.instance.name);
            nameField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
                if (!nameField.getText().trim().isEmpty()) {
                    InstanceDialog.this.instance.name = nameField.getText();
                }
            }));

            JLabel versionLabel = new JLabel(Language.translate("Version"));
            JComboBox<Version> versionCombo = new JComboBox<>(SwingUtils.createImmutComboBoxModel(XLauncher.getInstance().getVersionManager().versions));
            XLauncher.getInstance().getVersionManager().findVersion(InstanceDialog.this.instance.version).ifPresent(version -> versionCombo.setSelectedItem(version));

            versionCombo.addItemListener(e -> {
                if (e.getStateChange() != ItemEvent.SELECTED) return;
                InstanceDialog.this.instance.version = ((Version) versionCombo.getSelectedItem()).id;
            });

            contentPanel.add(nameLabel, new GridBagContrs().pos(0, 1).insetsH(0, 16));
            contentPanel.add(nameField, new GridBagContrs().pos(1, 1).weight(1, 0).fill(true, false));

            contentPanel.add(versionLabel, new GridBagContrs().pos(0, 2).insetsH(0, 16).insetsV(8, 0));
            contentPanel.add(versionCombo, new GridBagContrs().pos(1, 2).weight(1, 0).fill(true, false).insetsV(8, 0));

            this.add(contentPanel, new GridBagContrs().fill(true, false).weight(1, 0).pos(0, 1));

            JTextPane logArea = new JTextPane();
            DefaultListModel<String> logModel = XLauncher.getInstance().getInstanceManager().logs.computeIfAbsent(instance, key -> new DefaultListModel<>());
            logModel.addListDataListener(new ListDataListener() {
                private final SimpleAttributeSet attrs = new SimpleAttributeSet();

                @Override
                public void intervalAdded(ListDataEvent e) {
                    try {
                        String line = logModel.get(e.getIndex0());
                        if (line.contains("DEBUG")) {
                            StyleConstants.setForeground(attrs, Color.CYAN);
                        } else if (line.contains("ERROR")) {
                            StyleConstants.setForeground(attrs, Color.RED);
                        } else if (line.contains("WARN") || line.contains("WARNING")) {
                            StyleConstants.setForeground(attrs, Color.ORANGE);
                        } else {
                            StyleConstants.setForeground(attrs, Color.WHITE);
                        }

                        logArea.getDocument().insertString(logArea.getDocument().getLength(), line + "\n", attrs);
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                }

                @Override
                public void intervalRemoved(ListDataEvent e) {
                    logArea.setText("");
                }

                @Override
                public void contentsChanged(ListDataEvent e) {
                }
            });

            SimpleAttributeSet attrs = new SimpleAttributeSet();
            for (int i = 0; i < logModel.getSize(); ++i) {
                try {
                    String line = logModel.get(i);
                    if (line.contains("DEBUG")) {
                        StyleConstants.setForeground(attrs, Color.CYAN);
                    } else if (line.contains("ERROR")) {
                        StyleConstants.setForeground(attrs, Color.RED);
                    } else if (line.contains("WARN") || line.contains("WARNING")) {
                        StyleConstants.setForeground(attrs, Color.ORANGE);
                    } else {
                        StyleConstants.setForeground(attrs, Color.WHITE);
                    }

                    logArea.getDocument().insertString(logArea.getDocument().getLength(), line + "\n", attrs);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }

            this.add(new JLabel("Logs"), new GridBagContrs().pos(0, 2).insetsV(8, 0).anchor(GridBagConstraints.NORTHWEST));
            this.add(logArea, new GridBagContrs().fill(true, true).weight(1, 1).insetsV(8, 0).pos(0, 3));
        }
    }

    class JarModsView extends JPanel {
        public JarModsView() {
            super(new GridBagLayout());
            this.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            JTable table = new JTable(new JarModsTableModel());
            JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());

            JButton addButton = new JButton("Add");
            JButton removeButton = new JButton("Remove");

            JPanel bottomPanel = new JPanel(new GridBagLayout());
            bottomPanel.add(addButton, new GridBagContrs().pos(0, 0));
            bottomPanel.add(removeButton, new GridBagContrs().pos(1, 0).insetsH(8, 0));
            bottomPanel.add(new JPanel(), new GridBagContrs().pos(2, 0).weight(1, 0).fill(true, false));

            this.add(scrollPane, new GridBagContrs().fill(true, true).pos(0, 1).weight(1, 1));
            this.add(bottomPanel, new GridBagContrs().fill(true, false).weight(1, 0).pos(0, 2).insetsV(8, 0));
        }
    }

    static class JarModsTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return 0;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "Name";
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return "";
        }
    }

    class JavaAgentsView extends JPanel {
        public JavaAgentsView() {
            super(new GridBagLayout());
            this.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            JTable table = new JTable(new JavaAgentTableModel());
            JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());

            JButton addButton = new JButton("Add");
            JButton removeButton = new JButton("Remove");

            JPanel bottomPanel = new JPanel(new GridBagLayout());
            bottomPanel.add(addButton, new GridBagContrs().pos(0, 0));
            bottomPanel.add(removeButton, new GridBagContrs().pos(1, 0).insetsH(8, 0));
            bottomPanel.add(new JPanel(), new GridBagContrs().pos(2, 0).weight(1, 0).fill(true, false));

            this.add(scrollPane, new GridBagContrs().fill(true, true).pos(0, 1).weight(1, 1));
            this.add(bottomPanel, new GridBagContrs().fill(true, false).weight(1, 0).pos(0, 2).insetsV(8, 0));
        }
    }

    static class JavaAgentTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return 0;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "Name";
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return "";
        }
    }

    class NotesView extends JPanel {
        public NotesView() {
            super(new GridBagLayout());
            this.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            JTextArea notesArea = new JTextArea();
            notesArea.setText(InstanceDialog.this.instance.notes);

            JScrollPane scrollPane = new JScrollPane(notesArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());

            notesArea.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
                InstanceDialog.this.instance.notes = notesArea.getText();
                System.out.println(InstanceDialog.this.instance.notes);
            }));

            this.add(scrollPane, new GridBagContrs().fill(true, true).pos(0, 1).weight(1, 1));
        }
    }

    class SettingsView extends JPanel {
        private final JCheckBox javaInstallCheckbox;
        private final JTextField javaInstallField;

        private final JCheckBox memoryCheckBox;
        private final JLabel javaMinMemAllocLabel;
        private final JSpinner javaMinMemAllocSpinner;
        private final JLabel javaMaxMemAllocLabel;
        private final JSpinner javaMaxMemAllocSpinner;

        private final JCheckBox jvmArgsCheckbox;
        private final JTextArea jvmArgsArea;

        public SettingsView() {
            super(new GridBagLayout());
            this.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            {
                this.javaInstallCheckbox = new JCheckBox("Java Installation");
                this.javaInstallField = new JTextField();

                JPopupMenu javaPathPopupMenu = this.buildJavaPathPopupMenu(path -> {
                    if (path.startsWith(XLauncher.getInstance().getJavaInstallationsPath())) {
                        this.javaInstallField.setText(XLauncher.getInstance().getJavaInstallationsPath().relativize(path).toString());
                    } else {
                        this.javaInstallField.setText(path.toString());
                    }
                    InstanceDialog.this.instance.javaInstallationPath = path;
                });
                this.javaInstallField.setEditable(false);

                if (InstanceDialog.this.instance.javaInstallationPath == null) {
                    this.javaInstallField.setText("java");
                } else if (InstanceDialog.this.instance.javaInstallationPath.startsWith(XLauncher.getInstance().getJavaInstallationsPath())) {
                    this.javaInstallField.setText(XLauncher.getInstance().getJavaInstallationsPath().relativize(InstanceDialog.this.instance.javaInstallationPath).toString());
                } else {
                    this.javaInstallField.setText(InstanceDialog.this.instance.javaInstallationPath.toString());
                }

                this.add(this.javaInstallCheckbox, new GridBagContrs().anchor(GridBagContrs.NORTHWEST));
                this.javaInstallField.addActionListener(event -> javaPathPopupMenu.show(this.javaInstallField, 0, this.javaInstallField.getHeight()));

                this.javaInstallField.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent event) {
                        if (event.getKeyCode() == KeyEvent.VK_CONTEXT_MENU && SettingsView.this.javaInstallField.isEnabled()) {
                            javaPathPopupMenu.show(SettingsView.this.javaInstallField, 0, SettingsView.this.javaInstallField.getHeight());
                            event.consume();
                        }
                    }
                });

                this.javaInstallField.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseReleased(MouseEvent event) {
                        if (!SettingsView.this.javaInstallField.isEnabled()) return;
                        javaPathPopupMenu.show(SettingsView.this.javaInstallField, 0, SettingsView.this.javaInstallField.getHeight());
                    }
                });

                javaPathPopupMenu.addMenuKeyListener(new MenuKeyListener() {
                    private void helper(final MenuKeyEvent event) {
                        switch (event.getKeyCode()) {
                            case KeyEvent.VK_ENTER:
                            case KeyEvent.VK_SHIFT:
                            case KeyEvent.VK_CONTROL:
                            case KeyEvent.VK_ESCAPE:
                            case KeyEvent.VK_SPACE:
                            case KeyEvent.VK_UP:
                            case KeyEvent.VK_DOWN:
                            case KeyEvent.VK_LEFT:
                            case KeyEvent.VK_RIGHT:
                            case KeyEvent.VK_META:
                            case KeyEvent.VK_KP_LEFT:
                            case KeyEvent.VK_KP_UP:
                            case KeyEvent.VK_KP_RIGHT:
                            case KeyEvent.VK_KP_DOWN:
                            case KeyEvent.VK_ALT_GRAPH:
                                return;
                        }

                        switch (event.getKeyChar()) {
                            case '\n':
                            case ' ':
                                return;
                        }

                        event.consume();

                        SwingUtilities.invokeLater(() -> javaPathPopupMenu.setVisible(false));

                        SettingsView.this.javaInstallField.dispatchEvent(new KeyEvent(
                                SettingsView.this.javaInstallField,
                                event.getID(),
                                event.getWhen(),
                                event.getModifiersEx(),
                                event.getKeyCode(),
                                event.getKeyChar(),
                                event.getKeyLocation()
                        ));
                    }

                    @Override
                    public void menuKeyTyped(final MenuKeyEvent event) {
                        if (!SettingsView.this.javaInstallField.isEnabled()) return;
                        this.helper(event);
                    }

                    @Override
                    public void menuKeyPressed(final MenuKeyEvent event) {
                        if (!SettingsView.this.javaInstallField.isEnabled()) return;
                        this.helper(event);
                    }

                    @Override
                    public void menuKeyReleased(final MenuKeyEvent event) {
                        if (!SettingsView.this.javaInstallField.isEnabled()) return;
                        this.helper(event);
                    }
                });

                JPanel javaPanel = new JPanel(new BorderLayout());
                TitledBorder border = BorderFactory.createTitledBorder("");
                javaPanel.setBorder(new CompoundBorder(border, BorderFactory.createEmptyBorder(8, 8, 8, 8)));
                javaPanel.add(this.javaInstallField, BorderLayout.CENTER);

                this.javaInstallCheckbox.setSelected(InstanceDialog.this.instance.javaInstallation);
                this.javaInstallField.setEnabled(this.javaInstallCheckbox.isSelected());
                this.javaInstallCheckbox.addActionListener(e -> {
                    instance.javaInstallation = this.javaInstallCheckbox.isSelected();
                    this.javaInstallField.setEnabled(this.javaInstallCheckbox.isSelected());
                });

                this.add(javaPanel, new GridBagContrs().fillH().pos(0, 1).insetsV(4, 0).anchor(GridBagContrs.NORTH));
            }

            {
                this.memoryCheckBox = new JCheckBox("Override Memory");

                this.add(this.memoryCheckBox, new GridBagContrs().pos(0, 2).insetsV(8, 0).anchor(GridBagContrs.NORTHWEST));

                this.javaMinMemAllocLabel = new JLabel(Language.translate("Minimum memory allocation"));
                SpinnerNumberModel minModel = new SpinnerNumberModel();
                minModel.setMinimum(0);
                minModel.setValue(XLauncher.getInstance().getSettings().javaMinimumAlloc);
                minModel.setStepSize(128);
                this.javaMinMemAllocSpinner = new JSpinner(minModel);
                this.javaMinMemAllocSpinner.setEditor(new JSpinner.NumberEditor(this.javaMinMemAllocSpinner, "# MiB"));

                this.javaMaxMemAllocLabel = new JLabel(Language.translate("Maximum memory allocation"));
                SpinnerNumberModel maxModel = new SpinnerNumberModel();
                maxModel.setMinimum(0);
                maxModel.setValue(XLauncher.getInstance().getSettings().javaMaximumAlloc);
                maxModel.setStepSize(128);
                this.javaMaxMemAllocSpinner = new JSpinner(maxModel);
                this.javaMaxMemAllocSpinner.setEditor(new JSpinner.NumberEditor(this.javaMaxMemAllocSpinner, "# MiB"));

                JPanel javaPanel = new JPanel(new GridBagLayout());
                TitledBorder border = BorderFactory.createTitledBorder("");
                javaPanel.setBorder(new CompoundBorder(border, BorderFactory.createEmptyBorder(0, 8, 8, 8)));

                javaPanel.add(this.javaMinMemAllocLabel, new GridBagContrs().pos(0, 1).anchor(GridBagContrs.WEST));
                javaPanel.add(new JPanel(), new GridBagContrs().fillH().pos(1, 1));
                javaPanel.add(this.javaMinMemAllocSpinner, new GridBagContrs().pos(2, 1));

                javaPanel.add(this.javaMaxMemAllocLabel, new GridBagContrs().insetsV(8, 0).pos(0, 2).anchor(GridBagContrs.WEST));
                javaPanel.add(new JPanel(), new GridBagContrs().fillH().weight(1, 2).pos(1, 0));
                javaPanel.add(this.javaMaxMemAllocSpinner, new GridBagContrs().pos(2, 2).insetsV(8, 0));

                this.memoryCheckBox.setSelected(InstanceDialog.this.instance.overrideJavaMemory);
                this.updateMemory(this.memoryCheckBox.isSelected());

                this.memoryCheckBox.addActionListener(e -> this.updateMemory(this.memoryCheckBox.isSelected()));

                this.add(javaPanel, new GridBagContrs().fillH().pos(0, 3).insetsV(4, 0).anchor(GridBagContrs.NORTH));
            }

            {
                this.jvmArgsCheckbox = new JCheckBox("Override JVM Arguments");
                this.jvmArgsArea = new JTextArea();
                this.jvmArgsArea.setPreferredSize(new Dimension(0, 80));

                this.add(this.jvmArgsCheckbox, new GridBagContrs().pos(0, 4).insetsV(8, 0).anchor(GridBagContrs.NORTHWEST));

                JPanel javaPanel = new JPanel(new BorderLayout());
                TitledBorder border = BorderFactory.createTitledBorder("");
                javaPanel.setBorder(new CompoundBorder(border, BorderFactory.createEmptyBorder(8, 8, 8, 8)));
                javaPanel.add(this.jvmArgsArea, BorderLayout.CENTER);

                this.add(javaPanel, new GridBagContrs().fillH().pos(0, 5).insetsV(4, 0).anchor(GridBagContrs.NORTH));

                this.jvmArgsCheckbox.setSelected(InstanceDialog.this.instance.overrideJvmArguments);
                this.jvmArgsArea.setEnabled(this.jvmArgsCheckbox.isSelected());
                this.jvmArgsCheckbox.addActionListener(e -> this.jvmArgsArea.setEnabled(this.jvmArgsCheckbox.isSelected()));
            }

            this.add(new JPanel(), new GridBagContrs().fill(true, true).weight(1, 1).pos(0, 6));
        }

        private void updateMemory(boolean enabled) {
            this.javaMinMemAllocLabel.setEnabled(enabled);
            this.javaMaxMemAllocLabel.setEnabled(enabled);
            this.javaMinMemAllocSpinner.setEnabled(enabled);
            this.javaMaxMemAllocSpinner.setEnabled(enabled);
        }

        private JPopupMenu buildJavaPathPopupMenu(Consumer<Path> onPathSelect) {
            JPopupMenu popupMenu = new JPopupMenu();

            JMenuItem browseMenuItem = new JMenuItem("Browse...");
            browseMenuItem.addActionListener(e -> {
                SystemFileChooser fileChooser = new SystemFileChooser();
                fileChooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
                if (fileChooser.showOpenDialog(InstanceDialog.this) == SystemFileChooser.APPROVE_OPTION) {
                    onPathSelect.accept(fileChooser.getSelectedFile().toPath().toAbsolutePath());
                }
            });
            popupMenu.add(browseMenuItem);

            popupMenu.addSeparator();

            for (LocalJavaRuntime runtime : XLauncher.getInstance().getJavaRuntimeManager().getDownloaded()) {
                JMenuItem menuItem = new JMenuItem(runtime.path.toString());
                menuItem.addActionListener(e -> onPathSelect.accept(runtime.path));
                popupMenu.add(menuItem);
            }

            return popupMenu;
        }
    }
}
