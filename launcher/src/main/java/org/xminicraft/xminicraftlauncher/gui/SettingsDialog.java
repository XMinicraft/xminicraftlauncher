package org.xminicraft.xminicraftlauncher.gui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.SystemFileChooser;
import org.xminicraft.xminicraftlauncher.Language;
import org.xminicraft.xminicraftlauncher.XLauncher;
import org.xminicraft.xminicraftlauncher.gui.theme.DarkLauncherLaf;
import org.xminicraft.xminicraftlauncher.gui.theme.LightLauncherLaf;
import org.xminicraft.xminicraftlauncher.gui.util.GridBagContrs;
import org.xminicraft.xminicraftlauncher.gui.util.SimpleDocumentListener;
import org.xminicraft.xminicraftlauncher.gui.util.SvgIcon;
import org.xminicraft.xminicraftlauncher.gui.util.SwingUtils;
import org.xminicraft.xminicraftlauncher.java.LocalJavaRuntime;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SettingsDialog extends JDialog {
    private final JTabbedPane viewTabbedPane;
    private final GeneralView generalView;
    private final JavaView javaView;
    private final EnvironmentVariablesView environmentVariablesView;

    public SettingsDialog(JFrame owner) {
        super(owner, Language.translate("Settings"));
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                SettingsDialog.this.close();
            }
        });

        this.viewTabbedPane = new JTabbedPane();
        this.viewTabbedPane.setTabPlacement(JTabbedPane.LEFT);
        this.viewTabbedPane.putClientProperty("JTabbedPane.tabInsets", new Insets(8, 4, 8, 12));
        this.viewTabbedPane.putClientProperty("JTabbedPane.tabAlignment", "leading");

        this.generalView = new GeneralView(this);
        this.javaView = new JavaView(this);
        this.environmentVariablesView = new EnvironmentVariablesView();

        this.viewTabbedPane.add(Language.translate("General"), this.generalView);
        this.viewTabbedPane.add("Java", this.javaView);
//        this.viewTabbedPane.add(Language.translate("Environment Variables"), this.environmentVariablesView);

        this.viewTabbedPane.setIconAt(0, SvgIcon.get("icons/general"));
        this.viewTabbedPane.setIconAt(1, SvgIcon.get("icons/java"));
