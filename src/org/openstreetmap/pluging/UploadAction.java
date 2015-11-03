
package org.openstreetmap.pluging;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.upload.ApiPreconditionCheckerHook;
import org.openstreetmap.josm.actions.upload.DiscardTagsHook;
import org.openstreetmap.josm.actions.upload.FixDataHook;
import org.openstreetmap.josm.actions.upload.RelationUploadOrderHook;
import org.openstreetmap.josm.actions.upload.UploadHook;
import org.openstreetmap.josm.actions.upload.ValidateUploadHook;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.io.UploadDialog;
import org.openstreetmap.josm.gui.io.UploadPrimitivesTask;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

public class UploadAction extends JosmAction{
    
    private static final List<UploadHook> uploadHooks = new LinkedList<UploadHook>();
    private static final List<UploadHook> lateUploadHooks = new LinkedList<UploadHook>();
    static {
        
        uploadHooks.add(new ValidateUploadHook());
        uploadHooks.add(new FixDataHook());
        uploadHooks.add(new ApiPreconditionCheckerHook());        
        uploadHooks.add(new RelationUploadOrderHook());
        lateUploadHooks.add(new DiscardTagsHook());
    }

    
    public static void registerUploadHook(UploadHook hook) {
        registerUploadHook(hook, false);
    }

    public static void registerUploadHook(UploadHook hook, boolean late) {
        if(hook == null) return;
        if (late) {
            if (!lateUploadHooks.contains(hook)) {
                lateUploadHooks.add(0, hook);
            }
        } else {
            if (!uploadHooks.contains(hook)) {
                uploadHooks.add(0, hook);
            }
        }
    }

    
    public static void unregisterUploadHook(UploadHook hook) {
        if(hook == null) return;
        if (uploadHooks.contains(hook)) {
            uploadHooks.remove(hook);
        }
        if (lateUploadHooks.contains(hook)) {
            lateUploadHooks.remove(hook);
        }
    }

    public UploadAction() {
        super(tr("Upload data"), "upload", tr("Upload all changes in the active data layer to the OSM server"),
                Shortcut.registerShortcut("file:upload", tr("File: {0}", tr("Upload data")), KeyEvent.VK_UP, Shortcut.CTRL_SHIFT), true);
        putValue("help", ht("/Action/Upload"));
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getEditLayer() != null);
    }

    public boolean checkPreUploadConditions(OsmDataLayer layer) {
        return checkPreUploadConditions(layer, new APIDataSet(layer.data));
    }

    protected static void alertUnresolvedConflicts(OsmDataLayer layer) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                tr("<html>The data to be uploaded participates in unresolved conflicts of layer ''{0}''.<br>"
                        + "You have to resolve them first.</html>", layer.getName()
                ),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE,
                HelpUtil.ht("/Action/Upload#PrimitivesParticipateInConflicts")
        );
    }

    /**
     * returns true if the user wants to cancel, false if they
     * want to continue
     */
    public static boolean warnUploadDiscouraged(OsmDataLayer layer) {
        return GuiHelper.warnUser(tr("Upload discouraged"),
                "<html>" +
                tr("You are about to upload data from the layer ''{0}''.<br /><br />"+
                    "Sending data from this layer is <b>strongly discouraged</b>. If you continue,<br />"+
                    "it may require you subsequently have to revert your changes, or force other contributors to.<br /><br />"+
                    "Are you sure you want to continue?", layer.getName())+
                "</html>",
                ImageProvider.get("upload"), tr("Ignore this hint and upload anyway"));
    }

    
    public boolean checkPreUploadConditions(OsmDataLayer layer, APIDataSet apiData) {
        if (layer.isUploadDiscouraged()) {
            if (warnUploadDiscouraged(layer)) {
                return false;
            }
        }
        ConflictCollection conflicts = layer.getConflicts();
        if (apiData.participatesInConflict(conflicts)) {
            alertUnresolvedConflicts(layer);
            return false;
        }
       
        for (UploadHook hook : uploadHooks) {
            if (!hook.checkUpload(apiData))
                return false;
        }

        return true;
    }

   
    public void uploadData(final OsmDataLayer layer, APIDataSet apiData) {
        if (apiData.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("No changes to upload."),
                    tr("Warning"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        if (!checkPreUploadConditions(layer, apiData))
            return;

        final UploadDialog dialog = UploadDialog.getUploadDialog();
       
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dialog.setDefaultChangesetTags(layer.data.getChangeSetTags());
            }
        });
        dialog.setUploadedPrimitives(apiData);
        dialog.setVisible(true);
        if (dialog.isCanceled())
            return;
        dialog.rememberUserInput();

        for (UploadHook hook : lateUploadHooks) {
            if (!hook.checkUpload(apiData))
                return;
        }

        Main.worker.execute(
                new UploadPrimitivesTask(
                        UploadDialog.getUploadDialog().getUploadStrategySpecification(),
                        layer,
                        apiData,
                        UploadDialog.getUploadDialog().getChangeset()
                )
        );
        
        PleaseWaitProgressMonitor monitor=new PleaseWaitProgressMonitor();
       
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        if (Main.map == null) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Nothing to upload. Get some data first."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        APIDataSet apiData = new APIDataSet(Main.main.getCurrentDataSet());
        uploadData(Main.main.getEditLayer(), apiData);
    }
}
