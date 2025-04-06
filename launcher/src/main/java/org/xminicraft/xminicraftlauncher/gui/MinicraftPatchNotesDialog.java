package org.xminicraft.xminicraftlauncher.gui;

import org.xminicraft.xminicraftlauncher.version.Version;
import org.xminicraft.xminicraftlauncher.XLauncher;
import org.xminicraft.xminicraftlauncher.gui.util.GridBagContrs;
import org.xminicraft.xminicraftlauncher.gui.util.SwingUtils;

import javax.swing.*;
import java.awt.*;

public class MinicraftPatchNotesDialog extends JDialog {
    public MinicraftPatchNotesDialog(JFrame owner) {
        super(owner, "Patch Notes");

        JPanel rootPanel = new JPanel(new GridBagLayout());
        rootPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JList<Version> versionsList = new JList<>(SwingUtils.createImmutListModel(XLauncher.getInstance().getVersionManager().versions));
        JScrollPane versionsScrollPane = new JScrollPane(versionsList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JTextArea patchNote = new JTextArea();
        JScrollPane patchNoteScrollPane = new JScrollPane(patchNote, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        rootPanel.add(versionsScrollPane, new GridBagContrs().fill(false, true).weight(0, 1));
        rootPanel.add(patchNoteScrollPane, new GridBagContrs().fill(true, true).weight(1, 1).pos(1, 0));

        JPanel footerPanel = new JPanel(new GridBagLayout());
        footerPanel.add(new JPanel(), new GridBagContrs().fill(true, false).weight(1, 0));
        footerPanel.add(new JButton("Close"), new GridBagContrs().pos(1, 0));
        rootPanel.add(footerPanel, new GridBagContrs().fill(true, false).weight(1, 0).size(2, 1).pos(0, 1).insetsV(8, 0));

        this.add(rootPanel, BorderLayout.CENTER);

        this.getContentPane().setPreferredSize(new Dimension(600, 500));
        SwingUtils.registerEscapeAction(this.getRootPane(), this::dispose);

        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }
}