//        this.viewTabbedPane.setIconAt(2, SvgIcon.get("icons/env_vars"));

        this.add(this.viewTabbedPane, BorderLayout.CENTER);

        this.getContentPane().setPreferredSize(new Dimension(670, 500));
        SwingUtils.registerEscapeAction(this.getRootPane(), this::close);

        this.setMinimumSize(new Dimension(560, 396));
        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }

    private void close() {
        this.dispose();
        this.environmentVariablesView.applySettings();
    }

    public void updateTranslations() {
        this.setTitle(Language.translate("Settings"));
        this.viewTabbedPane.setTitleAt(0, Language.translate("General"));
//        this.viewTabbedPane.setTitleAt(2, Language.translate("Environment Variables"));

        this.generalView.updateTranslations();
        this.javaView.updateTranslations();
        this.environmentVariablesView.updateTranslations();
    }

    private static class GeneralView extends JPanel {
        private final JLabel themeLabel;
        private final JComboBox<String> themeComboBox;
        private final JLabel languageLabel;
        private final JComboBox<String> languageComboBox;

        private final JLabel instancesFolderLabel;
        private final JTextField instancesFolderField;
        private final JButton instancesFolderBrowseButton;

        private final JLabel iconsFolderLabel;
        private final JTextField iconsFolderField;
        private final JButton iconsFolderBrowseButton;

        private final JLabel javaFolderLabel;
        private final JTextField javaFolderField;
        private final JButton javaFolderBrowseButton;

        private TitledBorder uiBorder;
        private TitledBorder foldersBorder;

        public GeneralView(SettingsDialog settingsDialog) {
            super(new BorderLayout());

            this.themeLabel = new JLabel(Language.translate("Theme"));
            this.themeComboBox = new JComboBox<>(new String[]{
                    "Dark",
                    "Light"
            });
            this.themeComboBox.setSelectedIndex("Dark".equals(XLauncher.getInstance().getSettings().theme) ? 0 : 1);
            this.themeComboBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.DESELECTED) return;
                XLauncher.getInstance().getSettings().theme = this.themeComboBox.getItemAt(this.themeComboBox.getSelectedIndex());
                if ("Dark".equals(XLauncher.getInstance().getSettings().theme)) {
                    DarkLauncherLaf.setup();
                } else {
                    LightLauncherLaf.setup();
                }
                FlatLaf.updateUILater();
            });

            this.languageLabel = new JLabel(Language.translate("Language"));
            this.languageComboBox = new JComboBox<>(new String[]{
                    "English",
                    "Português"
            });
            this.languageComboBox.setSelectedIndex("en_us".equals(XLauncher.getInstance().getSettings().language) ? 0 : 1);
            this.languageComboBox.addItemListener(e -> {
                if (e.getStateChange() != ItemEvent.SELECTED) return;
                int index = this.languageComboBox.getSelectedIndex();
                String langCode = index == 0 ? "en_us" : "pt_pt";
                XLauncher.getInstance().getSettings().language = langCode;
                Language.getInstance().load(langCode);
                LauncherGui.getInstance().updateTranslations();
                settingsDialog.updateTranslations();
            });

            this.instancesFolderLabel = new JLabel(Language.translate("Instances"));
            this.instancesFolderField = new JTextField();
            this.instancesFolderField.putClientProperty("JTextField.placeholderText", "instances");
            this.instancesFolderField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
                if (Files.exists(Paths.get(this.instancesFolderField.getText()))) {
                    XLauncher.getInstance().getSettings().instancesFolder = this.instancesFolderField.getText();
                }
            }));
            this.instancesFolderBrowseButton = new JButton(Language.translate("Browse"));
            this.instancesFolderBrowseButton.addActionListener(e -> this.chooseDirectory().ifPresent(path -> {
                this.instancesFolderField.setText(path.toString());
                XLauncher.getInstance().getSettings().instancesFolder = this.instancesFolderField.getText();
            }));

            this.instancesFolderField.setText(XLauncher.getInstance().getSettings().instancesFolder);

            this.iconsFolderLabel = new JLabel(Language.translate("Icons"));
            this.iconsFolderField = new JTextField();
            this.iconsFolderField.putClientProperty("JTextField.placeholderText", "icons");
            this.iconsFolderField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
                if (Files.exists(Paths.get(this.iconsFolderField.getText()))) {
                    XLauncher.getInstance().getSettings().iconsFolder = this.iconsFolderField.getText();
                }
            }));

            this.iconsFolderBrowseButton = new JButton(Language.translate("Browse"));
            this.iconsFolderBrowseButton.addActionListener(e -> this.chooseDirectory().ifPresent(path -> {
                this.iconsFolderField.setText(path.toString());
                XLauncher.getInstance().getSettings().iconsFolder = this.iconsFolderField.getText();
            }));

            this.iconsFolderField.setText(XLauncher.getInstance().getSettings().iconsFolder);

            this.javaFolderLabel = new JLabel("Java");
            this.javaFolderField = new JTextField();
            this.javaFolderField.putClientProperty("JTextField.placeholderText", "java");
            this.javaFolderField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
                if (Files.exists(Paths.get(this.javaFolderField.getText()))) {
                    XLauncher.getInstance().getSettings().javaFolder = this.javaFolderField.getText();
                }
            }));

            this.javaFolderBrowseButton = new JButton(Language.translate("Browse"));
            this.javaFolderBrowseButton.addActionListener(e -> this.chooseDirectory().ifPresent(path -> {
                this.javaFolderField.setText(path.toString());
                XLauncher.getInstance().getSettings().javaFolder = this.javaFolderField.getText();
            }));

            this.javaFolderField.setText(XLauncher.getInstance().getSettings().javaFolder);

            this.add(this.buildLayout(), BorderLayout.CENTER);
        }

        public void updateTranslations() {
            this.themeLabel.setText(Language.translate("Theme"));
            this.languageLabel.setText(Language.translate("Language"));
            this.instancesFolderLabel.setText(Language.translate("Instances"));
            this.iconsFolderLabel.setText(Language.translate("Icons"));
            this.instancesFolderBrowseButton.setText(Language.translate("Browse"));
            this.iconsFolderBrowseButton.setText(Language.translate("Browse"));
            this.javaFolderBrowseButton.setText(Language.translate("Browse"));

            this.uiBorder.setTitle(Language.translate("User Interface"));
            this.foldersBorder.setTitle(Language.translate("Folders"));
        }

        private Optional<Path> chooseDirectory() {
            SystemFileChooser fileChooser = new SystemFileChooser();
            fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);

            if (fileChooser.showOpenDialog(this) == SystemFileChooser.APPROVE_OPTION) {
                return Optional.of(fileChooser.getSelectedFile().toPath());
            }

            return Optional.empty();
        }

        private JScrollPane buildLayout() {
            JPanel rootPanel = new JPanel();
            rootPanel.setLayout(new GridBagLayout());
            rootPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

            {
                JPanel geralPanel = new JPanel(new GridBagLayout());
                this.uiBorder = BorderFactory.createTitledBorder(Language.translate("User Interface"));
                geralPanel.setBorder(new CompoundBorder(this.uiBorder, BorderFactory.createEmptyBorder(8, 8, 8, 8)));

                geralPanel.add(this.themeLabel, new GridBagContrs().anchor(GridBagContrs.WEST));
                geralPanel.add(this.themeComboBox, new GridBagContrs().fillH().insetsH(16, 0));

                geralPanel.add(this.languageLabel, new GridBagContrs().insetsV(8, 0).pos(0, 1).anchor(GridBagContrs.WEST));
                geralPanel.add(this.languageComboBox, new GridBagContrs().fillH().pos(1, 1).insetsH(16, 0).insetsV(8, 0));

                rootPanel.add(geralPanel, new GridBagContrs().fillH().anchor(GridBagContrs.NORTH));
            }

            {
                JPanel foldersPanel = new JPanel(new GridBagLayout());
                this.foldersBorder = BorderFactory.createTitledBorder(Language.translate("Folders"));
                foldersPanel.setBorder(new CompoundBorder(this.foldersBorder, BorderFactory.createEmptyBorder(8, 8, 8, 8)));

                foldersPanel.add(this.instancesFolderLabel, new GridBagContrs().anchor(GridBagContrs.WEST));
                foldersPanel.add(this.instancesFolderField, new GridBagContrs().fillH().insetsH(16, 8));
                foldersPanel.add(this.instancesFolderBrowseButton);

                foldersPanel.add(this.iconsFolderLabel, new GridBagContrs().pos(0, 2).insetsV(8, 0).anchor(GridBagContrs.WEST));
                foldersPanel.add(this.iconsFolderField, new GridBagContrs().fillH().insetsH(16, 8).insetsV(8, 0).pos(1, 2));
                foldersPanel.add(this.iconsFolderBrowseButton, new GridBagContrs().pos(2, 2).insetsV(8, 0));

                foldersPanel.add(this.javaFolderLabel, new GridBagContrs().pos(0, 1).insetsV(8, 0).anchor(GridBagContrs.WEST));
                foldersPanel.add(this.javaFolderField, new GridBagContrs().fillH().insetsH(16, 8).insetsV(8, 0).pos(1, 1));
                foldersPanel.add(this.javaFolderBrowseButton, new GridBagContrs().pos(2, 1).insetsV(8, 0));

                rootPanel.add(foldersPanel, new GridBagContrs().fillH().pos(0, 1).insetsV(8, 0).anchor(GridBagContrs.NORTH));
            }

            rootPanel.add(new JPanel(), new GridBagContrs().fill(true, true).weight(1, 1).pos(0, 4));

            JScrollPane rootScrollPane = new JScrollPane(rootPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            rootScrollPane.setBorder(BorderFactory.createEmptyBorder());

            return rootScrollPane;
        }
    }

    private class JavaView extends JPanel {
        private final JCheckBox memoryCheckBox;
        private final JLabel javaMinMemAllocLabel;
        private final JSpinner javaMinMemAllocSpinner;
        private final JLabel javaMaxMemAllocLabel;
        private final JSpinner javaMaxMemAllocSpinner;

        private final JTable table;

        private final JButton downloadButton;
        private final JButton removeButton;
        private final JButton refreshButton;

        private final JTextField searchField;

        private TitledBorder runtimesBorder;

        public JavaView(SettingsDialog settingsDialog) {
            super(new BorderLayout());

            this.memoryCheckBox = new JCheckBox("Memory");

            this.javaMinMemAllocLabel = new JLabel(Language.translate("Minimum memory allocation"));
            SpinnerNumberModel minModel = SwingUtils.createSpinnerNumberModel(0, null, 128, XLauncher.getInstance().getSettings().javaMinimumAlloc);

            this.javaMinMemAllocSpinner = new JSpinner(minModel);
            this.javaMinMemAllocSpinner.setEditor(new JSpinner.NumberEditor(this.javaMinMemAllocSpinner, "# MiB"));
            minModel.addChangeListener(e -> XLauncher.getInstance().getSettings().javaMinimumAlloc = minModel.getNumber().intValue());

            this.javaMaxMemAllocLabel = new JLabel(Language.translate("Maximum memory allocation"));
            SpinnerNumberModel maxModel = SwingUtils.createSpinnerNumberModel(0, null, 128, XLauncher.getInstance().getSettings().javaMaximumAlloc);

            this.javaMaxMemAllocSpinner = new JSpinner(maxModel);
            this.javaMaxMemAllocSpinner.setEditor(new JSpinner.NumberEditor(this.javaMaxMemAllocSpinner, "# MiB"));
            maxModel.addChangeListener(e -> XLauncher.getInstance().getSettings().javaMaximumAlloc = maxModel.getNumber().intValue());

            JavaVersionsTableModel tableModel = new JavaVersionsTableModel();
            this.table = new JTable(tableModel);
            this.table.getColumnModel().getColumn(0).setPreferredWidth(100);
            this.table.getColumnModel().getColumn(1).setPreferredWidth(80);
            this.table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            JTableHeader header = this.table.getTableHeader();
            TableColumn lastColumn = header.getColumnModel().getColumn(1);
            header.setResizingColumn(lastColumn);

            this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            this.downloadButton = new JButton(Language.translate("Download"));
            this.downloadButton.addActionListener(e -> new JavaDownloadDialog(settingsDialog));

            this.removeButton = new JButton(Language.translate("Remove"));
            this.refreshButton = new JButton(Language.translate("Refresh"));
            this.refreshButton.addActionListener(e -> tableModel.refresh());

            this.searchField = new JTextField();
            this.searchField.putClientProperty("JTextField.placeholderText", Language.translate("Search..."));

            this.memoryCheckBox.addActionListener(e -> {
                boolean checked = this.memoryCheckBox.isSelected();
                XLauncher.getInstance().getSettings().javaMemory = checked;

                this.javaMinMemAllocLabel.setEnabled(checked);
                this.javaMaxMemAllocLabel.setEnabled(checked);
                this.javaMinMemAllocSpinner.setEnabled(checked);
                this.javaMaxMemAllocSpinner.setEnabled(checked);
            });

            boolean javaMemory = XLauncher.getInstance().getSettings().javaMemory;

            this.memoryCheckBox.setSelected(javaMemory);
            this.javaMinMemAllocLabel.setEnabled(javaMemory);
            this.javaMaxMemAllocLabel.setEnabled(javaMemory);
            this.javaMinMemAllocSpinner.setEnabled(javaMemory);
            this.javaMaxMemAllocSpinner.setEnabled(javaMemory);

            this.add(this.buildLayout(), BorderLayout.CENTER);
        }

        public void updateTranslations() {
            this.javaMinMemAllocLabel.setText(Language.translate("Minimum memory allocation"));
            this.javaMaxMemAllocLabel.setText(Language.translate("Maximum memory allocation"));

            this.downloadButton.setText(Language.translate("Download"));
            this.removeButton.setText(Language.translate("Remove"));
            this.refreshButton.setText(Language.translate("Refresh"));

            this.searchField.putClientProperty("JTextField.placeholderText", Language.translate("Search..."));

            this.memoryCheckBox.setText(Language.translate("Memory"));
            this.runtimesBorder.setTitle(Language.translate("Java Runtimes"));
        }

        private JScrollPane buildLayout() {
            JPanel rootPanel = new JPanel();
            rootPanel.setLayout(new GridBagLayout());
            rootPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

            {
                rootPanel.add(this.memoryCheckBox, new GridBagContrs().anchor(GridBagContrs.NORTHWEST));

                JPanel javaPanel = new JPanel(new GridBagLayout());
                TitledBorder border = BorderFactory.createTitledBorder("");
                javaPanel.setBorder(new CompoundBorder(border, BorderFactory.createEmptyBorder(0, 8, 8, 8)));

                javaPanel.add(this.javaMinMemAllocLabel, new GridBagContrs().pos(0, 1).anchor(GridBagContrs.WEST));
                javaPanel.add(new JPanel(), new GridBagContrs().fillH().pos(1, 1));
                javaPanel.add(this.javaMinMemAllocSpinner, new GridBagContrs().pos(2, 1));

                javaPanel.add(this.javaMaxMemAllocLabel, new GridBagContrs().insetsV(8, 0).pos(0, 2).anchor(GridBagContrs.WEST));
                javaPanel.add(new JPanel(), new GridBagContrs().fillH().weight(1, 2).pos(1, 0));
                javaPanel.add(this.javaMaxMemAllocSpinner, new GridBagContrs().pos(2, 2).insetsV(8, 0));

                rootPanel.add(javaPanel, new GridBagContrs().fillH().pos(0, 1).insetsV(4, 0).anchor(GridBagContrs.NORTH));
            }

            {
                JPanel javaPanel = new JPanel(new GridBagLayout());
                this.runtimesBorder = BorderFactory.createTitledBorder(Language.translate("Java Runtimes"));
                javaPanel.setBorder(new CompoundBorder(this.runtimesBorder, BorderFactory.createEmptyBorder(8, 8, 8, 8)));

                JScrollPane tableScrollPane = new JScrollPane(this.table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                tableScrollPane.setPreferredSize(new Dimension(0, 150));
                javaPanel.add(tableScrollPane, new GridBagContrs().fill(true, true).weight(1, 1).pos(0, 2).size(3, 1));

                JPanel bottomPanel = new JPanel(new GridBagLayout());

                bottomPanel.add(this.downloadButton, new GridBagContrs().pos(0, 0));
                bottomPanel.add(this.removeButton, new GridBagContrs().pos(1, 0).insetsH(8, 0));
                bottomPanel.add(this.refreshButton, new GridBagContrs().pos(2, 0).insetsH(8, 0));
                bottomPanel.add(this.searchField, new GridBagContrs().pos(3, 0).fillH().insetsH(8, 0).weight(1, 0));

                javaPanel.add(bottomPanel, new GridBagContrs().fillH().weight(2, 0).pos(0, 3).size(3, 0).insetsV(8, 0));

                rootPanel.add(javaPanel, new GridBagContrs().fillH().pos(0, 2).insetsV(8, 0).anchor(GridBagContrs.NORTH));
            }

            rootPanel.add(new JPanel(), new GridBagContrs().fill(true, true).weight(1, 1).pos(0, 3));

            JScrollPane rootScrollPane = new JScrollPane(rootPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            rootScrollPane.setBorder(BorderFactory.createEmptyBorder());
            return rootScrollPane;
        }
    }

    private class JavaVersionsTableModel extends AbstractTableModel {
        private List<LocalJavaRuntime> runtimes;

        public JavaVersionsTableModel() {
            this.runtimes = XLauncher.getInstance().getJavaRuntimeManager().getDownloaded();
        }

        public void refresh() {
            LauncherGui.getInstance().startTask(SettingsDialog.this, XLauncher.getInstance().getJavaRuntimeManager().refresh(true));
            this.runtimes = XLauncher.getInstance().getJavaRuntimeManager().getDownloaded();
            this.fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return this.runtimes.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0: return Language.translate("Version");
                case 1: return Language.translate("Architecture");
                case 2: return Language.translate("Path");
                default: return "";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            LocalJavaRuntime runtime = this.runtimes.get(rowIndex);
            switch (columnIndex) {
                case 0: return runtime.version;
                case 1: return runtime.architecture;
                case 2:
                {
                    Path javaPath = XLauncher.getInstance().getJavaInstallationsPath().toAbsolutePath();
                    if (runtime.path.startsWith(javaPath)) {
                        return javaPath.getParent().relativize(runtime.path);
                    }
                    return runtime.path;
                }
            }
            return "";
        }
    }

    private static class EnvironmentVariablesView extends JPanel {
        private final EnvVarsTableModel envVarsTableModel;
        private final JTable table;

        private final JButton addEnvVarButton;
        private final JButton removeEnvVarButton;
        private final JButton clearEnvVarButton;

        private TitledBorder envVarsBorder;

        public EnvironmentVariablesView() {
            super(new BorderLayout());

            this.envVarsTableModel = new EnvVarsTableModel();
            this.table = new JTable(this.envVarsTableModel);
            this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            this.addEnvVarButton = new JButton(Language.translate("Add"));
            this.addEnvVarButton.addActionListener(e -> {
                this.envVarsTableModel.addNewRow();
                this.table.getSelectionModel().setSelectionInterval(this.envVarsTableModel.getRowCount() - 1, this.envVarsTableModel.getRowCount() - 1);
            });
            this.removeEnvVarButton = new JButton(Language.translate("Remove"));
            this.removeEnvVarButton.addActionListener(e -> {
                this.envVarsTableModel.removeRowAt(this.table.getSelectionModel().getAnchorSelectionIndex());
                this.table.getSelectionModel().setSelectionInterval(this.envVarsTableModel.getRowCount() - 1, this.envVarsTableModel.getRowCount() - 1);
            });
            this.clearEnvVarButton = new JButton(Language.translate("Clear"));
            this.clearEnvVarButton.addActionListener(e -> this.envVarsTableModel.clear());

            this.add(this.buildLayout(), BorderLayout.CENTER);
        }

        public void applySettings() {
            List<String[]> envVars = new ArrayList<>();
            for (String[] envVar : this.envVarsTableModel.getEnvVars()) {
                String name = envVar[0].trim();
                if (name.isEmpty()) continue;
                String value = envVar[1].trim();
                envVars.add(new String[]{name, value});
            }
            XLauncher.getInstance().getSettings().envVars = envVars;
        }

        public void updateTranslations() {
            this.addEnvVarButton.setText(Language.translate("Add"));
            this.removeEnvVarButton.setText(Language.translate("Remove"));
            this.clearEnvVarButton.setText(Language.translate("Clear"));

            this.envVarsBorder.setTitle(Language.translate("Environment Variables"));
        }

        private JScrollPane buildLayout() {
            JPanel rootPanel = new JPanel();
            rootPanel.setLayout(new GridBagLayout());
            rootPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

            JPanel envVarsPanel = new JPanel(new GridBagLayout());
            this.envVarsBorder = BorderFactory.createTitledBorder(Language.translate("Environment Variables"));
            envVarsPanel.setBorder(new CompoundBorder(this.envVarsBorder, BorderFactory.createEmptyBorder(8, 8, 8, 8)));

            JScrollPane tableScrollPane = new JScrollPane(this.table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            tableScrollPane.setPreferredSize(new Dimension(0, 150));
            envVarsPanel.add(tableScrollPane, new GridBagContrs().fill());

            JPanel bottomEnvVarPanel = new JPanel(new GridBagLayout());

            bottomEnvVarPanel.add(this.addEnvVarButton);
            bottomEnvVarPanel.add(this.removeEnvVarButton, new GridBagContrs().pos(1, 0).insetsH(8, 0));
            bottomEnvVarPanel.add(this.clearEnvVarButton, new GridBagContrs().pos(2, 0).insetsH(8, 0));
            bottomEnvVarPanel.add(new JPanel(), new GridBagContrs().pos(3, 0).fillH());

            envVarsPanel.add(bottomEnvVarPanel, new GridBagContrs().pos(0, 1).fillH().insetsV(8, 0));
            rootPanel.add(envVarsPanel, new GridBagContrs().fillH().pos(0, 3).anchor(GridBagContrs.NORTH));

            rootPanel.add(new JPanel(), new GridBagContrs().fill(true, true).weight(1, 1).pos(0, 4));

            JScrollPane rootScrollPane = new JScrollPane(rootPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            rootScrollPane.setBorder(BorderFactory.createEmptyBorder());
            return rootScrollPane;
        }
    }

    private static class EnvVarsTableModel extends AbstractTableModel {
        private final List<String[]> envVars = new ArrayList<>();

        public List<String[]> getEnvVars() {
            return this.envVars;
        }

        @Override
        public int getRowCount() {
            return this.envVars.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? "Name" : "Value";
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return this.envVars.get(rowIndex)[columnIndex];
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            this.envVars.get(rowIndex)[columnIndex] = String.valueOf(aValue);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        public void addNewRow() {
            int index = this.envVars.size();
            this.envVars.add(new String[]{"ENV_VAR", "VALUE"});
            this.fireTableRowsInserted(index, index);
        }

        public void removeRowAt(int index) {
            if (index < 0 || index >= this.envVars.size()) return;
            this.envVars.remove(index);
            this.fireTableRowsDeleted(index, index);
        }

        public void clear() {
            if (this.envVars.isEmpty()) return;
            int size = this.envVars.size();
            this.envVars.clear();
            this.fireTableRowsDeleted(0, size - 1);
        }
    }
}
