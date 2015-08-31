package org.openstreetmap.pluging.dao;
// License: GPL. For details, see LICENSE file.

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.Preferences.Setting;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;

import org.openstreetmap.josm.gui.io.ChangesetCommentModel;
import org.openstreetmap.josm.gui.io.ChangesetManagementPanel;
import org.openstreetmap.josm.gui.io.ConfigurationParameterRequestHandler;
import org.openstreetmap.josm.gui.io.TagSettingsPanel;
import org.openstreetmap.josm.gui.io.UploadParameterSummaryPanel;
import org.openstreetmap.josm.gui.io.UploadStrategy;
import org.openstreetmap.josm.gui.io.UploadStrategySpecification;

import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.WindowGeometry;
import org.openstreetmap.pluging.main.autoUploadDialog;

public class UploadDialog extends JDialog implements PropertyChangeListener, PreferenceChangedListener {

    static private UploadDialog uploadDialog;

    static private final Collection<Component> customComponents = new ArrayList<Component>();
    static String jtfC;
    static String jtfS;

    // private static final BasicUploadSettingsPanel busp = new BasicUploadSettingsPanel();
    static public UploadDialog getUploadDialog(String jtfComment, String jtfSource) {
        jtfC = jtfComment;
        jtfS = jtfSource;
        //busp.enviar(jtfComment, jtfSource);
        System.out.println("Esto se recibe en getuploaddialog:" + jtfComment + jtfC + jtfSource + jtfS);

        if (uploadDialog == null) {
            uploadDialog = new UploadDialog();
            UploadDialog.jtfC = jtfComment;
            UploadDialog.jtfS = jtfSource;
        }
        return uploadDialog;

    }

    public UploadDialog() {
        super(JOptionPane.getFrameForComponent(Main.parent), ModalityType.DOCUMENT_MODAL);
//        busp.enviar(jtfC, jtfS);
        build();
    }
    /**
     * the panel with the objects to upload
     */
    private BasicUploadSettingsPanel pnlBasicUploadSettings;

    private TagSettingsPanel pnlTagSettings;
    /**
     * the tabbed pane used below of the list of primitives
     */
    private JTabbedPane tpConfigPanels;
    /**
     * the upload button
     */
    private JButton btnUpload;
    private boolean canceled = false;

    private final ChangesetCommentModel changesetCommentModel = new ChangesetCommentModel();
    private final ChangesetCommentModel changesetSourceModel = new ChangesetCommentModel();

    /**
     * builds the content panel for the upload dialog
     *
     * @return the content panel
     */
    protected JPanel buildContentPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        for (Component c : customComponents) {
            pnl.add(c, GBC.eol().fill(GBC.HORIZONTAL));
        }

        tpConfigPanels = new JTabbedPane() {
            @Override
            public Dimension getPreferredSize() {
                // make sure the tabbed pane never grabs more space than necessary
                //
                return super.getMinimumSize();
            }
        };

        System.out.println("Estas son las entradas importantes en uploaddialog:" + UploadDialog.jtfC + UploadDialog.jtfS + uploadDialog.jtfC + uploadDialog.jtfS);

        tpConfigPanels.add(pnlBasicUploadSettings = new BasicUploadSettingsPanel(UploadDialog.jtfC, UploadDialog.jtfS, changesetCommentModel, changesetSourceModel));
        tpConfigPanels.setTitleAt(0, tr("Settings"));
        tpConfigPanels.setToolTipTextAt(0, tr("Decide how to upload the data and which changeset to use"));

        tpConfigPanels.add(pnlTagSettings = new TagSettingsPanel(changesetCommentModel, changesetSourceModel));
        tpConfigPanels.setTitleAt(1, tr("Tags of new changeset"));
        tpConfigPanels.setToolTipTextAt(1, tr("Apply tags to the changeset data is uploaded to"));

        System.out.println("Esto recibe basicuploadsettingpanel:" + jtfC + jtfS);
        pnl.add(tpConfigPanels, GBC.eol().fill(GBC.HORIZONTAL));
        return pnl;
    }

    /**
     * builds the panel with the OK and CANCEL buttons
     *
     * @return The panel with the OK and CANCEL buttons
     */
    protected JPanel buildActionPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.CENTER));
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

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
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        pnl.add(new SideButton(new ContextSensitiveHelpAction(ht("/Dialog/Upload"))));
        HelpUtil.setHelpContext(getRootPane(), ht("/Dialog/Upload"));
        return pnl;
    }

    protected void build() {
        setTitle(tr("Upload to ''{0}''", OsmApi.getOsmApi().getBaseUrl()));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildContentPanel(), BorderLayout.CENTER);
        getContentPane().add(buildActionPanel(), BorderLayout.SOUTH);

        addWindowListener(new WindowEventHandler());

        // make sure the configuration panels listen to each other
        // changes
        //
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

    /**
     * constructor
     */
    public void startUserInput() {
        tpConfigPanels.setSelectedIndex(0);
        pnlBasicUploadSettings.startUserInput();
        pnlTagSettings.startUserInput();

        UploadParameterSummaryPanel pnl = pnlBasicUploadSettings.getUploadParameterSummaryPanel();
    }

    //interesa solo el getchangeset
    public Changeset getChangeset() {

        Changeset cs = new Changeset();
        cs.setKeys(getDefaultChangesetTags());
        return cs;
    }

    public Map<String, String> getDefaultChangesetTags() {
        Map<String, String> tags = new HashMap<String, String>();
        try {
            tags.put("comment", jtfC);
            tags.put("source", jtfS);
            String agent = Version.getInstance().getAgentString(false);
            System.out.println("Este es el agente" + agent);
            tags.put("created_by", agent);
            return tags;

        } catch (Exception e) {

        }
        return tags;
    }

    public UploadStrategySpecification getUploadStrategySpecification() {
        UploadStrategySpecification spec = new UploadStrategySpecification();
        spec.setStrategy(UploadStrategy.SINGLE_REQUEST_STRATEGY);
        spec.setCloseChangesetAfterUpload(true);
        return spec;
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
                            new Dimension(400, 600)
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

    autoUploadDialog aud = new autoUploadDialog();

    class UploadAction extends AbstractAction {

        public UploadAction() {
            putValue(NAME, tr("Upload Changes"));
            putValue(SMALL_ICON, ImageProvider.get("upload"));
            putValue(SHORT_DESCRIPTION, tr("Upload the changed primitives"));
        }

        protected boolean warnUploadComment() {
            return aud.getComment().isEmpty();
        }

        protected boolean warnUploadSource() {
            return aud.getSource().isEmpty();
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            if ((aud.getComment().trim().length() < 10 && warnUploadComment())
                    || (aud.getSource().trim().isEmpty() && warnUploadSource())) {

                tpConfigPanels.setSelectedIndex(0);
                pnlBasicUploadSettings.initEditingOfUploadComment();
                return;
            }

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
            UploadStrategySpecification strategySpecification = new UploadStrategySpecification(strategy);
            if (strategy.getStrategy().equals(UploadStrategy.CHUNKED_DATASET_STRATEGY)) {
                if (strategy.getChunkSize() == UploadStrategySpecification.UNSPECIFIED_CHUNK_SIZE) {
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
            Changeset cs = (Changeset) evt.getNewValue();
            if (cs == null) {
                tpConfigPanels.setTitleAt(1, tr("Tags of new changeset"));
            } else {
                tpConfigPanels.setTitleAt(1, tr("Tags of changeset {0}", cs.getId()));
            }
        }
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e.getKey() == null || !e.getKey().equals("osm-server.url")) {
            return;
        }
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
