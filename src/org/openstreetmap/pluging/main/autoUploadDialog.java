package org.openstreetmap.pluging.main;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;

import static org.openstreetmap.josm.gui.mappaint.mapcss.ExpressionFactory.Functions.tr;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.pluging.dao.BasicUploadSettingsPanel;
import org.openstreetmap.pluging.dao.UploadDialog;
import org.openstreetmap.pluging.dao.UploadPrimitivesTask;

import org.openstreetmap.pluging.dao.autoUploadAction;

/**
 *
 * @author samely
 */
public class autoUploadDialog extends ToggleDialog implements ActionListener {

    JPanel jpComment = new JPanel();
    JPanel jpSource = new JPanel();
    JPanel jpCheckBox = new JPanel();
    private final SideButton sbSkip;
    private final JTextField txfComment;
    private final JTextField txfSource;
    private final JCheckBox jchb1, jchb5, jchb10;
    private autoUploadAction aua;
    private BasicUploadSettingsPanel busp = new BasicUploadSettingsPanel();

    public String getComment() {
        return txfComment.getText();
    }

    public String getSource() {
        return txfSource.getText();
    }
   
    public autoUploadDialog() {
        super("Auto Uploading", "up", tr("Open Auto-Uploading."), Shortcut.registerShortcut("tool:autoUpload", tr("Toggle: {0}", tr("Auto Uploading")),
                KeyEvent.VK_F, Shortcut.CTRL_SHIFT), 75);

        sbSkip = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Active"));
                putValue(SMALL_ICON, ImageProvider.get("mapmode", "skip.png"));
                putValue(SHORT_DESCRIPTION, tr("Skip Error"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showConfirmDialog(null, "This upload the layers");
            }
        });

        txfComment = new JTextField();
        txfComment.setText("Here's the comment of the uploading");

        txfSource = new JTextField();
        txfSource.setText("Here's the source of the uploading");
       
        jchb1 = new JCheckBox(new AbstractAction() {
            {
                putValue(NAME, tr("1 min."));
                putValue(SHORT_DESCRIPTION, tr("Upload editions every 1 minutes."));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (jchb1.isSelected()) {
                    aua = new autoUploadAction();
                    aua.actionPerformed(e);
                }

            }
        });

        jchb5 = new JCheckBox(new AbstractAction() {

            {
                putValue(NAME, tr("5 min."));
                putValue(SHORT_DESCRIPTION, tr("Upload editions every 5 minutes."));
               
            }
            private Set<IPrimitive> processedPrimitives = new HashSet<IPrimitive>();

            @Override
            public void actionPerformed(ActionEvent e) {
                if (jchb5.isSelected()) {
                     busp.enviar(getComment(), getSource());
                    APIDataSet apiData = new APIDataSet(Main.main.getCurrentDataSet());
                    
                    System.out.println("Esto son los datos en autouploaddialog:" + getComment() + getSource());
                   
                    Main.worker.execute(
                            new UploadPrimitivesTask(
                                    UploadDialog.getUploadDialog(getComment(), getSource()).getUploadStrategySpecification(),
                                    Main.main.getEditLayer(),
                                    apiData,
                                    UploadDialog.getUploadDialog(getComment(), getSource()).getChangeset()
                            )
                    );

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
                aua = new autoUploadAction();
                aua.actionPerformed(e);

            }

        });

        this.setPreferredSize(
                new Dimension(0, 40));
        jpSource.add(txfSource);
        jpComment.add(txfComment);
        jpComment.add(jpSource);

        jpComment.add(jpCheckBox);

        jpCheckBox.add(jchb1);

        jpCheckBox.add(jchb5);

        jpCheckBox.add(jchb10);

        createLayout(jpComment, false, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JOptionPane.showConfirmDialog(null, "Auto Uploading Plugin in construction.");
    }

}
