package org.openstreetmap.pluging;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import static org.openstreetmap.josm.gui.mappaint.mapcss.ExpressionFactory.Functions.tr;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;


public class autoUploadDialog extends ToggleDialog implements ActionListener {

    JPanel jpCheckBox = new JPanel();
    private final SideButton sbSkip;

    private final JCheckBox jchb1, jchb5, jchb10;

    public autoUploadDialog() {
        super("Second", "up", tr("Open Second."), Shortcut.registerShortcut("tool:Second", tr("Toggle: {0}", tr("Second")),
                KeyEvent.VK_F, Shortcut.CTRL_SHIFT), 75);

        sbSkip = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Active"));
                putValue(SMALL_ICON, ImageProvider.get("mapmode", "skip.png"));
                putValue(SHORT_DESCRIPTION, tr("Skip Error"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showConfirmDialog(null, "This upload the layers in back");
            }
        });

        jchb1 = new JCheckBox(new AbstractAction() {
            {
                putValue(NAME, tr("1 min."));
                putValue(SHORT_DESCRIPTION, tr("Upload editions every 1 minutes."));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (jchb1.isSelected()) {
                    new UploadAction().uploadData(Main.main.getEditLayer(), new APIDataSet(Main.main.getCurrentDataSet()));
                                                                           
                }
            }
        });

        jchb5 = new JCheckBox(new AbstractAction() {

            {
                putValue(NAME, tr("5 min."));
                putValue(SHORT_DESCRIPTION, tr("Upload editions every 5 minutes."));

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (jchb5.isSelected()) {
                }
            }
        });

        jchb10 = new JCheckBox(new AbstractAction() {
            {
                putValue(NAME, tr("10 min."));
                putValue(SHORT_DESCRIPTION, tr("Upload editions every 10 minutes."));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (jchb10.isSelected()) {

                }
            }

        });

        this.setPreferredSize(new Dimension(0, 40));
        jpCheckBox.add(jchb1);

        jpCheckBox.add(jchb5);

        jpCheckBox.add(jchb10);

        createLayout(jpCheckBox, false, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JOptionPane.showConfirmDialog(null, "Auto Uploading Plugin in construction.");
    }
}
