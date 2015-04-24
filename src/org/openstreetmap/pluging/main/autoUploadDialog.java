
package org.openstreetmap.pluging.main;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import static org.openstreetmap.josm.gui.mappaint.mapcss.ExpressionFactory.Functions.tr;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 *
 * @author samely
 */
public class autoUploadDialog extends ToggleDialog implements ActionListener{
    JPanel jpValue= new JPanel();
    private final SideButton sbSkip;

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
         
        this.setPreferredSize(new Dimension(0, 40));
        createLayout(jpValue, false, Arrays.asList(new SideButton[]{
           sbSkip
        }));
    }
    
    
    @Override
    public void actionPerformed(ActionEvent e) {
       JOptionPane.showConfirmDialog(null, "Auto Uploading Plugin in construction.");
    }
    
    
}
