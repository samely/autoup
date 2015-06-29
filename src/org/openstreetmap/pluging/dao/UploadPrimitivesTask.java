// License: GPL. For details, see LICENSE file.
package org.openstreetmap.pluging.dao;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.CheckParameterUtil.ensureParameterNotNull;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.io.AbstractUploadTask;
import org.openstreetmap.josm.gui.io.MaxChangesetSizeExceededPolicy;
import org.openstreetmap.josm.gui.io.UploadDialog;
import org.openstreetmap.josm.gui.io.UploadStrategySpecification;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.ChangesetClosedException;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiPrimitiveGoneException;
import org.openstreetmap.josm.io.OsmServerWriter;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.xml.sax.SAXException;

/**
 * The task for uploading a collection of primitives
 *
 */
public class UploadPrimitivesTask extends AbstractUploadTask {

    private boolean uploadCanceled = false;
    private Exception lastException = null;
    private APIDataSet toUpload;
    private OsmServerWriter writer;
    private OsmDataLayer layer;
    private Changeset changeset;
    private Set<IPrimitive> processedPrimitives;
    private UploadStrategySpecification strategy;

       
    public UploadPrimitivesTask(UploadStrategySpecification strategy, OsmDataLayer layer, APIDataSet toUpload, Changeset changeset) {
        super(tr("Uploading data for layer ''{0}''", layer.getName()), false /* don't ignore exceptions */);
        ensureParameterNotNull(layer, "layer");
        ensureParameterNotNull(strategy, "strategy");
        ensureParameterNotNull(changeset, "changeset");
        this.toUpload = toUpload;
        this.layer = layer;
        this.changeset = changeset;
        this.strategy = strategy;
        this.processedPrimitives = new HashSet<IPrimitive>();
       
    }

