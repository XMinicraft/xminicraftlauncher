package org.xminicraft.xminicraftlauncher.gui;

import org.xminicraft.xminicraftlauncher.Language;
import org.xminicraft.xminicraftlauncher.XLauncher;
import org.xminicraft.xminicraftlauncher.gui.util.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AboutDialog extends JDialog {
    public AboutDialog(Window owner) {
        super(owner, Language.translate("About"));
        this.setModalityType(ModalityType.APPLICATION_MODAL);

        JPanel rootPanel = new JPanel();

        XLauncher.Build build = XLauncher.getInstance().getBuild();

        JTextPane aboutPane = new JTextPane();
        aboutPane.setContentType("text/html");
        aboutPane.setEditable(false);
        aboutPane.setText("<html><p>XMinicraft Launcher " + build.version + "</p>Build time: " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(build.buildTime) + "<br/><a href=\"#\">Github</a></html>");

        rootPanel.add(aboutPane);

        this.getContentPane().add(rootPanel, BorderLayout.CENTER);
        SwingUtils.registerEscapeAction(this.getRootPane(), this::dispose);
        this.setResizable(false);
        this.getContentPane().setPreferredSize(new Dimension(180, 80));
        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }
}
