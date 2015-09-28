package org.openstreetmap.pluging;

import java.util.LinkedList;
import java.util.List;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

public class Comment {

    APIDataSet apiData = new APIDataSet(Main.main.getCurrentDataSet());
    DataSet ds = Main.main.getCurrentDataSet();
    String c = "";

    private List<OsmPrimitive> newway = new LinkedList<OsmPrimitive>();
    private List<OsmPrimitive> updatedway = new LinkedList<OsmPrimitive>();
    private List<OsmPrimitive> deletedway = new LinkedList<OsmPrimitive>();

    private List<OsmPrimitive> newnode = new LinkedList<OsmPrimitive>();
    private List<OsmPrimitive> updatednode = new LinkedList<OsmPrimitive>();
    private List<OsmPrimitive> deletednode = new LinkedList<OsmPrimitive>();

    private List<OsmPrimitive> ways = new LinkedList<OsmPrimitive>();
    private List<OsmPrimitive> nodes = new LinkedList<OsmPrimitive>();
    private List<OsmPrimitive> relations = new LinkedList<OsmPrimitive>();

    public String getComment() {

        ds.allModifiedPrimitives();

        //Llena las listas de wats, nodes y relations.
        for (OsmPrimitive osm : ds.allModifiedPrimitives()) {
            try {
                switch (osm.getDisplayType().getAPIName()) {
                    case "way":
                        ways.add(osm);
                        break;
                    case "node":
                        nodes.add(osm);
                        break;
                    case "relation":
                        relations.add(osm);
                        break;
                    case "closedway":
                        ways.add(osm);
                        break;
                    case "multipolygon":
                        ways.add(osm);
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid");
                }
            } catch (Exception e) {
                System.out.println(e);
            }

        }

        //Tamaño inicial de las listas: ways, nodes y relations.
        int initialways = ways.size();
        int initialnodes = nodes.size();
        int initialrelations = relations.size();

        //Elimina los nodos incluidos en los ways y ubica los ways agregados, modificados y agregados.
        Way way = new Way();
        Node node = new Node();
        for (OsmPrimitive osmway : ways) {
            //Ubica
            if (osmway.isNew() && !osmway.isDeleted()) {
                newway.add(osmway);

            } else if (osmway.isModified() && !osmway.isDeleted()) {
                updatedway.add(osmway);

            } else if (osmway.isDeleted() && !osmway.isNew() && osmway.isModified()) {
                deletedway.add(osmway);
            }

            way = (Way) osmway;

            //Elimina
            try {
                if (nodes.size() > 0) {
                    for (int i = 0; i < nodes.size(); i++) {
                        node = (Node) nodes.get(i);
                        if (way.isInnerNode(node)) {
                            nodes.remove(i);
                        }
                    }
                }

            } catch (Exception en) {
                System.out.println("Deleting error: " + en);
            }

        }
        System.out.println("ways add " + newway.size() + " ways update " + updatedway.size() + " ways deleted " + deletedway.size());

        for (OsmPrimitive osmnode : nodes) {
            //Ubica
            if (osmnode.isNew() && !osmnode.isDeleted()) {
                newnode.add(osmnode);

            } else if (osmnode.isModified() && !osmnode.isDeleted()) {
                updatednode.add(osmnode);

            } else if (osmnode.isDeleted() && !osmnode.isNew() && osmnode.isModified()) {
                deletednode.add(osmnode);
            }
        }

        System.out.println("nodes add " + newnode.size() + " nodes update " + updatednode.size() + " nodes deleted " + deletednode.size());

        //New objects
        c = c + processComment(newway, "way", "added");
        c = c + processComment(newnode, "node", "added");

        //Modified objects
        c = c + processComment(updatedway, "way", "aligned");
        c = c + processComment(updatednode, "node", "aligned");

        //Deleted objects
        c = c + processComment(deletedway, "way", "deleted");
        c = c + processComment(deletednode, "node", "deleted");

        if (c.length() == 0) {
            if (initialnodes == 1) {
                c = c + "A node was aligned";

            } else {
                c = c + "Ways were aligned.";
            }
        }

        System.out.println("Final comment: " + c);
        return c;
    }

    public String commentComplement(int size, int counter, String type, String action) {

        String com = "";
        if (size == 1 && counter == 0) {
            com = com + " a " + type + " was " + action + ". ";
        } else if (size == 1 && counter == 1) {
            com = com + " " + type + " was " + action + ". ";
        } else if (size > 1 && counter == 0) {
            com = com + " " + type + "s were " + action + ". ";
        } else if (size > 1 && counter > 1) {
            com = com + " " + type + "s were " + action + ". ";
        }
        return com;
    }

    public String processComment(List<OsmPrimitive> losp, String type, String action) {
        int flag = 0;
        int fleg = 0;
        String cm = "";

        if (losp.size() > 0) {
            for (int i = 0; i < losp.size(); i++) {
                if (losp.get(i).isTagged() && losp.get(i).getInterestingTags().containsKey("name")) {
                  
                    if (i == losp.size() - 1) {
                        if (losp.size() > 1) {
                            cm = cm + "and " + losp.get(i).getInterestingTags().get("name");
                            flag++;
                        } else {
                            cm = cm + losp.get(i).getInterestingTags().get("name");
                            flag++;
                        }
                    } else {
                        if (losp.size() == 2 || i == losp.size() - 2) {
                            cm = cm + losp.get(i).getInterestingTags().get("name") + " ";
                            flag++;
                        } else {
                            cm = cm + losp.get(i).getInterestingTags().get("name") + ", ";
                            flag++;
                        }
                    }
                }
            }

            if (flag > 0) {
                cm = cm + commentComplement(losp.size(), flag, type, action);
            }

            for (int i = 0; i < losp.size(); i++) {
                if (losp.get(i).isTagged() && !losp.get(i).getInterestingTags().containsKey("name")) {
                    if (i == losp.size() - 1) {
                        if (losp.size() > 1) {
                            cm = cm + "and " + losp.get(i).getInterestingTags().keySet().iterator().next();
                            fleg++;
                        } else {
                            cm = cm + losp.get(i).getInterestingTags().keySet().iterator().next();
                            fleg++;
                        }
                    } else {
                        if (losp.size() == 2 || i == losp.size() - 2) {
                            cm = cm + losp.get(i).getInterestingTags().keySet().iterator().next() + " ";
                            fleg++;
                        } else {
                            cm = cm + losp.get(i).getInterestingTags().keySet().iterator().next() + ", ";
                            fleg++;
                        }
                    }
                }
            }

            if (fleg > 0) {
                cm = cm + commentComplement(losp.size(), fleg, "", action);
            }

        }
        return cm;
    }  
}
