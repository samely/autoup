
package org.openstreetmap.pluging;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.Preferences.Setting;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.io.BasicUploadSettingsPanel;
import org.openstreetmap.josm.gui.io.ChangesetCommentModel;
import org.openstreetmap.josm.gui.io.ChangesetManagementPanel;
import org.openstreetmap.josm.gui.io.ConfigurationParameterRequestHandler;
import org.openstreetmap.josm.gui.io.TagSettingsPanel;
import org.openstreetmap.josm.gui.io.UploadParameterSummaryPanel;
import org.openstreetmap.josm.gui.io.UploadStrategy;
import org.openstreetmap.josm.gui.io.UploadStrategySelectionPanel;
import org.openstreetmap.josm.gui.io.UploadStrategySpecification;
import org.openstreetmap.josm.gui.io.UploadedObjectsSummaryPanel;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.WindowGeometry;

 
public class UploadDialog extends JDialog implements PropertyChangeListener, PreferenceChangedListener{
     static private UploadDialog uploadDialog;

    
    static private final Collection<Component> customComponents = new ArrayList<Component>();

    
    static public UploadDialog getUploadDialog() {
        if (uploadDialog == null) {
            uploadDialog = new UploadDialog();
        }
        return uploadDialog;
    }

     private UploadedObjectsSummaryPanel pnlUploadedObjects;
     private ChangesetManagementPanel pnlChangesetManagement;

    private BasicUploadSettingsPanel pnlBasicUploadSettings;

    private UploadStrategySelectionPanel pnlUploadStrategySelectionPanel;

     private TagSettingsPanel pnlTagSettings;
     private JTabbedPane tpConfigPanels;
     private JButton btnUpload;
    private boolean canceled = false;

     private final ChangesetCommentModel changesetCommentModel = new ChangesetCommentModel();
    private final ChangesetCommentModel changesetSourceModel = new ChangesetCommentModel();

    
    protected JPanel buildContentPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        
        pnl.add(pnlUploadedObjects = new UploadedObjectsSummaryPanel(), GBC.eol().fill(GBC.BOTH));

         for (Component c : customComponents) {
            pnl.add(c, GBC.eol().fill(GBC.HORIZONTAL));
        }

         
        tpConfigPanels = new JTabbedPane() {
            @Override
            public Dimension getPreferredSize() {                
                return super.getMinimumSize();
            }
        };

        tpConfigPanels.add(pnlBasicUploadSettings = new BasicUploadSettingsPanel(changesetCommentModel, changesetSourceModel));
        tpConfigPanels.setTitleAt(0, tr("Settings"));
        tpConfigPanels.setToolTipTextAt(0, tr("Decide how to upload the data and which changeset to use"));

        tpConfigPanels.add(pnlTagSettings = new TagSettingsPanel(changesetCommentModel, changesetSourceModel));
        tpConfigPanels.setTitleAt(1, tr("Tags of new changeset"));
        tpConfigPanels.setToolTipTextAt(1, tr("Apply tags to the changeset data is uploaded to"));

        tpConfigPanels.add(pnlChangesetManagement = new ChangesetManagementPanel(changesetCommentModel));
        tpConfigPanels.setTitleAt(2, tr("Changesets"));
        tpConfigPanels.setToolTipTextAt(2, tr("Manage open changesets and select a changeset to upload to"));

        tpConfigPanels.add(pnlUploadStrategySelectionPanel = new UploadStrategySelectionPanel());
        tpConfigPanels.setTitleAt(3, tr("Advanced"));
        tpConfigPanels.setToolTipTextAt(3, tr("Configure advanced settings"));

