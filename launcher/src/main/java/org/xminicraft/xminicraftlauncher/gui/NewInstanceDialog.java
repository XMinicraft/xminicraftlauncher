package org.xminicraft.xminicraftlauncher.gui;

import org.xminicraft.xminicraftlauncher.Language;
import org.xminicraft.xminicraftlauncher.XLauncher;
import org.xminicraft.xminicraftlauncher.gui.util.GridBagContrs;
import org.xminicraft.xminicraftlauncher.gui.util.SimpleDocumentListener;
import org.xminicraft.xminicraftlauncher.gui.util.SwingUtils;
import org.xminicraft.xminicraftlauncher.util.InstanceCreateTask;
import org.xminicraft.xminicraftlauncher.version.Version;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Vector;

public class NewInstanceDialog extends JDialog {
    private final JTextField nameField;
    private final JComboBox<String> groupCombo;
    private final JTable versionsTable;

    private final JCheckBox javaFilterCheckbox;
    private final JCheckBox executableFilterCheckbox;
    private final JTextField searchField;

    private final JTextField resultField;
    private final JButton createButton;
    private final JButton cancelButton;

    public NewInstanceDialog(JFrame owner) {
        super(owner, Language.translate("New Instance"));
        this.setModalityType(ModalityType.APPLICATION_MODAL);

        this.nameField = new JTextField();
        this.nameField.putClientProperty("JTextField.placeholderText", Language.translate("Instance Name"));

        this.groupCombo = new JComboBox<>(new Vector<>(XLauncher.getInstance().getInstanceManager().groupNames));
        this.groupCombo.setEditable(true);
        this.groupCombo.setSelectedItem("<default>");

        VersionTableModel model = new VersionTableModel();
        this.versionsTable = new JTable(model);
        this.versionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.versionsTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        this.versionsTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        this.versionsTable.getColumnModel().getColumn(2).setPreferredWidth(60);

        this.searchField = new JTextField();
        this.searchField.putClientProperty("JTextField.placeholderText", Language.translate("Search..."));

        this.javaFilterCheckbox = new JCheckBox("Java");
        this.executableFilterCheckbox = new JCheckBox(Language.translate("Executable"));

        TableRowSorter<VersionTableModel> sorter = new TableRowSorter<>(model);
        sorter.setRowFilter(new VersionsRowFilter(this.javaFilterCheckbox, this.executableFilterCheckbox, this.searchField));

        this.searchField.getDocument().addDocumentListener(new SimpleDocumentListener(sorter::sort));
        this.javaFilterCheckbox.addActionListener(e -> sorter.sort());
        this.executableFilterCheckbox.addActionListener(e -> sorter.sort());

        this.versionsTable.setRowSorter(sorter);

        this.resultField = new JTextField();
        this.resultField.setEnabled(false);

        this.createButton = new JButton(Language.translate("Create"));
        this.createButton.setEnabled(false);

        this.cancelButton = new JButton(Language.translate("Cancel"));
        this.cancelButton.addActionListener(e -> this.dispose());

        this.nameField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            String text = this.nameField.getText().trim();
            this.createButton.setEnabled(!text.isEmpty() && this.versionsTable.getSelectedRow() != -1);
        }));

        this.versionsTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            this.resultField.setText(Language.translate("Selected version") + ": " + model.getAt(this.versionsTable.getSelectedRow()));
            this.createButton.setEnabled(!this.nameField.getText().trim().isEmpty() && this.versionsTable.getSelectedRow() != -1);
        });

        this.createButton.addActionListener(e -> {
            this.dispose();
            LauncherGui.getInstance().startTask(owner, new InstanceCreateTask(this.nameField.getText(), String.valueOf(this.groupCombo.getSelectedItem()), (Version) model.getValueAt(this.versionsTable.getRowSorter().convertRowIndexToModel(this.versionsTable.getSelectedRow()), -1)));
        });

        this.versionsTable.getSelectionModel().setSelectionInterval(0, 0);

        this.add(this.buildLayout(), BorderLayout.CENTER);
        this.getContentPane().setPreferredSize(new Dimension(650, 400));
        this.setMinimumSize(this.getContentPane().getPreferredSize());
        SwingUtils.registerEscapeAction(this.getRootPane(), this::dispose);
        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }

    private JPanel buildLayout() {
        JPanel rootPanel = new JPanel(new GridBagLayout());
        rootPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel topPanel = new JPanel(new GridBagLayout());

        topPanel.add(new JLabel(Language.translate("Name")), new GridBagContrs().insetsH(0, 16));
        topPanel.add(this.nameField, new GridBagContrs().fill(true, false).weight(1, 0).pos(1, 0));

        topPanel.add(new JLabel(Language.translate("Group")), new GridBagContrs().insetsH(0, 16).pos(0, 1).insetsV(8, 0));
        topPanel.add(this.groupCombo, new GridBagContrs().fill(true, false).weight(1, 0).pos(1, 1).insetsV(8, 0));

        rootPanel.add(topPanel, new GridBagContrs().fill(true, false).weight(1, 0).insetsV(0, 8).size(2, 1));

        JScrollPane scrollPane = new JScrollPane(this.versionsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, UIManager.getColor("Component.borderColor")));

        rootPanel.add(scrollPane, new GridBagContrs().fill(true, true).weight(1, 1).pos(0, 1));

        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBorder(new CompoundBorder(BorderFactory.createTitledBorder(Language.translate("Filter")), BorderFactory.createEmptyBorder(6, 8, 8, 8)));

        filterPanel.add(this.javaFilterCheckbox, new GridBagContrs().anchor(GridBagContrs.NORTHWEST));
        filterPanel.add(this.executableFilterCheckbox, new GridBagContrs().pos(0, 1).insetsV(6, 0).anchor(GridBagContrs.NORTHWEST));
        filterPanel.add(new JPanel(), new GridBagContrs().fill(true, true).weight(1, 1).pos(0, 2));

        rootPanel.add(filterPanel, new GridBagContrs().pos(1, 1).weight(0, 1).insetsH(8, 0).fill(false, true));

        JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.add(this.searchField, new GridBagContrs().fill(true, false).weight(1, 0));

        rootPanel.add(bottomPanel, new GridBagContrs().fill(true, false).weight(1, 0).size(2, 1).pos(0, 2).insetsV(8, 8));

        JPanel footerBar = new JPanel(new GridBagLayout());
        footerBar.add(this.resultField, new GridBagContrs().fill(true, false).weight(1, 0).insetsH(0, 16));
        footerBar.add(this.createButton, new GridBagContrs().insetsH(0, 8));
        footerBar.add(this.cancelButton);
        rootPanel.add(footerBar, new GridBagContrs().fill(true, false).weight(1, 0).size(2, 1).pos(0, 3));

        return rootPanel;
    }

    private static class VersionTableModel extends AbstractTableModel {
        private final DateTimeFormatter releaseTimeFormatter;
        private final List<Version> versions;

        public VersionTableModel() {
            this.releaseTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            this.versions = XLauncher.getInstance().getVersionManager().versions;
        }

        public Version getAt(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= this.versions.size()) return null;
            return this.versions.get(rowIndex);
        }

        @Override
        public int getRowCount() {
            return this.versions.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0: return "Id";
                case 1: return Language.translate("Release Time");
                case 2: return Language.translate("Type");
                default: return "";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Version version = this.versions.get(rowIndex);
            switch (columnIndex) {
                case 0: return version.id;
                case 1: return this.releaseTimeFormatter.format(version.releaseTime);
                case 2: return Language.translate(version.type.name);
                case -1: return version;
                default: return "";
            }
        }
    }

    private static class VersionsRowFilter extends RowFilter<VersionTableModel, Integer> {
        private final JCheckBox javaCheckbox;
        private final JCheckBox executableCheckbox;
        private final JTextField searchField;

        public VersionsRowFilter(JCheckBox javaCheckbox, JCheckBox executableCheckbox, JTextField searchField) {
            this.javaCheckbox = javaCheckbox;
            this.executableCheckbox = executableCheckbox;
            this.searchField = searchField;
        }

        @Override
        public boolean include(Entry<? extends VersionTableModel, ? extends Integer> entry) {
            Version version = XLauncher.getInstance().getVersionManager().versions.get(entry.getIdentifier());
            if (this.javaCheckbox.isSelected() != this.executableCheckbox.isSelected()) {
                if (this.javaCheckbox.isSelected() && version.type != Version.Type.JAVA) return false;
                if (this.executableCheckbox.isSelected() && version.type != Version.Type.EXECUTABLE) return false;
            }
            return version.id.contains(this.searchField.getText());
        }
    }
}
