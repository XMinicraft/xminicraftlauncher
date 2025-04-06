package org.xminicraft.xminicraftlauncher.gui;

import org.xminicraft.xminicraftlauncher.Language;
import org.xminicraft.xminicraftlauncher.java.JavaRuntime;
import org.xminicraft.xminicraftlauncher.XLauncher;
import org.xminicraft.xminicraftlauncher.gui.util.GridBagContrs;
import org.xminicraft.xminicraftlauncher.gui.util.SwingUtils;
import org.xminicraft.xminicraftlauncher.java.JavaRuntimeSource;
import org.xminicraft.xminicraftlauncher.util.DownloadJavaTask;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JavaDownloadDialog extends JDialog {
    private final JList<JavaRuntimeSource> sourcesList;
    private final JList<String> majorVersionsList;
    private final JTable table;
    private final JavaRuntimeTableModel tableModel;

    private final JButton downloadButton;
    private final JButton cancelButton;

    public JavaDownloadDialog(JDialog owner, Runnable onFinished) {
        super(owner, Language.translate("Download Java"));
        this.setModalityType(ModalityType.APPLICATION_MODAL);

        this.sourcesList = new JList<>(SwingUtils.createImmutListModel(new ArrayList<>(XLauncher.getInstance().getJavaRuntimeManager().sources.values())));
        this.sourcesList.setSelectedIndex(0);
        this.sourcesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.majorVersionsList = new JList<>();
        this.majorVersionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.tableModel = new JavaRuntimeTableModel();
        this.table = new JTable(this.tableModel);
        this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.sourcesList.addListSelectionListener(e -> JavaDownloadDialog.this.loadVersions());
        this.majorVersionsList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            JavaDownloadDialog.this.loadRuntimes();
        });
        this.loadVersions();

        this.downloadButton = new JButton(Language.translate("Download"));
        this.downloadButton.addActionListener(e -> {
            LauncherGui.getInstance().startTask(this, new DownloadJavaTask(this.tableModel.getRuntime(this.table.getSelectedRow()), () -> {
                this.dispose();
                onFinished.run();
            }));
        });

        this.cancelButton = new JButton(Language.translate("Cancel"));
        this.cancelButton.addActionListener(e -> this.dispose());

        this.add(this.buildLayout());
        SwingUtils.registerEscapeAction(this.getRootPane(), this::dispose);
        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }

    private JPanel buildLayout() {
        JPanel rootPanel = new JPanel(new GridBagLayout());
        JScrollPane sourcesSP = new JScrollPane(this.sourcesList);
        JScrollPane majorVersionsSP = new JScrollPane(this.majorVersionsList);

        sourcesSP.setMinimumSize(new Dimension(90, 0));
        sourcesSP.setPreferredSize(new Dimension(90, 0));
        majorVersionsSP.setMinimumSize(new Dimension(70, 0));
        majorVersionsSP.setPreferredSize(new Dimension(70, 0));
        JScrollPane tableSP = new JScrollPane(this.table);

        rootPanel.add(sourcesSP, new GridBagContrs().fill(false, true).weight(0, 1).insetsH(0, 10));
        rootPanel.add(majorVersionsSP, new GridBagContrs().fill(false, true).weight(0, 1).insetsH(0, 10));
        rootPanel.add(tableSP, new GridBagContrs().fill(true, true).weight(1, 1));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel footerP = new JPanel(new GridBagLayout());

        footerP.add(new JPanel(), new GridBagContrs().fill(true, false).weight(1, 0));
        footerP.add(this.downloadButton);
        footerP.add(this.cancelButton, new GridBagContrs().insetsH(10, 0));

        rootPanel.add(footerP, new GridBagContrs().fill(true, false).weight(1, 0).size(3, 1).pos(0, 1).insetsV(10, 0));
        return rootPanel;
    }

    private void loadVersions() {
        JavaRuntimeSource source = this.sourcesList.getSelectedValue();
        this.majorVersionsList.setModel(SwingUtils.createImmutListModel(source.availableReleases.stream().map(it -> "Java " + it).collect(Collectors.toList())));
        this.majorVersionsList.setSelectedIndex(0);
    }

    private void loadRuntimes() {
        JavaRuntimeSource source = this.sourcesList.getSelectedValue();
        if (this.majorVersionsList.getSelectedIndex() < 0) {
            return;
        }
        int version = Integer.parseInt(source.availableReleases.get(this.majorVersionsList.getSelectedIndex()));
        this.tableModel.setRuntimes(source.runtimes.get(version));
        this.table.getSelectionModel().setSelectionInterval(0, 0);
    }

    static class JavaRuntimeTableModel extends AbstractTableModel {
        private final DateTimeFormatter releaseTimeFormatter;
        private List<JavaRuntime> runtimes;

        public JavaRuntimeTableModel() {
            this.releaseTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            this.runtimes = Collections.emptyList();
        }

        public void setRuntimes(List<JavaRuntime> runtimes) {
            this.runtimes = runtimes;
            this.fireTableDataChanged();
        }

        public JavaRuntime getRuntime(int rowIndex) {
            return this.runtimes.get(rowIndex);
        }

        @Override
        public int getRowCount() {
            return this.runtimes.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0: return Language.translate("Version");
                case 1: return Language.translate("Architecture");
                case 2: return Language.translate("Name");
                case 3: return Language.translate("Release Time");
                case 4: return Language.translate("Type");
                default: return "";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= this.getRowCount() || columnIndex < 0 || columnIndex >= this.getColumnCount()) {
                return null;
            }
            JavaRuntime runtime = this.runtimes.get(rowIndex);
            if (columnIndex == 0) {
                return runtime.version;
            } else if (columnIndex == 1) {
                String[] s = runtime.runtimeOs.split("-");
                if (s.length < 2) return "?";
                return s[1];
            } else if (columnIndex == 2) {
                return runtime.name;
            } else if (columnIndex == 3) {
                if (runtime.releaseTime == null) return "";
                return this.releaseTimeFormatter.format(runtime.releaseTime);
            } else if (columnIndex == 4) {
                return "jre";
            }
            return null;
        }
    }
}