        pnl.add(tpConfigPanels, GBC.eol().fill(GBC.HORIZONTAL));
        return pnl;
    }
 
    protected JPanel buildActionPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.CENTER));
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // -- upload button
        UploadAction uploadAction = new UploadAction();
        pnl.add(btnUpload = new SideButton(uploadAction));
        btnUpload.setFocusable(true);
        InputMapUtils.enableEnter(btnUpload);

        // -- cancel button
        CancelAction cancelAction = new CancelAction();
        pnl.add(new SideButton(cancelAction));
        getRootPane().registerKeyboardAction(
                cancelAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        pnl.add(new SideButton(new ContextSensitiveHelpAction(ht("/Dialog/Upload"))));
        HelpUtil.setHelpContext(getRootPane(),ht("/Dialog/Upload"));
        return pnl;
    }

    /**
     * builds the gui
     */
    protected void build() {
        setTitle(tr("Upload to ''{0}''", OsmApi.getOsmApi().getBaseUrl()));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildContentPanel(), BorderLayout.CENTER);
        getContentPane().add(buildActionPanel(), BorderLayout.SOUTH);

        addWindowListener(new WindowEventHandler());


        pnlChangesetManagement.addPropertyChangeListener(
                pnlBasicUploadSettings.getUploadParameterSummaryPanel()
        );
        pnlChangesetManagement.addPropertyChangeListener(this);
        pnlUploadedObjects.addPropertyChangeListener(
                pnlBasicUploadSettings.getUploadParameterSummaryPanel()
        );
        pnlUploadedObjects.addPropertyChangeListener(pnlUploadStrategySelectionPanel);
        pnlUploadStrategySelectionPanel.addPropertyChangeListener(
                pnlBasicUploadSettings.getUploadParameterSummaryPanel()
        );


        pnlBasicUploadSettings.getUploadParameterSummaryPanel().setConfigurationParameterRequestListener(
                new ConfigurationParameterRequestHandler() {
                    @Override
                    public void handleUploadStrategyConfigurationRequest() {
                        tpConfigPanels.setSelectedIndex(3);
                    }
                    @Override
                    public void handleChangesetConfigurationRequest() {
                        tpConfigPanels.setSelectedIndex(2);
                    }
                }
        );

        pnlBasicUploadSettings.setUploadTagDownFocusTraversalHandlers(
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btnUpload.requestFocusInWindow();
                    }
                }
        );

        Main.pref.addPreferenceChangeListener(this);
    }

    public UploadDialog() {
        super(JOptionPane.getFrameForComponent(Main.parent), ModalityType.DOCUMENT_MODAL);
        build();
    }

    
    public void setUploadedPrimitives(APIDataSet toUpload) {
        if (toUpload == null) {
            List<OsmPrimitive> emptyList = Collections.emptyList();
            pnlUploadedObjects.setUploadedPrimitives(emptyList, emptyList, emptyList);
            return;
        }
        pnlUploadedObjects.setUploadedPrimitives(
                toUpload.getPrimitivesToAdd(),
                toUpload.getPrimitivesToUpdate(),
                toUpload.getPrimitivesToDelete()
        );
    }

    
    public void rememberUserInput() {
        pnlBasicUploadSettings.rememberUserInput();
        pnlUploadStrategySelectionPanel.rememberUserInput();
    }

    public void startUserInput() {
        tpConfigPanels.setSelectedIndex(0);
        pnlBasicUploadSettings.startUserInput();
        pnlTagSettings.startUserInput();
        pnlTagSettings.initFromChangeset(pnlChangesetManagement.getSelectedChangeset());
        pnlUploadStrategySelectionPanel.initFromPreferences();
        UploadParameterSummaryPanel pnl = pnlBasicUploadSettings.getUploadParameterSummaryPanel();
        pnl.setUploadStrategySpecification(pnlUploadStrategySelectionPanel.getUploadStrategySpecification());
        pnl.setCloseChangesetAfterNextUpload(pnlChangesetManagement.isCloseChangesetAfterUpload());
        pnl.setNumObjects(pnlUploadedObjects.getNumObjectsToUpload());
    }

   
    public Changeset getChangeset() {
        Changeset cs = pnlChangesetManagement.getSelectedChangeset();
        if (cs == null) {
            cs = new Changeset();
        }
        cs.setKeys(pnlTagSettings.getTags(false));
        return cs;
    }

    public void setSelectedChangesetForNextUpload(Changeset cs) {
        pnlChangesetManagement.setSelectedChangesetForNextUpload(cs);
    }

    public Map<String, String> getDefaultChangesetTags() {
        return pnlTagSettings.getDefaultTags();
    }

    public void setDefaultChangesetTags(Map<String, String> tags) {
        pnlTagSettings.setDefaultTags(tags);
         for (Entry<String, String> entry: tags.entrySet()) {
            if ("comment".equals(entry.getKey())) {
                changesetCommentModel.setComment(entry.getValue());
            }
        }
    }

   
    public UploadStrategySpecification getUploadStrategySpecification() {
        UploadStrategySpecification spec = pnlUploadStrategySelectionPanel.getUploadStrategySpecification();
        spec.setCloseChangesetAfterUpload(pnlChangesetManagement.isCloseChangesetAfterUpload());
        return spec;
    }

    
    protected String getUploadComment() {
        return changesetCommentModel.getComment();
    }

    
    protected String getUploadSource() {
        return changesetSourceModel.getComment();
    }

    public boolean isCanceled() {
        return canceled;
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            Main.parent,
                            new Dimension(400,600)
                    )
            ).applySafe(this);
            startUserInput();
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    
    public static boolean addCustomComponent(Component c) {
        if (c != null) {
            return customComponents.add(c);
        }
        return false;
    }

   
    class UploadAction extends AbstractAction {
        public UploadAction() {
            putValue(NAME, tr("Upload Changes"));
            putValue(SMALL_ICON, ImageProvider.get("upload"));
            putValue(SHORT_DESCRIPTION, tr("Upload the changed primitives"));
        }

       
        protected boolean warnUploadComment() {
            return warnUploadTag(
                    tr("Please revise upload comment"),
                    tr("Your upload comment is <i>empty</i>, or <i>very short</i>.<br /><br />" +
                            "This is technically allowed, but please consider that many users who are<br />" +
                            "watching changes in their area depend on meaningful changeset comments<br />" +
                            "to understand what is going on!<br /><br />" +
                            "If you spend a minute now to explain your change, you will make life<br />" +
                            "easier for many other mappers."),
                    "upload_comment_is_empty_or_very_short"
            );
        }

       
        protected boolean warnUploadSource() {
            return warnUploadTag(
                    tr("Please specify a changeset source"),
                    tr("You did not specify a source for your changes.<br />" +
                            "This is technically allowed, but it assists other users <br />" +
                            "to understand the origins of the data.<br /><br />" +
                            "If you spend a minute now to explain your change, you will make life<br />" +
                            "easier for many other mappers."),
                    "upload_source_is_empty"
            );
        }

        protected boolean warnUploadTag(final String title, final String message, final String togglePref) {
            ExtendedDialog dlg = new ExtendedDialog(UploadDialog.this,
                    title,
                    new String[] {tr("Revise"), tr("Cancel"), tr("Continue as is")});
            dlg.setContent("<html>" + message + "</html>");
            dlg.setButtonIcons(new Icon[] {
                    ImageProvider.get("ok"),
                    ImageProvider.get("cancel")
//                    ImageProvider.overlay(
//                            ImageProvider.get("upload"),
//                            new ImageIcon(ImageProvider.get("warning-small").getImage().getScaledInstance(10 , 10, Image.SCALE_SMOOTH)),
//                            ImageProvider.OverlayPosition.SOUTHEAST)
            });
            dlg.setToolTipTexts(new String[] {
                    tr("Return to the previous dialog to enter a more descriptive comment"),
                    tr("Cancel and return to the previous dialog"),
                    tr("Ignore this hint and upload anyway")});
            dlg.setIcon(JOptionPane.WARNING_MESSAGE);
            dlg.toggleEnable(togglePref);
            //dlg.setToggleCheckboxText(tr("Do not show this message again"));
            dlg.setCancelButton(1, 2);
            return dlg.showDialog().getValue() != 3;
        }

        protected void warnIllegalChunkSize() {
            HelpAwareOptionPane.showOptionDialog(
                    UploadDialog.this,
                    tr("Please enter a valid chunk size first"),
                    tr("Illegal chunk size"),
                    JOptionPane.ERROR_MESSAGE,
                    ht("/Dialog/Upload#IllegalChunkSize")
            );
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if ((getUploadComment().trim().length() < 10 && warnUploadComment()) /* abort for missing comment */
                    || (getUploadSource().trim().isEmpty() && warnUploadSource()) /* abort for missing changeset source */
                    ) {
                tpConfigPanels.setSelectedIndex(0);
                pnlBasicUploadSettings.initEditingOfUploadComment();
                return;
            }

            /* test for empty tags in the changeset metadata and proceed only after user's confirmation.
             * though, accept if key and value are empty (cf. xor). */
            List<String> emptyChangesetTags = new ArrayList<String>();
            for (final Entry<String, String> i : pnlTagSettings.getTags(true).entrySet()) {
                final boolean isKeyEmpty = i.getKey() == null || i.getKey().trim().isEmpty();
                final boolean isValueEmpty = i.getValue() == null || i.getValue().trim().isEmpty();
                final boolean ignoreKey = "comment".equals(i.getKey()) || "source".equals(i.getKey());
                if ((isKeyEmpty ^ isValueEmpty) && !ignoreKey) {
                    emptyChangesetTags.add(tr("{0}={1}", i.getKey(), i.getValue()));
                }
            }
            if (!emptyChangesetTags.isEmpty() && JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(
                    Main.parent,
                    trn(
                            "<html>The following changeset tag contains an empty key/value:<br>{0}<br>Continue?</html>",
                            "<html>The following changeset tags contain an empty key/value:<br>{0}<br>Continue?</html>",
                            emptyChangesetTags.size(), Utils.joinAsHtmlUnorderedList(emptyChangesetTags)),
                    tr("Empty metadata"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            )) {
                tpConfigPanels.setSelectedIndex(0);
                pnlBasicUploadSettings.initEditingOfUploadComment();
                return;
            }

            UploadStrategySpecification strategy = getUploadStrategySpecification();
            if (strategy.getStrategy().equals(UploadStrategy.CHUNKED_DATASET_STRATEGY)) {
                if (strategy.getChunkSize() == UploadStrategySpecification.UNSPECIFIED_CHUNK_SIZE) {
                    warnIllegalChunkSize();
                    tpConfigPanels.setSelectedIndex(0);
                    return;
                }
            }
            setCanceled(false);
            setVisible(false);
        }
    }

   
    class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, tr("Cancel"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
            putValue(SHORT_DESCRIPTION, tr("Cancel the upload and resume editing"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setCanceled(true);
            setVisible(false);
        }
    }

   
    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            setCanceled(true);
        }

        @Override
        public void windowActivated(WindowEvent arg0) {
            if (tpConfigPanels.getSelectedIndex() == 0) {
                pnlBasicUploadSettings.initEditingOfUploadComment();
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ChangesetManagementPanel.SELECTED_CHANGESET_PROP)) {
            Changeset cs = (Changeset)evt.getNewValue();
            if (cs == null) {
                tpConfigPanels.setTitleAt(1, tr("Tags of new changeset"));
            } else {
                tpConfigPanels.setTitleAt(1, tr("Tags of changeset {0}", cs.getId()));
            }
        }
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e.getKey() == null || ! e.getKey().equals("osm-server.url"))
            return;
        final Setting<?> newValue = e.getNewValue();
        final String url;
        if (newValue == null || newValue.getValue() == null) {
            url = OsmApi.getOsmApi().getBaseUrl();
        } else {
            url = newValue.getValue().toString();
        }
        setTitle(tr("Upload to ''{0}''", url));
    }
}