    protected MaxChangesetSizeExceededPolicy askMaxChangesetSizeExceedsPolicy() {
        ButtonSpec[] specs = new ButtonSpec[]{
            new ButtonSpec(
            tr("Continue uploading"),
            ImageProvider.get("upload"),
            tr("Click to continue uploading to additional new changesets"),
            null /* no specific help text */
            ),
            new ButtonSpec(
            tr("Go back to Upload Dialog"),
            ImageProvider.get("dialogs", "uploadproperties"),
            tr("Click to return to the Upload Dialog"),
            null /* no specific help text */
            ),
            new ButtonSpec(
            tr("Abort"),
            ImageProvider.get("cancel"),
            tr("Click to abort uploading"),
            null /* no specific help text */
            )
        };
        int numObjectsToUploadLeft = toUpload.getSize() - processedPrimitives.size();
        String msg1 = tr("The server reported that the current changeset was closed.<br>"
                + "This is most likely because the changesets size exceeded the max. size<br>"
                + "of {0} objects on the server ''{1}''.",
                OsmApi.getOsmApi().getCapabilities().getMaxChangesetSize(),
                OsmApi.getOsmApi().getBaseUrl()
        );
        String msg2 = trn(
                "There is {0} object left to upload.",
                "There are {0} objects left to upload.",
                numObjectsToUploadLeft,
                numObjectsToUploadLeft
        );
        String msg3 = tr(
                "Click ''<strong>{0}</strong>'' to continue uploading to additional new changesets.<br>"
                + "Click ''<strong>{1}</strong>'' to return to the upload dialog.<br>"
                + "Click ''<strong>{2}</strong>'' to abort uploading and return to map editing.<br>",
                specs[0].text,
                specs[1].text,
                specs[2].text
        );
        String msg = "<html>" + msg1 + "<br><br>" + msg2 + "<br><br>" + msg3 + "</html>";
        int ret = HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                msg,
                tr("Changeset is full"),
                JOptionPane.WARNING_MESSAGE,
                null, /* no special icon */
                specs,
                specs[0],
                ht("/Action/Upload#ChangesetFull")
        );
        switch (ret) {
            case 0:
                return MaxChangesetSizeExceededPolicy.AUTOMATICALLY_OPEN_NEW_CHANGESETS;
            case 1:
                return MaxChangesetSizeExceededPolicy.FILL_ONE_CHANGESET_AND_RETURN_TO_UPLOAD_DIALOG;
            case 2:
                return MaxChangesetSizeExceededPolicy.ABORT;
            case JOptionPane.CLOSED_OPTION:
                return MaxChangesetSizeExceededPolicy.ABORT;
        }
        // should not happen
        return null;
    }

    protected void openNewChangeset() {
        // make sure the current changeset is removed from the upload dialog.
        //
        ChangesetCache.getInstance().update(changeset);
        Changeset newChangeSet = new Changeset();
        newChangeSet.setKeys(this.changeset.getKeys());
        this.changeset = newChangeSet;
    }

    protected boolean recoverFromChangesetFullException() {
        if (toUpload.getSize() - processedPrimitives.size() == 0) {
            strategy.setPolicy(MaxChangesetSizeExceededPolicy.ABORT);
            return false;
        }
        if (strategy.getPolicy() == null || strategy.getPolicy().equals(MaxChangesetSizeExceededPolicy.ABORT)) {
            MaxChangesetSizeExceededPolicy policy = askMaxChangesetSizeExceedsPolicy();
            strategy.setPolicy(policy);
        }
        switch (strategy.getPolicy()) {
            case ABORT:
            // don't continue - finish() will send the user back to map editing
                //
                return false;
            case FILL_ONE_CHANGESET_AND_RETURN_TO_UPLOAD_DIALOG:
            // don't continue - finish() will send the user back to the upload dialog
                //
                return false;
            case AUTOMATICALLY_OPEN_NEW_CHANGESETS:
            // prepare the state of the task for a next iteration in uploading.
                //
                openNewChangeset();
                toUpload.removeProcessed(processedPrimitives);
                return true;
        }
        // should not happen
        return false;
    }

    /**
     * Retries to recover the upload operation from an exception which was
     * thrown because an uploaded primitive was already deleted on the server.
     *
     * @param e the exception throw by the API
     * @param monitor a progress monitor
     * @throws OsmTransferException thrown if we can't recover from the
     * exception
     */
    protected void recoverFromGoneOnServer(OsmApiPrimitiveGoneException e, ProgressMonitor monitor) throws OsmTransferException {
        if (!e.isKnownPrimitive()) {
            throw e;
        }
        OsmPrimitive p = layer.data.getPrimitiveById(e.getPrimitiveId(), e.getPrimitiveType());
        if (p == null) {
            throw e;
        }
        if (p.isDeleted()) {
            // we tried to delete an already deleted primitive.
            final String msg;
            final String displayName = p.getDisplayName(DefaultNameFormatter.getInstance());
            if (p instanceof Node) {
                msg = tr("Node ''{0}'' is already deleted. Skipping object in upload.", displayName);
            } else if (p instanceof Way) {
                msg = tr("Way ''{0}'' is already deleted. Skipping object in upload.", displayName);
            } else if (p instanceof Relation) {
                msg = tr("Relation ''{0}'' is already deleted. Skipping object in upload.", displayName);
            } else {
                msg = tr("Object ''{0}'' is already deleted. Skipping object in upload.", displayName);
            }
            monitor.appendLogMessage(msg);
            Main.warn(msg);
            processedPrimitives.addAll(writer.getProcessedPrimitives());
            processedPrimitives.add(p);
            toUpload.removeProcessed(processedPrimitives);
            return;
        }
        // exception was thrown because we tried to *update* an already deleted
        // primitive. We can't resolve this automatically. Re-throw exception,
        // a conflict is going to be created later.
        throw e;
    }

    protected void cleanupAfterUpload() {
        // we always clean up the data, even in case of errors. It's possible the data was
        // partially uploaded. Better run on EDT.
        //
        Runnable r = new Runnable() {
            @Override
            public void run() {
                layer.cleanupAfterUpload(processedPrimitives);
                layer.onPostUploadToServer();
                ChangesetCache.getInstance().update(changeset);
            }
        };

        try {
            SwingUtilities.invokeAndWait(r);
        } catch (InterruptedException e) {
            lastException = e;
        } catch (InvocationTargetException e) {
            lastException = new OsmTransferException(e.getCause());
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException {
       
        try {
            uploadloop:
            while (true) {
                try {
                    getProgressMonitor().subTask(trn("Uploading {0} object...", "Uploading {0} objects...", toUpload.getSize(), toUpload.getSize()));
                    synchronized (this) {
                        writer = new OsmServerWriter();
                    }
                    writer.uploadOsm(strategy, toUpload.getPrimitives(), changeset, getProgressMonitor().createSubTaskMonitor(1, false));
                    

                    
                    break;
                } catch (OsmTransferCanceledException e) {
                    e.printStackTrace();
                    uploadCanceled = true;
                    break uploadloop;
                } catch (OsmApiPrimitiveGoneException e) {
                    // try to recover from  410 Gone
                    //
                    recoverFromGoneOnServer(e, getProgressMonitor());
                } catch (ChangesetClosedException e) {
                    processedPrimitives.addAll(writer.getProcessedPrimitives()); // OsmPrimitive in => OsmPrimitive out
                    changeset.setOpen(false);
                    switch (e.getSource()) {
                        case UNSPECIFIED:
                            throw e;
                        case UPDATE_CHANGESET:
                        
                            throw e;
                        case UPLOAD_DATA:
                        // Most likely the changeset is full. Try to recover and continue
                            // with a new changeset, but let the user decide first (see
                            // recoverFromChangesetFullException)
                            //
                            if (recoverFromChangesetFullException()) {
                                continue;
                            }
                            lastException = e;
                            break uploadloop;
                    }
                } finally {
                    if (writer != null) {
                        processedPrimitives.addAll(writer.getProcessedPrimitives());
                    }
                    synchronized (this) {
                        writer = null;
                    }
                }
            }
        // if required close the changeset
            //
            if (strategy.isCloseChangesetAfterUpload() && changeset != null && !changeset.isNew() && changeset.isOpen()) {
                OsmApi.getOsmApi().closeChangeset(changeset, progressMonitor.createSubTaskMonitor(0, false));
            }
        } catch (Exception e) {
            if (uploadCanceled) {
                Main.info(tr("Ignoring caught exception because upload is canceled. Exception is: {0}", e.toString()));
            } else {
                lastException = e;
            }
        }
        if (uploadCanceled && processedPrimitives.isEmpty()) {
            return;
        }
        cleanupAfterUpload();
    }

    @Override
    protected void finish() {
        if (uploadCanceled) {
            return;
        }

        // depending on the success of the upload operation and on the policy for
        // multi changeset uploads this will sent the user back to the appropriate
        // place in JOSM, either
        // - to an error dialog
        // - to the Upload Dialog
        // - to map editing
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                // if the changeset is still open after this upload we want it to
                // be selected on the next upload
                //
                ChangesetCache.getInstance().update(changeset);
                if (changeset != null && changeset.isOpen()) {
                    UploadDialog.getUploadDialog().setSelectedChangesetForNextUpload(changeset);
                }
                if (lastException == null) {
                    new Notification(
                            "<h3>" + tr("Upload successful!") + "</h3>")
                            .setIcon(ImageProvider.get("misc", "check_large"))
                            .show();
                    return;
                }
                if (lastException instanceof ChangesetClosedException) {
                    ChangesetClosedException e = (ChangesetClosedException) lastException;
                    if (e.getSource().equals(ChangesetClosedException.Source.UPDATE_CHANGESET)) {
                        handleFailedUpload(lastException);
                        return;
                    }
                    if (strategy.getPolicy() == null) /* do nothing if unknown policy */ {
                        return;
                    }
                    if (e.getSource().equals(ChangesetClosedException.Source.UPLOAD_DATA)) {
                        switch (strategy.getPolicy()) {
                            case ABORT:
                                break; /* do nothing - we return to map editing */

                            case AUTOMATICALLY_OPEN_NEW_CHANGESETS:
                                break; /* do nothing - we return to map editing */

                            case FILL_ONE_CHANGESET_AND_RETURN_TO_UPLOAD_DIALOG:
                            // return to the upload dialog
                                //
                                toUpload.removeProcessed(processedPrimitives);
                                UploadDialog.getUploadDialog().setUploadedPrimitives(toUpload);
                                UploadDialog.getUploadDialog().setVisible(true);
                                break;
                        }
                    } else {
                        handleFailedUpload(lastException);
                    }
                } else {
                    handleFailedUpload(lastException);
                }
            }
        });
    }

    @Override
    protected void cancel() {
        uploadCanceled = true;
        synchronized (this) {
            if (writer != null) {
                writer.cancel();
            }
        }
    }
}
