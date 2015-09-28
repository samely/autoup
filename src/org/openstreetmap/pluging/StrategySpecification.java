package org.openstreetmap.pluging;

import org.openstreetmap.josm.gui.io.UploadStrategy;
import org.openstreetmap.josm.gui.io.UploadStrategySpecification;

public class StrategySpecification {
    
    public UploadStrategySpecification getUploadStrategySpecification() {
        UploadStrategySpecification spec = new UploadStrategySpecification();
        spec.setStrategy(UploadStrategy.SINGLE_REQUEST_STRATEGY);
        spec.setCloseChangesetAfterUpload(true);
        return spec;
    }
    
}
