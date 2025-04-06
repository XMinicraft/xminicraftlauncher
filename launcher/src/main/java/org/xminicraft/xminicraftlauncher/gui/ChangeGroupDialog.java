package org.xminicraft.xminicraftlauncher.gui;

import org.xminicraft.xminicraftlauncher.Instance;
import org.xminicraft.xminicraftlauncher.Language;
import org.xminicraft.xminicraftlauncher.XLauncher;
import org.xminicraft.xminicraftlauncher.gui.util.GridBagContrs;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class ChangeGroupDialog extends JDialog {
    private final JComboBox<String> groupField;
    private final JButton okButton;
    private final JButton cancelButton;

    public ChangeGroupDialog(JFrame owner, Instance instance, Runnable onSuccess) {
        super(owner, Language.translate("Change Group"));
        this.setModalityType(ModalityType.APPLICATION_MODAL);

        Set<String> groupNames = XLauncher.getInstance().getInstanceManager().groupNames;

        this.groupField = new JComboBox<>(new Vector<>(groupNames));
        this.groupField.setEditable(true);
        this.groupField.setSelectedItem(instance.groupName);

        this.okButton = new JButton("OK");
        this.okButton.addActionListener(e -> {
            instance.groupName = String.valueOf(this.groupField.getSelectedItem());
            groupNames.add(instance.groupName);
            this.dispose();
            onSuccess.run();
        });

        this.cancelButton = new JButton(Language.translate("Cancel"));
        this.cancelButton.addActionListener(e -> this.dispose());

        this.add(this.buildLayout(), BorderLayout.CENTER);
        Dimension dim = this.getContentPane().getPreferredSize();
        dim.width = 300;
        this.getContentPane().setPreferredSize(dim);
        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }

    private JPanel buildLayout() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        root.add(new JLabel(Language.translate("Group")));
        root.add(this.groupField, new GridBagContrs().fillH().pos(1, 0).insetsH(8, 0));

        JPanel bottom = new JPanel(new GridBagLayout());

        bottom.add(new JPanel(), new GridBagContrs().fillH());
        bottom.add(this.okButton, new GridBagContrs().insetsH(0, 8).pos(1, 0));
        bottom.add(this.cancelButton, new GridBagContrs().pos(2, 0));

        root.add(bottom, new GridBagContrs().insetsV(8, 0).pos(0, 1).size(2, 1).fillH());

        return root;
    }
}
