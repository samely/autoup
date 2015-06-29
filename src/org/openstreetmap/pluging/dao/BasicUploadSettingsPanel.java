/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openstreetmap.pluging.dao;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.io.ChangesetCommentModel;
import org.openstreetmap.josm.gui.io.UploadParameterSummaryPanel;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.pluging.main.autoUploadDialog;

/**
 * BasicUploadSettingsPanel allows to enter the basic parameters required for
 * uploading data.
 *
 */
public class BasicUploadSettingsPanel extends JPanel {

    public static final String HISTORY_KEY = "upload.comment.history";
    public static final String HISTORY_LAST_USED_KEY = "upload.comment.last-used";
    public static final String SOURCE_HISTORY_KEY = "upload.source.history";

    /**
     * the history combo box for the upload comment
     */
    private final HistoryComboBox hcbUploadComment = new HistoryComboBox();
    private final HistoryComboBox hcbUploadSource = new HistoryComboBox();

    /**
     * the panel with a summary of the upload parameters
     */
    private final UploadParameterSummaryPanel pnlUploadParameterSummary = new UploadParameterSummaryPanel();

    private final ChangesetCommentModel changesetCommentModel;
    private final ChangesetCommentModel changesetSourceModel;
    private String jtfComment;
    private String jtfSource;
    public String a;
    public String b;
    

    public BasicUploadSettingsPanel() {
        this.changesetCommentModel = null;
        this.changesetSourceModel = null;
    }

    public void enviar(String c, String s) {
        jtfComment = c;
        jtfSource = s;
        a = c;
        b = s;
        System.out.println("Nuevo metodo" + jtfComment + jtfSource+a+b);

    }
    

