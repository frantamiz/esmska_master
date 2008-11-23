/*
 * StatusPanel.java
 *
 * Created on 4. leden 2008, 23:27
 */
package esmska.gui;

import esmska.data.Log;
import esmska.utils.L10N;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.Timer;
import org.openide.awt.Mnemonics;

/** Status bar panel
 *
 * @author  ripper
 */
public class StatusPanel extends javax.swing.JPanel {

    private static final Logger logger = Logger.getLogger(StatusPanel.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final DateFormat shortTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
    private Log log = new Log();
    private LogAction logAction = new LogAction();
    private Timer statusTimer = new Timer(5000, new StatusListener());
    
    /** Creates new form StatusPanel */
    public StatusPanel() {
        initComponents();
        statusTimer.setRepeats(false);
        addToLog(l10n.getString("Program_start"), new Date(), null);
    }

    /** Prints message to status bar
     * 
     * @param message text
     * @param printTime show timestamp before text
     * @param icon show icon with text. Use null for no icon.
     * @param addToLog whether the message should be logged
     */
    public void setStatusMessage(String message, boolean printTime, ImageIcon icon,
            boolean addToLog) {
        Date time = new Date();
        if (printTime) {
            String timestamp = shortTimeFormat.format(new Date());
            statusMessageLabel.setText("<html>[" + timestamp + "] " + message + "</html>");
        } else {
            statusMessageLabel.setText("<html>" + message + "</html>");
        }
        statusMessageLabel.setIcon(icon);
        
        //add to log
        if (addToLog) {
            addToLog(message, time, icon);
        }
    }
    
    /** Hide current status message after specified time. If new status message
     *  is displayed in the meantime, this scheduled action is cancelled.
     * @param millis time in milliseconds. Use 0 or negative number to cancel the timer.
     */
    public void hideStatusMessageAfter(int millis) {
        if (millis <= 0) {
            statusTimer.stop();
            return;
        }
        statusTimer.setInitialDelay(millis);
        statusTimer.restart();
    }

    /** Tells main form whether it should display task busy icon */
    public void setTaskRunning(boolean b) {
        if (b == false) {
            statusAnimationLabel.setIcon(new ImageIcon(getClass().getResource(RES + "task-idle.png")));
        } else {
            statusAnimationLabel.setIcon(new ImageIcon(getClass().getResource(RES + "task-busy.gif")));
        }
    }

    /** Set progress on progress bar
     * Use null to any parameter if you don't want to set it
     */
    public void setProgress(Integer value, String text, Boolean stringPainted, Boolean visible) {
        if (value != null) {
            progressBar.setValue(value);
        }
        if (text != null) {
            progressBar.setString(text);
        }
        if (stringPainted != null) {
            progressBar.setStringPainted(stringPainted);
        }
        if (visible != null) {
            progressBar.setVisible(visible);
        }
    }
    
    /** get action to show log frame */
    public Action getLogAction() {
        return logAction;
    }
    
    /** add record to log */
    private void addToLog(String message, Date time, ImageIcon icon) {
        Log.Record record = new Log.Record(message, time, icon);
        log.addRecord(record);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        statusMessageLabel = new JLabel();
        statusAnimationLabel = new JLabel();
        progressBar = new JProgressBar();

        Mnemonics.setLocalizedText(statusMessageLabel, l10n.getString("StatusPanel.statusMessageLabel.text")); // NOI18N
        statusMessageLabel.setToolTipText(l10n.getString("StatusPanel.statusMessageLabel.toolTipText")); // NOI18N
        statusMessageLabel.setFocusable(false);
        statusMessageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        statusMessageLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                statusMessageLabelMouseClicked(evt);
            }
        });

        statusAnimationLabel.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/task-idle.png"))); // NOI18N
        statusAnimationLabel.setFocusable(false);

        progressBar.setMaximum(15);
        progressBar.setFocusable(false);
        progressBar.setString(l10n.getString("StatusPanel.progressBar.string")); // NOI18N
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel, GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addComponent(statusMessageLabel, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 20, Short.MAX_VALUE)
            .addComponent(statusAnimationLabel, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
            .addComponent(progressBar, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void statusMessageLabelMouseClicked(MouseEvent evt) {//GEN-FIRST:event_statusMessageLabelMouseClicked
       logAction.actionPerformed(null);
    }//GEN-LAST:event_statusMessageLabelMouseClicked

    /** show the log frame */
    private class LogAction extends AbstractAction {
        private LogFrame logFrame;
        public LogAction() {
            L10N.setLocalizedText(this, l10n.getString("Log_"));
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "log-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "log-48.png")));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("Show_application_log"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.fine("Showing Log frame...");
            if (logFrame != null && logFrame.isVisible()) {
                logFrame.requestFocus();
                logFrame.toFront();
            } else {
                logFrame = new LogFrame();
                logFrame.setLog(log);
                logFrame.setLocationRelativeTo(MainFrame.getInstance());
                logFrame.setVisible(true);
            }
        }
    }
    
    /** Hide all information in status message label */
    private class StatusListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            statusMessageLabel.setIcon(null);
            statusMessageLabel.setText(null);
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JProgressBar progressBar;
    private JLabel statusAnimationLabel;
    private JLabel statusMessageLabel;
    // End of variables declaration//GEN-END:variables
}
