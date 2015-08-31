package org.openstreetmap.pluging.main;

import org.openstreetmap.josm.gui.IconToggleButton;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 *
 * @author samely
 */
public class autoUpload extends Plugin {

    private IconToggleButton btnIcon;
    protected static autoUploadDialog atUploadDialog;

    public autoUpload(PluginInformation info) {
        super(info);
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (newFrame != null) {
            newFrame.addToggleDialog(atUploadDialog = new autoUploadDialog());
        }
    }
}