    protected JPanel buildUploadCommentPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());
        System.out.println("Esto se recibe en la clase basicuploadsettingspanel inicio:" + a + b+jtfComment+jtfSource);

        pnl.add(new JLabel(tr("Provide a brief comment for the changes you are uploading:")), GBC.eol().insets(0, 5, 10, 3));
        hcbUploadComment.setToolTipText(tr("Enter an upload comment"));
        hcbUploadComment.setMaxTextLength(Changeset.MAX_CHANGESET_TAG_LENGTH);
        List<String> cmtHistory = new LinkedList<String>(Main.pref.getCollection(HISTORY_KEY, new LinkedList<String>()));
        Collections.reverse(cmtHistory); // we have to reverse the history, because ComboBoxHistory will reverse it again in addElement()
        hcbUploadComment.setPossibleItems(cmtHistory);
        final CommentModelListener commentModelListener = new CommentModelListener(a, hcbUploadComment, changesetCommentModel);
        hcbUploadComment.getEditor().addActionListener(commentModelListener);
        hcbUploadComment.getEditor().getEditorComponent().addFocusListener(commentModelListener);
        pnl.add(hcbUploadComment, GBC.eol().fill(GBC.HORIZONTAL));

        pnl.add(new JLabel(tr("Specify the data source for the changes:")), GBC.eol().insets(0, 8, 10, 3));
        hcbUploadSource.setToolTipText(tr("Enter a source"));
        List<String> sourceHistory = new LinkedList<String>(Main.pref.getCollection(SOURCE_HISTORY_KEY, new LinkedList<String>()));
        Collections.reverse(sourceHistory); // we have to reverse the history, because ComboBoxHistory will reverse it again in addElement()
        hcbUploadSource.setPossibleItems(sourceHistory);
        final CommentModelListener sourceModelListener = new CommentModelListener(b, hcbUploadSource, changesetSourceModel);
        hcbUploadSource.getEditor().addActionListener(sourceModelListener);
        hcbUploadSource.getEditor().getEditorComponent().addFocusListener(sourceModelListener);
        pnl.add(hcbUploadSource, GBC.eol().fill(GBC.HORIZONTAL));

        System.out.println("Esto se recibe en la clase basicuploadsettingspanel:" + a + b);

        return pnl;
    }

    protected void build() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        add(buildUploadCommentPanel(), BorderLayout.NORTH);
        add(pnlUploadParameterSummary, BorderLayout.CENTER);
    }

    /**
     * Creates the panel
     *
     * @param jtfComment
     * @param jtfSource
     * @param changesetCommentModel the model for the changeset comment. Must
     * not be null
     * @param changesetSourceModel the model for the changeset source. Must not
     * be null.
     * @throws IllegalArgumentException thrown if {@code changesetCommentModel}
     * is null
     */
    public BasicUploadSettingsPanel(String jtfComment, String jtfSource, ChangesetCommentModel changesetCommentModel, ChangesetCommentModel changesetSourceModel) {
        CheckParameterUtil.ensureParameterNotNull(changesetCommentModel, "changesetCommentModel");
        CheckParameterUtil.ensureParameterNotNull(changesetSourceModel, "changesetSourceModel");
        this.changesetCommentModel = changesetCommentModel;
        this.changesetSourceModel = changesetSourceModel;
//        this.jtfComment = jtfComment;
//        this.jtfSource = jtfSource;
        changesetCommentModel.addObserver(new ChangesetCommentObserver(hcbUploadComment));
        changesetSourceModel.addObserver(new ChangesetCommentObserver(hcbUploadSource));
        build();
        System.out.println("Esto se recibe en basicuploadsettingspanel como parametros:" + jtfComment + jtfSource);
        System.out.println("ahora el valor de commen y source es:" + this.jtfComment + this.jtfSource);
    }

    public void setUploadTagDownFocusTraversalHandlers(final Action handler) {
        setHistoryComboBoxDownFocusTraversalHandler(handler, hcbUploadComment);
        setHistoryComboBoxDownFocusTraversalHandler(handler, hcbUploadSource);
    }

    public void setHistoryComboBoxDownFocusTraversalHandler(final Action handler, final HistoryComboBox hcb) {
        hcb.getEditor().addActionListener(handler);
        hcb.getEditor().getEditorComponent().addKeyListener(
                new KeyListener() {
                    @Override
                    public void keyTyped(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_TAB) {
                            handler.actionPerformed(new ActionEvent(hcb, 0, "focusDown"));
                        }
                    }

                    @Override
                    public void keyReleased(KeyEvent e) {
                    }

                    @Override
                    public void keyPressed(KeyEvent e) {
                    }
                }
        );
    }

    /**
     * Remembers the user input in the preference settings
     */
    public void rememberUserInput() {
        // store the history of comments
        hcbUploadComment.addCurrentItemToHistory();
        Main.pref.putCollection(HISTORY_KEY, hcbUploadComment.getHistory());
        Main.pref.putInteger(HISTORY_LAST_USED_KEY, (int) (System.currentTimeMillis() / 1000));
        // store the history of sources
        hcbUploadSource.addCurrentItemToHistory();
        Main.pref.putCollection(SOURCE_HISTORY_KEY, hcbUploadSource.getHistory());
    }

    /**
     * Initializes the panel for user input
     */
    public void startUserInput() {
        List<String> history = hcbUploadComment.getHistory();
        int age = (int) (System.currentTimeMillis() / 1000 - Main.pref.getInteger(HISTORY_LAST_USED_KEY, 0));
        // only pre-select latest entry if used less than 4 hours ago.
        if (age < 4 * 3600 * 1000 && history != null && !history.isEmpty()) {
            hcbUploadComment.setText(history.get(0));
        }
        hcbUploadComment.requestFocusInWindow();
        hcbUploadComment.getEditor().getEditorComponent().requestFocusInWindow();
    }

    public void initEditingOfUploadComment() {
        hcbUploadComment.getEditor().selectAll();
        hcbUploadComment.requestFocusInWindow();
    }

    public UploadParameterSummaryPanel getUploadParameterSummaryPanel() {
        return pnlUploadParameterSummary;
    }

    /**
     * Updates the changeset comment model upon changes in the input field.
     */
    class CommentModelListener extends FocusAdapter implements ActionListener {

        final HistoryComboBox source;
        final ChangesetCommentModel destination;
        final String jtf;

        CommentModelListener(String jtf, HistoryComboBox source, ChangesetCommentModel destination) {
            this.source = source;
            this.jtf = jtf;
            this.destination = destination;
            System.out.println("Esto es el codigo" + destination.getComment().trim());
            System.out.println("Esto es el string" + jtf);

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //destination.setComment(source.getText());
            destination.setComment(jtf);
        }

        @Override
        public void focusLost(FocusEvent e) {
            //destination.setComment(source.getText());
            destination.setComment(jtf);
        }
    }

    /**
     * Observes the changeset comment model and keeps the comment input field in
     * sync with the current changeset comment
     */
    class ChangesetCommentObserver implements Observer {

        private final HistoryComboBox destination;

        ChangesetCommentObserver(HistoryComboBox destination) {
            this.destination = destination;
        }

        @Override
        public void update(Observable o, Object arg) {
            if (!(o instanceof ChangesetCommentModel)) {
                return;
            }
            String newComment = (String) arg;
            if (!destination.getText().equals(newComment)) {
                destination.setText(newComment);
            }
        }
    }
}
