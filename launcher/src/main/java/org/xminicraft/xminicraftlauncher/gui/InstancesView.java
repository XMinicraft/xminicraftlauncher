package org.xminicraft.xminicraftlauncher.gui;

import org.xminicraft.xminicraftlauncher.Instance;
import org.xminicraft.xminicraftlauncher.Language;
import org.xminicraft.xminicraftlauncher.XLauncher;
import org.xminicraft.xminicraftlauncher.gui.util.GridBagContrs;
import org.xminicraft.xminicraftlauncher.gui.util.SimpleDocumentListener;
import org.xminicraft.xminicraftlauncher.gui.util.SvgIcon;
import org.xminicraft.xminicraftlauncher.gui.util.SwingUtils;
import org.xminicraft.xminicraftlauncher.util.InstanceDeleteTask;
import org.xminicraft.xminicraftlauncher.util.InstanceLaunchTask;
import org.xminicraft.xminicraftlauncher.util.OperatingSystem;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.Instant;

public class InstancesView extends JPanel {
    private final JFrame owner;
    private final JPanel contentPanel;

    private final JTextField searchField;
    private final JLabel groupLabel;
    private final JComboBox<String> groupCombo;
    private final JButton addButton;

    private final JLabel hoveredInfo;

    public InstancesView(JFrame owner) {
        super(new GridBagLayout());
        this.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        this.owner = owner;

        this.searchField = new JTextField();
        this.searchField.putClientProperty("JTextField.placeholderText", Language.translate("Search..."));

        this.groupLabel = new JLabel(Language.translate("Group"));
        this.groupCombo = new JComboBox<>(SwingUtils.createImmutComboBoxModel(XLauncher.getInstance().getInstanceManager().getGroupList()));

        this.addButton = new JButton(Language.translate("Add Instance"));
        this.addButton.addActionListener(e -> new NewInstanceDialog(owner));
        this.add(this.searchField, new GridBagContrs().fill(true, false).weight(1, 0).insetsH(0, 16));
        this.add(this.groupLabel, new GridBagContrs().pos(1, 0));
        this.add(this.groupCombo, new GridBagContrs().pos(2, 0).insetsH(8, 16));
        this.add(this.addButton, new GridBagContrs().pos(3, 0));
        this.groupCombo.setSelectedIndex(0);
        this.groupCombo.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.DESELECTED) {
                this.refresh((String) this.groupCombo.getSelectedItem());
            }
        });

        this.contentPanel = new JPanel();
        this.contentPanel.setLayout(new WrapLayout(WrapLayout.LEFT, 8, 8));
        this.contentPanel.setBorder(BorderFactory.createEmptyBorder());
        this.contentPanel.setBackground(UIManager.getColor("InstancesView.backgroundColor"));

        for (Instance instance : XLauncher.getInstance().getInstanceManager().getInstancesForGroup((String) this.groupCombo.getSelectedItem())) {
            this.contentPanel.add(new InstanceCardItem(instance), -1);
        }

        XLauncher.getInstance().getInstanceManager().loaded.connect(owner, () -> {
            this.groupCombo.setModel(SwingUtils.createImmutComboBoxModel(XLauncher.getInstance().getInstanceManager().getGroupList()));
            this.refresh((String) this.groupCombo.getSelectedItem());
        });

        this.searchField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            String text = this.searchField.getText().trim();
            for (int i = 0; i < this.contentPanel.getComponentCount(); ++i) {
                Component item = this.contentPanel.getComponent(i);
                item.setVisible(text.isEmpty());
            }
        }));

        JScrollPane scrollPane = new JScrollPane(this.contentPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.add(scrollPane, new GridBagContrs().fill(true, true).weight(1, 1).size(4, 1).pos(0, 1).insetsV(8, 0));

        JPanel statusBar = new JPanel(new GridBagLayout());
        statusBar.setBorder(new CompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        this.hoveredInfo = new JLabel("");
        statusBar.add(this.hoveredInfo);
        statusBar.add(new JPanel(), new GridBagContrs().pos(1, 0).fill(true, false).weight(1, 0));
        statusBar.add(new JLabel("Total playtime: 0m"), new GridBagContrs().pos(2, 0));
        this.add(statusBar, new GridBagContrs().fill(true, false).weight(1, 0).size(4, 1).pos(0, 2));
    }

    public void updateTranslations() {
        this.searchField.putClientProperty("JTextField.placeholderText", Language.translate("Search..."));
        this.groupLabel.setText(Language.translate("Group"));
        this.addButton.setText(Language.translate("Add Instance"));
    }

    private void refresh(String groupName) {
        this.contentPanel.removeAll();
        for (Instance instance : XLauncher.getInstance().getInstanceManager().getInstancesForGroup(groupName)) {
            this.contentPanel.add(new InstanceCardItem(instance), -1);
        }
        this.contentPanel.setBackground(UIManager.getColor("InstancesView.backgroundColor"));
        this.updateUI();
    }

    public class InstanceCardItem extends JPanel {
        private final Instance instance;

        private boolean mouseOver;
        private boolean mousePressed;

        private Color defaultBackgroundColor;
        private Color hoverBackgroundColor;
        private Color pressedBackgroundColor;

        public InstanceCardItem(Instance instance) {
            super(new BorderLayout(), true);

            this.instance = instance;

            JLabel icon = new JLabel();
            try {
                icon.setIcon(new ImageIcon(ImageIO.read(LauncherGui.class.getResourceAsStream("/icon.png")).getScaledInstance(64, 64, 0)));
            } catch (IOException e) {
                e.printStackTrace();
            }
            icon.setPreferredSize(new Dimension(64, 64));
            icon.setHorizontalAlignment(JLabel.CENTER);
            this.add(icon, BorderLayout.CENTER);

            JLabel textLabel = new JLabel(instance.name);
            textLabel.setHorizontalAlignment(JLabel.CENTER);
            this.add(textLabel, BorderLayout.SOUTH);

            this.setPreferredSize(new Dimension(110, 110));

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    InstanceCardItem.this.mouseOver = true;
                    InstanceCardItem.this.repaint();
                    InstancesView.this.hoveredInfo.setText(instance.name + " - " + instance.version + ", total played for " + Instant.ofEpochMilli(instance.totalTimePlayed).getEpochSecond() + "s");
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    InstanceCardItem.this.mouseOver = false;
                    InstanceCardItem.this.repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON3) return;
                    InstanceCardItem.this.mousePressed = true;
                    InstanceCardItem.this.repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    InstanceCardItem.this.mousePressed = false;
                    InstanceCardItem.this.repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        InstanceCardItem.this.launch();
                    }
                }
            });

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem launchMenu = new JMenuItem(Language.translate("Launch"), SvgIcon.get("icons/play"));
                        launchMenu.setEnabled(!instance.isRunning.get());

                        JMenuItem killMenu = new JMenuItem(Language.translate("Kill"), SvgIcon.get("icons/kill"));
                        killMenu.addActionListener(ev -> {
                            if (instance.runningProcess.isAlive()) {
                                instance.runningProcess.destroyForcibly();
                            }
                        });

                        menu.add(launchMenu).addActionListener(ev -> InstanceCardItem.this.launch());
                        menu.add(killMenu).setEnabled(instance.isRunning.get());
                        menu.addSeparator();
                        menu.add(new JMenuItem(Language.translate("Edit"), SvgIcon.get("icons/edit"))).addActionListener(ev -> {
                            new InstanceDialog(InstancesView.this.owner, instance, () -> InstancesView.this.refresh((String) InstancesView.this.groupCombo.getSelectedItem()));
                        });

                        JMenuItem changeGroupMenu = new JMenuItem(Language.translate("Change Group"), SvgIcon.get("icons/group"));
                        menu.add(changeGroupMenu);
                        changeGroupMenu.addActionListener(ev -> {
                            new ChangeGroupDialog(InstancesView.this.owner, instance, () -> {
                                InstancesView.this.groupCombo.setModel(SwingUtils.createImmutComboBoxModel(XLauncher.getInstance().getInstanceManager().getGroupList()));
                                InstancesView.this.groupCombo.setSelectedIndex(0);
                                InstancesView.this.refresh((String) InstancesView.this.groupCombo.getSelectedItem());
                            });
                        });

                        JMenuItem deleteMenu = new JMenuItem(Language.translate("Delete"), SvgIcon.get("icons/bin"));
                        deleteMenu.setEnabled(!instance.isRunning.get());
                        deleteMenu.addActionListener(ev -> {
                            if (JOptionPane.showConfirmDialog(InstancesView.this.owner, "Are you sure you want to delete this instance?", "Delete",
                                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                                LauncherGui.getInstance().startTask(owner, new InstanceDeleteTask(instance, () -> {
                                    refresh((String) groupCombo.getSelectedItem());
                                }));
                            }
                        });

                        menu.add(deleteMenu);
                        menu.addSeparator();

                        JMenuItem openMenu = new JMenuItem(Language.translate("Open Folder"), SvgIcon.get("icons/open"));
                        openMenu.addActionListener(ev -> OperatingSystem.get().open(instance.path));

                        menu.add(openMenu);
                        menu.show(InstanceCardItem.this, e.getX(), e.getY());

                        instance.isRunning.signal.connect(menu, (value) -> {
                            launchMenu.setEnabled(!value);
                            killMenu.setEnabled(value);
                            deleteMenu.setEnabled(!value);
                        });
                    }
                }
            });

            this.setOpaque(false);
            this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            this.setToolTipText(Language.translate("Name") + ": " + instance.name + "\n" + Language.translate("Version") + ": " + instance.version);
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            this.updateColors();
        }

        private void launch() {
            if (this.instance.isRunning.get())
                return;

            SwingUtilities.invokeLater(() -> LauncherGui.getInstance().startTask(null, new InstanceLaunchTask(this.instance)));
        }

        @Override
        public void updateUI() {
            super.updateUI();
            this.updateColors();
        }

        private void updateColors() {
            this.defaultBackgroundColor = UIManager.getColor("InstanceItemCard.defaultBackgroundColor");
            this.hoverBackgroundColor = UIManager.getColor("InstanceItemCard.hoverBackgroundColor");
            this.pressedBackgroundColor = UIManager.getColor("InstanceItemCard.pressedBackgroundColor");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = ((Graphics2D) g);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(this.mousePressed ? this.pressedBackgroundColor : this.mouseOver ? this.hoverBackgroundColor : this.defaultBackgroundColor);
            g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 10, 10);

            super.paintComponent(g);
        }
    }
}
