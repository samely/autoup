package org.openstreetmap.pluging;

import java.util.List;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.ImageryLayer;

public class Source {

    public String getSource() {
        String s = "";
        List<ImageryLayer> il=Main.map.mapView.getLayersOfType(ImageryLayer.class);
         
        for (int i = 0; i < il.size(); i++) {
            if (i == il.size() - 1) {
                s = s + " " + (il.get(i).getName());
            } else {
                s = s + " " + (il.get(i).getName()) + ", ";
            }
        }
        System.out.println(s);
        return s;
    }

}
