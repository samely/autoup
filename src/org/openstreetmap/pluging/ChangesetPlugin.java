package org.openstreetmap.pluging;

import java.util.HashMap;
import java.util.Map;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.Changeset;

public class ChangesetPlugin {

    public Changeset getChangeset() {

        Changeset cs = new Changeset();
        cs.setKeys(getDefaultChangesetTags());
        return cs;
    }

    public Map<String, String> getDefaultChangesetTags() {
        Comment c = new Comment();
        Source s = new Source();
        Map<String, String> tags = new HashMap<String, String>();
        String agent = Version.getInstance().getAgentString(false); 

        tags.put("comment", c.getComment());
        tags.put("source", s.getSource());               
        tags.put("created_by", agent);
        
        return tags;
    }

}
