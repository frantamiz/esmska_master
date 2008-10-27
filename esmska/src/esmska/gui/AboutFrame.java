/*
 * AboutFrame.java
 *
 * Created on 8. červenec 2007, 13:18
 */

package esmska.gui;

import esmska.data.Config;
import esmska.persistence.PersistenceManager;
import esmska.utils.L10N;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.apache.commons.io.IOUtils;
import org.jvnet.substance.SubstanceLookAndFeel;

/** About form
 *
 * @author  ripper
 */
public class AboutFrame extends javax.swing.JFrame {
    private static final Logger logger = Logger.getLogger(AboutFrame.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final Config config = PersistenceManager.getConfig();
    
    /** Creates new form AboutFrame */
    public AboutFrame() {
        initComponents();
        closeButton.requestFocusInWindow();
        this.getRootPane().setDefaultButton(closeButton);
        
        //close on Ctrl+W
        String command = "close";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(
                KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), command);
        getRootPane().getActionMap().put(command, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeButtonActionPerformed(e);
            }
        });
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        creditsButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();
        licenseButton = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        linkButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        supportButton = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("esmska/resources/l10n"); // NOI18N
        setTitle(bundle.getString("AboutFrame.title")); // NOI18N
        setIconImage(new ImageIcon(getClass().getResource(RES + "about-24.png")).getImage());
        setLocationByPlatform(true);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/esmska.png"))); // NOI18N
        jLabel1.setFocusable(false);

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getStyle() | java.awt.Font.BOLD, jLabel2.getFont().getSize()+22));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, "Esmska " + config.getLatestVersion());
        jLabel2.setFocusable(false);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, bundle.getString("AboutFrame.jLabel3.text")); // NOI18N
        jLabel3.setFocusable(false);

        creditsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/about-22.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(creditsButton, bundle.getString("AboutFrame.creditsButton.text")); // NOI18N
        creditsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                creditsButtonActionPerformed(evt);
            }
        });

        closeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/close-22.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(closeButton, bundle.getString("Close_")); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(licenseButton, bundle.getString("AboutFrame.licenseButton.text")); // NOI18N
        licenseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                licenseButtonActionPerformed(evt);
            }
        });

        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getSize()-2f));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/copyleft-12.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, bundle.getString("AboutFrame.jLabel5.text")); // NOI18N
        jLabel5.setFocusable(false);

        org.openide.awt.Mnemonics.setLocalizedText(linkButton, bundle.getString("AboutFrame.linkButton.text")); // NOI18N
        linkButton.setToolTipText(bundle.getString("AboutFrame.linkButton.toolTipText")); // NOI18N
        linkButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        linkButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);
        linkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                linkButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(supportButton, "<html><a href=\"" +
            l10n.getString("AboutFrame.supportButton.link") +
            "\">" +
            l10n.getString("AboutFrame.supportButton.text") +
            "</a></html>");
        supportButton.setToolTipText(bundle.getString("AboutFrame.supportButton.toolTipText")); // NOI18N
        supportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        supportButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);
        supportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                supportButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.CENTER, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.CENTER, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.CENTER, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.CENTER, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(creditsButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(licenseButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 45, Short.MAX_VALUE)
                        .addComponent(closeButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(linkButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, 112, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(supportButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 112, Short.MAX_VALUE)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {closeButton, creditsButton, licenseButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(linkButton)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(supportButton)
                    .addComponent(jLabel7)
                    .addComponent(jLabel8))
                .addGap(18, 21, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(creditsButton)
                    .addComponent(closeButton)
                    .addComponent(licenseButton))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jLabel4, jLabel6});

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {closeButton, creditsButton, licenseButton});

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void linkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_linkButtonActionPerformed
        if (!Desktop.isDesktopSupported())
            return;
        //start browser
        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.browse(new URL("http://esmska.googlecode.com/").toURI());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not launch browser", e);
        }
    }//GEN-LAST:event_linkButtonActionPerformed
        
    private void licenseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_licenseButtonActionPerformed
        //show licence
        try {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            URL url = getClass().getResource(RES + "license.txt");
            JTextPane tp = new JTextPane();
            tp.setPage(url);
            tp.setEditable(false);
            tp.setPreferredSize(new Dimension((int)d.getWidth()/2,(int)d.getHeight()/2)); //reasonable size
            String option = l10n.getString("AboutFrame.Acknowledge");
            JOptionPane op = new JOptionPane(new JScrollPane(tp),JOptionPane.INFORMATION_MESSAGE,
                    JOptionPane.DEFAULT_OPTION, null, new Object[]{option}, option);
            JDialog dialog = op.createDialog(this,l10n.getString("AboutFrame.License"));
            dialog.setResizable(true);
            dialog.pack();
            dialog.setVisible(true);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not show license", ex);
        }
    }//GEN-LAST:event_licenseButtonActionPerformed
    
    private void creditsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_creditsButtonActionPerformed
        //show credits
        try {
            String credits = IOUtils.toString(
                    getClass().getResourceAsStream(RES + "credits.html"), "UTF-8");
            String translators = l10n.getString("Translators");
            if ("translator-credits".equals(translators)) {
                //there are no translators mentioned
                translators = "";
            } else {
                translators = "&nbsp;&nbsp;" + translators.replaceAll("\n", "<br>\n").
                    replaceAll("\n  ", "\n&nbsp;&nbsp;");
                //add hyperlinks to the Launchpad URLs
                translators = translators.replaceAll("(https://[^<]*)", 
                        "<a href=\"$1\">$1</a>");
            }
            
            String document = MessageFormat.format(credits, l10n.getString("Credits.authors"),
                    l10n.getString("Credits.contributors"), l10n.getString("Credits.graphics"),
                    l10n.getString("Credits.sponsors"), l10n.getString("Credits.translators"),
                    translators);
            
            JTextPane tp = new JTextPane();
            tp.setContentType("text/html; charset=UTF-8");
            tp.setText(document);
            tp.setEditable(false);
            tp.setPreferredSize(new Dimension(450, 400));
            tp.setCaretPosition(0);
            //make links clickable
            tp.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(final HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED &&
                            Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (Exception ex) {
                            logger.log(Level.SEVERE, "Can't browse hyperlink: " + e.getURL(), ex);
                        }
                    }
                }
            });
            
            String option = l10n.getString("AboutFrame.Thank_you");
            JOptionPane op = new JOptionPane(new JScrollPane(tp),JOptionPane.INFORMATION_MESSAGE,
                    JOptionPane.DEFAULT_OPTION, null, new Object[]{option}, option);
            JDialog dialog = op.createDialog(this,l10n.getString("AboutFrame.Credits"));
            dialog.setResizable(true);
            dialog.pack();
            dialog.setVisible(true);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not show credits", e);
        }
    }//GEN-LAST:event_creditsButtonActionPerformed
    
    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_closeButtonActionPerformed

    private void supportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_supportButtonActionPerformed
        if (!Desktop.isDesktopSupported())
            return;
        //start browser
        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.browse(new URL(l10n.getString("AboutFrame.supportButton.link")).toURI());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not launch browser", e);
        }
}//GEN-LAST:event_supportButtonActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
    private javax.swing.JButton creditsButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JButton licenseButton;
    private javax.swing.JButton linkButton;
    private javax.swing.JButton supportButton;
    // End of variables declaration//GEN-END:variables
    
}
