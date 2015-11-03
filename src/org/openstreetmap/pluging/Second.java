package org.openstreetmap.pluging;

//import org.openstreetmap.josm.gui.IconToggleButton;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;


/**
 *
 * @author samely
 */
public class Second extends Plugin {

    //private IconToggleButton btnIcon;
    protected static autoUploadDialog atUploadDialog;

    public Second(PluginInformation info) {
        super(info);
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (newFrame != null) {
            newFrame.addToggleDialog(atUploadDialog = new autoUploadDialog());
        }
    }
}