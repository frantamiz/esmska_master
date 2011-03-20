/*
 * ConfigFrame.java
 *
 * Created on 20. červenec 2007, 18:59
 */

package esmska.gui;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticTheme;
import esmska.Context;
import esmska.data.Config.CheckUpdatePolicy;
import esmska.data.Gateways.Events;
import esmska.data.event.ValuedEvent;
import esmska.gui.ThemeManager.LAF;
import esmska.data.Config;
import esmska.data.Contact;
import esmska.data.Keyring;
import esmska.data.Gateway;
import esmska.data.Gateways;
import esmska.data.Icons;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.ELProperty;
import esmska.transfer.ProxyManager;
import esmska.data.event.AbstractDocumentListener;
import esmska.utils.L10N;
import esmska.data.Tuple;
import esmska.data.event.ValuedListener;
import esmska.gui.InfoLabel.Type;
import esmska.utils.MiscUtils;
import esmska.utils.RuntimeUtils;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.jdesktop.beansbinding.Converter;
import org.openide.awt.Mnemonics;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultTableCellRenderer;
import org.pushingpixels.substance.api.skin.SkinInfo;

/** Configure settings form
 *
 * @author  ripper
 */
public class ConfigFrame extends javax.swing.JFrame {
    private static final Logger logger = Logger.getLogger(ConfigFrame.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final Keyring keyring = Keyring.getInstance();
    private static final Gateways gateways = Gateways.getInstance();
    /** when to take updates seriously */
    private boolean fullyInicialized;
    private DefaultComboBoxModel lafModel = new DefaultComboBoxModel();
    private Character passwordEchoChar;
    private static final HashSet<String> restartRequests = new HashSet<String>();
    private static final HashMap<String, Object> originalSettings = new HashMap<String, Object>();
    private final GatewayTableModel gwTableModel =  new GatewayTableModel();
    private final GatewaySelectionListener gwSelectionListener = new GatewaySelectionListener();

    public enum Tabs {
        GENERAL, APPEARANCE, GATEWAYS, PRIVACY, CONNECTION
    }

    /** Creates new form ConfigFrame */
    public ConfigFrame() {
        initComponents();

        //set window images
        ArrayList<Image> images = new ArrayList<Image>();
        images.add(Icons.get("config-16.png").getImage());
        images.add(Icons.get("config-32.png").getImage());
        images.add(Icons.get("config-48.png").getImage());
        setIconImages(images);

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
        
        //set tab mnemonics
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            L10N.setLocalizedText(tabbedPane, i, tabbedPane.getTitleAt(i));
        }
        
        //add LaFs to combo box
        for (LAF laf : LAF.values()) {
            if (ThemeManager.isLaFSupported(laf)) {
                lafModel.addElement(laf);
            }
        }
        
        //select chosen laf
        LAF laf = config.getLookAndFeel();
        if (lafModel.getIndexOf(laf) >= 0) {
            lafModel.setSelectedItem(laf);
        } else {
            logger.warning("Chosen LaF '" + laf + "' not present in " +
                    "the list of available LaFs!");
        }

        //disable some stuff on java webstart
        if (RuntimeUtils.isRunAsWebStart()) {
            checkUpdatesCheckBox.setEnabled(false);
            updatePolicyComboBox.setEnabled(false);
            unstableUpdatesCheckBox.setEnabled(false);
        }

        //update other components
        updateThemeComboBox();
        updateUnstableUpdateCheckbox();
        if (!NotificationIcon.isSupported()) {
            notificationAreaCheckBox.setSelected(false);
        }
        updateInfoLabel();
        countryPrefixPanel.setCountryPrefix(config.getCountryPrefix());

        //adjust columns widths for gatewayTable
        TableCellRenderer headerRenderer = gatewayTable.getTableHeader().getDefaultRenderer();
        TableColumnModel columnModel = gatewayTable.getColumnModel();
        int colsWidth = 0;
        for (int i = 0; i <= 1; i++) {
            //first two columns should be as narrow as possible
            TableColumn column = columnModel.getColumn(i);
            Component comp = headerRenderer.getTableCellRendererComponent(
                    null, column.getHeaderValue(),
                    false, false, 0, 0);
            int headerWidth = comp.getPreferredSize().width + 10; //add some pixels, otherwise it's cropped under Substance
            column.setPreferredWidth(headerWidth);
            colsWidth += headerWidth;
        }
        //the last column should use all the rest of the space
        columnModel.getColumn(2).setPreferredWidth(gatewayTable.getWidth() - colsWidth);

        //set gatewayTable listeners and renderers
        columnModel.getColumn(1).setCellRenderer(new FavoriteGwRenderer());
        columnModel.getColumn(2).setCellRenderer(new GatewayRenderer());
        FavoriteGwListener favoriteGwListener = new FavoriteGwListener();
        gatewayTable.addMouseListener(favoriteGwListener);
        gatewayTable.addKeyListener(favoriteGwListener.keyListener);
        gatewayTable.getSelectionModel().addListSelectionListener(gwSelectionListener);

        //add more listeners
        senderNumberTextField.getDocument().addDocumentListener(new AbstractDocumentListener() {
            @Override
            public void onUpdate(DocumentEvent e) {
                updateSenderNumberWarnLabel();
            }
        });
        DocumentListener keyringListener = new AbstractDocumentListener() {
            @Override
            public void onUpdate(DocumentEvent e) {
                updateKeyring();
            }
        };
        loginField.getDocument().addDocumentListener(keyringListener);
        passwordField.getDocument().addDocumentListener(keyringListener);
        
        //show simple or advanced settings
        advancedCheckBoxActionPerformed(null);

        //store original settings
        if (!originalSettings.containsKey("debugMode")) {
            originalSettings.put("debugMode", config.isDebugMode());
        }
        
        //end of init
        fullyInicialized = true;
        if (gwTableModel.getRowCount() > 0) {
            gatewayTable.getSelectionModel().setSelectionInterval(0, 0);
        }
    }

    public void switchToTab(Tabs tab) {
        Component comp = null;
        switch (tab) {
            case GENERAL:
                comp = generalPanel;
                break;
            case APPEARANCE:
                comp = appearancePanel;
                break;
            case GATEWAYS:
                comp = gatewayPanel;
                break;
            case PRIVACY:
                comp = privacyPanel;
                break;
            case CONNECTION:
                comp = connectionPanel;
                break;
            default:
                logger.warning("Unknown tab: " + tab);
                assert false: "Unknown tab: " + tab;
        }
        tabbedPane.setSelectedIndex(tabbedPane.indexOfComponent(comp));
    }

    /** Show or hide infoPanel according to requests in restartRequests */
    private void updateInfoLabel() {
        boolean wasVisible = restartLabel.isVisible();
        restartLabel.setVisible(!restartRequests.isEmpty());

        if (!fullyInicialized) {
            return;
        }
        if (!wasVisible && restartLabel.isVisible()) {
            invalidate();
            validate();
            if (MiscUtils.isCropped(closeButton)) {
                pack();
            }
        }
    }
    
    /** Update theme according to L&F */
    private void updateThemeComboBox() {
        boolean temp = fullyInicialized;
        fullyInicialized = false;

        themeComboBox.setEnabled(false);
        LAF laf = (LAF) lafComboBox.getSelectedItem();

        if (laf.equals(LAF.JGOODIES)) {
            ArrayList<String> themes = new ArrayList<String>();
            for (Object o : PlasticLookAndFeel.getInstalledThemes()) {
                themes.add(((PlasticTheme) o).getName());
            }
            themeComboBox.setModel(new DefaultComboBoxModel(themes.toArray()));
            themeComboBox.setSelectedItem(config.getLafJGoodiesTheme());
            themeComboBox.setEnabled(true);
        }
        
        else if (laf.equals(LAF.SUBSTANCE)) {
            ArrayList<String> themes = new ArrayList<String>();
            for (SkinInfo skinInfo : SubstanceLookAndFeel.getAllSkins().values()) {
                themes.add(skinInfo.getDisplayName());
            }
            themeComboBox.setModel(new DefaultComboBoxModel(themes.toArray()));
            themeComboBox.setSelectedItem(config.getLafSubstanceSkin());
            themeComboBox.setEnabled(true);
        }

        fullyInicialized = temp;
    }
    
    /** Reaction for gateway key (login, password) change */
    private void updateKeyring() {
        if (!fullyInicialized) {
            return;
        }
        int row = gatewayTable.getSelectedRow();
        Gateway gateway = row >= 0 ? gwTableModel.getGateway(row) : null;
        if (gateway == null) {
            return;
        }
        
        Tuple<String, String> key = new Tuple<String, String>(loginField.getText(),
            new String(passwordField.getPassword()));
        
        if (StringUtils.isEmpty(key.get1()) && StringUtils.isEmpty(key.get2())) {
            //if both empty, remove the key
            keyring.removeKey(gateway.getName());
        } else {
            //else update/set the key
            keyring.putKey(gateway.getName(), key);
        }
    }
    
    /** Reaction to proxy configuration change */
    private void updateProxy() {
        if (!fullyInicialized) {
            return;
        }
        boolean useProxy = useProxyCheckBox.isSelected();

        if (useProxy) {
            ProxyManager.setProxy(httpProxyTextField.getText(),
                    httpsProxyTextField.getText(),
                    socksProxyTextField.getText());
        } else {
            ProxyManager.setProxy(null, null, null);
        }
    }

    /** Update status of unstableUpdate checkbox */
    private void updateUnstableUpdateCheckbox() {
        if (!Config.isStableVersion()) {
            //non-editable in unstable versions
            unstableUpdatesCheckBox.setEnabled(false);
        } else {
            unstableUpdatesCheckBox.setEnabled(checkUpdatesCheckBox.isSelected());
        }
    }

    /** Update visibility of senderNumberWarnLabel */
    private void updateSenderNumberWarnLabel() {
        String number = senderNumberTextField.getText();
        if (StringUtils.isEmpty(number) || Contact.isValidNumber(number)) {
            senderNumberWarnLabel.setVisible(false);
        } else {
            senderNumberWarnLabel.setVisible(true);
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bindingGroup = new BindingGroup();

        config = Config.getInstance();
        tabbedPane = new JTabbedPane();
        generalPanel = new JPanel();
        removeAccentsCheckBox = new JCheckBox();
        checkUpdatesCheckBox = new JCheckBox();
        unstableUpdatesCheckBox = new JCheckBox();
        updatePolicyComboBox = new JComboBox();
        debugCheckBox = new JCheckBox();
        logLocationLabel = new JLabel();
        countryPrefixPanel = new CountryPrefixPanel();
        useSenderIDCheckBox = new JCheckBox();
        jLabel1 = new JLabel();
        senderNumberTextField = new JTextField();
        senderNameTextField = new JTextField();
        demandDeliveryReportCheckBox = new JCheckBox();
        jLabel3 = new JLabel();
        senderNumberWarnLabel = new JLabel();
        appearancePanel = new JPanel();
        lafComboBox = new JComboBox();
        lookLabel = new JLabel();
        themeComboBox = new JComboBox();
        themeLabel = new JLabel();
        windowCenteredCheckBox = new JCheckBox();
        toolbarVisibleCheckBox = new JCheckBox();
        notificationAreaCheckBox = new JCheckBox();
        tipsCheckBox = new JCheckBox();
        startMinimizedCheckBox = new JCheckBox();
        advancedControlsCheckBox = new JCheckBox();
        gatewayPanel = new JPanel();
        clearKeyringButton = new JButton();
        jScrollPane1 = new JScrollPane();
        gatewayTable = new JTable() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int row = getSelectedRow();
                int column = getSelectedColumn();
                super.tableChanged(e);
                changeSelection(row, column, false, false);
            }
        };
        gwTipLabel = new InfoLabel(Type.TIP);
        gwDetailsPanel = new JPanel();
        passwordField = new JPasswordField();
        jLabel12 = new JLabel();
        loginField = new JTextField();
        showPasswordCheckBox = new JCheckBox();
        jLabel11 = new JLabel();
        privacyPanel = new JPanel();
        reducedHistoryCheckBox = new JCheckBox();
        reducedHistorySpinner = new JSpinner();
        jLabel18 = new JLabel();
        connectionPanel = new JPanel();
        useProxyCheckBox = new JCheckBox();
        httpProxyTextField = new JTextField();
        sameProxyCheckBox = new JCheckBox();
        httpsProxyTextField = new JTextField();
        socksProxyTextField = new JTextField();
        jLabel14 = new JLabel();
        jLabel15 = new JLabel();
        jLabel16 = new JLabel();
        jLabel17 = new JLabel();
        closeButton = new JButton();
        advancedCheckBox = new JCheckBox();
        restartLabel = new InfoLabel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(l10n.getString("ConfigFrame.title")); // NOI18N
        addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent evt) {
                formWindowGainedFocus(evt);
            }
            public void windowLostFocus(WindowEvent evt) {
            }
        });
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        Mnemonics.setLocalizedText(removeAccentsCheckBox, l10n.getString("ConfigFrame.removeAccentsCheckBox.text"));
        removeAccentsCheckBox.setToolTipText(l10n.getString("ConfigFrame.removeAccentsCheckBox.toolTipText")); // NOI18N
        Binding binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${removeAccents}"), removeAccentsCheckBox, BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        Mnemonics.setLocalizedText(checkUpdatesCheckBox, l10n.getString("ConfigFrame.checkUpdatesCheckBox.text"));
        checkUpdatesCheckBox.setToolTipText(l10n.getString("ConfigFrame.checkUpdatesCheckBox.toolTipText")); // NOI18N
        binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${checkUpdatePolicy}"), checkUpdatesCheckBox, BeanProperty.create("selected"));
        binding.setConverter(new CheckUpdateConverter());
        bindingGroup.addBinding(binding);


        checkUpdatesCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                checkUpdatesCheckBoxActionPerformed(evt);
            }
        });
        Mnemonics.setLocalizedText(unstableUpdatesCheckBox, l10n.getString("ConfigFrame.unstableUpdatesCheckBox.text"));
        unstableUpdatesCheckBox.setToolTipText(l10n.getString("ConfigFrame.unstableUpdatesCheckBox.toolTipText")); // NOI18N
        binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${checkForUnstableUpdates}"), unstableUpdatesCheckBox, BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        updatePolicyComboBox.setModel(new DefaultComboBoxModel(new CheckUpdatePolicy[]{CheckUpdatePolicy.CHECK_PROGRAM, CheckUpdatePolicy.CHECK_GATEWAYS, CheckUpdatePolicy.CHECK_ALL}));
        updatePolicyComboBox.setToolTipText(checkUpdatesCheckBox.getToolTipText());
        updatePolicyComboBox.setRenderer(new UpdatePolicyComboRenderer());

        binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${checkUpdatePolicy}"), updatePolicyComboBox, BeanProperty.create("selectedItem"));
        binding.setConverter(new UpdatePolicyConverter());
        bindingGroup.addBinding(binding);
        binding = Bindings.createAutoBinding(UpdateStrategy.READ, checkUpdatesCheckBox, ELProperty.create("${selected}"), updatePolicyComboBox, BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        Mnemonics.setLocalizedText(debugCheckBox, l10n.getString("ConfigFrame.debugCheckBox.text"));
        debugCheckBox.setToolTipText(l10n.getString("ConfigFrame.debugCheckBox.toolTipText")); // NOI18N
        binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${debugMode}"), debugCheckBox, BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        debugCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                debugCheckBoxItemStateChanged(evt);
            }
        });

        Mnemonics.setLocalizedText(logLocationLabel, l10n.getString("ConfigFrame.logLocationLabel.text"));
        logLocationLabel.setText(MessageFormat.format(logLocationLabel.getText(),
            Context.persistenceManager.getLogFile()));

    binding = Bindings.createAutoBinding(UpdateStrategy.READ, debugCheckBox,
        BeanProperty.create("selected"), logLocationLabel, BeanProperty.create("visible"));
    bindingGroup.addBinding(binding);

        Mnemonics.setLocalizedText(useSenderIDCheckBox, l10n.getString("ConfigFrame.useSenderIDCheckBox.text"));
    useSenderIDCheckBox.setToolTipText(l10n.getString("ConfigFrame.useSenderIDCheckBox.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${useSenderID}"), useSenderIDCheckBox, BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);

    jLabel1.setLabelFor(senderNumberTextField);
        Mnemonics.setLocalizedText(jLabel1, l10n.getString("ConfigFrame.jLabel1.text")); // NOI18N
    jLabel1.setToolTipText(senderNumberTextField.getToolTipText());

    senderNumberTextField.setColumns(13);

    senderNumberTextField.setToolTipText(l10n.getString("ConfigFrame.senderNumberTextField.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${senderNumber}"), senderNumberTextField, BeanProperty.create("text"));
    bindingGroup.addBinding(binding);
    binding = Bindings.createAutoBinding(UpdateStrategy.READ, useSenderIDCheckBox, ELProperty.create("${selected}"), senderNumberTextField, BeanProperty.create("enabled"));
    bindingGroup.addBinding(binding);

    senderNameTextField.setColumns(13);

    senderNameTextField.setToolTipText(l10n.getString("ConfigFrame.senderNameTextField.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${senderName}"), senderNameTextField, BeanProperty.create("text_ON_ACTION_OR_FOCUS_LOST"));
    bindingGroup.addBinding(binding);
    binding = Bindings.createAutoBinding(UpdateStrategy.READ, useSenderIDCheckBox, ELProperty.create("${selected}"), senderNameTextField, BeanProperty.create("enabled"));
    bindingGroup.addBinding(binding);

        Mnemonics.setLocalizedText(demandDeliveryReportCheckBox, l10n.getString("ConfigFrame.demandDeliveryReportCheckBox.text"));
    demandDeliveryReportCheckBox.setToolTipText(l10n.getString("ConfigFrame.demandDeliveryReportCheckBox.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${demandDeliveryReport}"), demandDeliveryReportCheckBox, BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, useSenderIDCheckBox, ELProperty.create("${selected}"), demandDeliveryReportCheckBox, BeanProperty.create("enabled"));
    bindingGroup.addBinding(binding);

    jLabel3.setLabelFor(senderNameTextField);
        Mnemonics.setLocalizedText(jLabel3, l10n.getString("ConfigFrame.jLabel3.text")); // NOI18N
    jLabel3.setToolTipText(senderNameTextField.getToolTipText());

    senderNumberWarnLabel.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/warning-16.png"))); // NOI18N
    senderNumberWarnLabel.setToolTipText(senderNumberTextField.getToolTipText());
    senderNumberWarnLabel.setVisible(false);

        GroupLayout generalPanelLayout = new GroupLayout(generalPanel);
    generalPanel.setLayout(generalPanelLayout);
    generalPanelLayout.setHorizontalGroup(
        generalPanelLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(generalPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(generalPanelLayout.createParallelGroup(Alignment.LEADING)
                .addComponent(countryPrefixPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(useSenderIDCheckBox)
                .addGroup(generalPanelLayout.createSequentialGroup()
                    .addGap(17, 17, 17)
                    .addGroup(generalPanelLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(generalPanelLayout.createSequentialGroup()
                            .addGroup(generalPanelLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(jLabel3)
                                .addComponent(jLabel1))
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(generalPanelLayout.createParallelGroup(Alignment.LEADING)
                                .addGroup(generalPanelLayout.createSequentialGroup()
                                    .addComponent(senderNumberTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(senderNumberWarnLabel))
                                .addComponent(senderNameTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                        .addComponent(demandDeliveryReportCheckBox)))
                .addGroup(generalPanelLayout.createSequentialGroup()
                    .addGap(22, 22, 22)
                    .addComponent(logLocationLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addComponent(removeAccentsCheckBox)
                .addGroup(generalPanelLayout.createSequentialGroup()
                    .addComponent(checkUpdatesCheckBox)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(generalPanelLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(unstableUpdatesCheckBox)
                        .addComponent(updatePolicyComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                .addComponent(debugCheckBox))
            .addContainerGap(322, Short.MAX_VALUE))
    );

    generalPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {jLabel1, jLabel3});

    generalPanelLayout.setVerticalGroup(
        generalPanelLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(generalPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(countryPrefixPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(useSenderIDCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(generalPanelLayout.createParallelGroup(Alignment.LEADING, false)
                .addComponent(senderNumberWarnLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(senderNumberTextField)
                .addComponent(jLabel1))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(generalPanelLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(jLabel3)
                .addComponent(senderNameTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(demandDeliveryReportCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(removeAccentsCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(generalPanelLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(checkUpdatesCheckBox)
                .addComponent(updatePolicyComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(unstableUpdatesCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(debugCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(logLocationLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addContainerGap(50, Short.MAX_VALUE))
    );

    tabbedPane.addTab(l10n.getString("ConfigFrame.generalPanel.TabConstraints.tabTitle"), new ImageIcon(getClass().getResource("/esmska/resources/config-16.png")), generalPanel); // NOI18N

    lafComboBox.setModel(lafModel);
    lafComboBox.setToolTipText(l10n.getString("ConfigFrame.lafComboBox.toolTipText")); // NOI18N
    lafComboBox.setRenderer(new LaFComboRenderer());
    lafComboBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            lafComboBoxActionPerformed(evt);
        }
    });

    lookLabel.setLabelFor(lafComboBox);
        Mnemonics.setLocalizedText(lookLabel, l10n.getString("ConfigFrame.lookLabel.text")); // NOI18N
    lookLabel.setToolTipText(lafComboBox.getToolTipText());

    themeComboBox.setToolTipText(l10n.getString("ConfigFrame.themeComboBox.toolTipText")); // NOI18N
    themeComboBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            themeComboBoxActionPerformed(evt);
        }
    });

    themeLabel.setLabelFor(themeComboBox);
        Mnemonics.setLocalizedText(themeLabel, l10n.getString("ConfigFrame.themeLabel.text")); // NOI18N
    themeLabel.setToolTipText(themeComboBox.getToolTipText());

        Mnemonics.setLocalizedText(windowCenteredCheckBox, l10n.getString("ConfigFrame.windowCenteredCheckBox.text"));
    windowCenteredCheckBox.setToolTipText(l10n.getString("ConfigFrame.windowCenteredCheckBox.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${startCentered}"), windowCenteredCheckBox, BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);

        Mnemonics.setLocalizedText(toolbarVisibleCheckBox, l10n.getString("ConfigFrame.toolbarVisibleCheckBox.text"));
    toolbarVisibleCheckBox.setToolTipText(l10n.getString("ConfigFrame.toolbarVisibleCheckBox.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${toolbarVisible}"), toolbarVisibleCheckBox, BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);

        Mnemonics.setLocalizedText(notificationAreaCheckBox, l10n.getString("ConfigFrame.notificationAreaCheckBox.text"));
    notificationAreaCheckBox.setToolTipText(l10n.getString("ConfigFrame.notificationAreaCheckBox.toolTipText")); // NOI18N
    notificationAreaCheckBox.setEnabled(NotificationIcon.isSupported());

    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${notificationIconVisible}"), notificationAreaCheckBox, BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);


    notificationAreaCheckBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            notificationAreaCheckBoxActionPerformed(evt);
        }
    });
        Mnemonics.setLocalizedText(tipsCheckBox, l10n.getString("ConfigFrame.tipsCheckBox.text"));
    tipsCheckBox.setToolTipText(l10n.getString("ConfigFrame.tipsCheckBox.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${showTips}"), tipsCheckBox, BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);

        Mnemonics.setLocalizedText(startMinimizedCheckBox, l10n.getString("ConfigFrame.startMinimizedCheckBox.text"));
    startMinimizedCheckBox.setToolTipText(l10n.getString("ConfigFrame.startMinimizedCheckBox.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${startMinimized}"), startMinimizedCheckBox, BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, notificationAreaCheckBox, ELProperty.create("${selected && enabled}"), startMinimizedCheckBox, BeanProperty.create("enabled"));
    bindingGroup.addBinding(binding);

        Mnemonics.setLocalizedText(advancedControlsCheckBox, l10n.getString("ConfigFrame.advancedControlsCheckBox.text"));
    advancedControlsCheckBox.setToolTipText(l10n.getString("ConfigFrame.advancedControlsCheckBox.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${showAdvancedControls}"), advancedControlsCheckBox, BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);

        GroupLayout appearancePanelLayout = new GroupLayout(appearancePanel);
    appearancePanel.setLayout(appearancePanelLayout);

    appearancePanelLayout.setHorizontalGroup(
        appearancePanelLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(appearancePanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(appearancePanelLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(appearancePanelLayout.createSequentialGroup()
                    .addComponent(lookLabel)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(lafComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addGroup(appearancePanelLayout.createSequentialGroup()
                    .addComponent(themeLabel)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(themeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addComponent(windowCenteredCheckBox)
                .addGroup(appearancePanelLayout.createSequentialGroup()
                    .addGap(21, 21, 21)
                    .addComponent(startMinimizedCheckBox))
                .addComponent(tipsCheckBox)
                .addComponent(notificationAreaCheckBox)
                .addComponent(toolbarVisibleCheckBox)
                .addComponent(advancedControlsCheckBox))
            .addContainerGap(471, Short.MAX_VALUE))
    );

    appearancePanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {lafComboBox, themeComboBox});

    appearancePanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {lookLabel, themeLabel});

    appearancePanelLayout.setVerticalGroup(
        appearancePanelLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(appearancePanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(appearancePanelLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(lookLabel)
                .addComponent(lafComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(appearancePanelLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(themeLabel)
                .addComponent(themeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(windowCenteredCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(toolbarVisibleCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(notificationAreaCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(startMinimizedCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(tipsCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(advancedControlsCheckBox)
            .addContainerGap(115, Short.MAX_VALUE))
    );

    appearancePanelLayout.linkSize(SwingConstants.VERTICAL, new Component[] {lafComboBox, themeComboBox});

    tabbedPane.addTab(l10n.getString("ConfigFrame.appearancePanel.TabConstraints.tabTitle"), new ImageIcon(getClass().getResource("/esmska/resources/appearance-16.png")), appearancePanel); // NOI18N
    clearKeyringButton.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/clear-22.png"))); // NOI18N
    Mnemonics.setLocalizedText(clearKeyringButton, l10n.getString("ConfigFrame.clearKeyringButton.text"));
    clearKeyringButton.setToolTipText(l10n.getString("ConfigFrame.clearKeyringButton.toolTipText")); // NOI18N
    clearKeyringButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            clearKeyringButtonActionPerformed(evt);
        }
    });

    gatewayTable.setModel(gwTableModel);
    gatewayTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    gatewayTable.getTableHeader().setReorderingAllowed(false);
    jScrollPane1.setViewportView(gatewayTable);

    Mnemonics.setLocalizedText(gwTipLabel,l10n.getString("ConfigFrame.gwTipLabel.text")); // NOI18N
    gwDetailsPanel.setBorder(BorderFactory.createTitledBorder(l10n.getString("ConfigFrame.gwDetailsPanel.border.title"))); // NOI18N

    passwordField.setColumns(12);
    passwordField.setToolTipText(l10n.getString("ConfigFrame.passwordField.toolTipText")); // NOI18N
    passwordField.enableInputMethods(true);

    jLabel12.setLabelFor(passwordField);
        Mnemonics.setLocalizedText(jLabel12, l10n.getString("ConfigFrame.jLabel12.text")); // NOI18N
    jLabel12.setToolTipText(passwordField.getToolTipText());

    loginField.setColumns(12);

    loginField.setToolTipText(l10n.getString("ConfigFrame.loginField.toolTipText")); // NOI18N
    Mnemonics.setLocalizedText(showPasswordCheckBox, l10n.getString("ConfigFrame.showPasswordCheckBox.text"));
    showPasswordCheckBox.setToolTipText(l10n.getString("ConfigFrame.showPasswordCheckBox.toolTipText")); // NOI18N
    showPasswordCheckBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            showPasswordCheckBoxActionPerformed(evt);
        }
    });

    jLabel11.setLabelFor(loginField);
        Mnemonics.setLocalizedText(jLabel11, l10n.getString("ConfigFrame.jLabel11.text")); // NOI18N
    jLabel11.setToolTipText(loginField.getToolTipText());

        GroupLayout gwDetailsPanelLayout = new GroupLayout(gwDetailsPanel);
    gwDetailsPanel.setLayout(gwDetailsPanelLayout);
    gwDetailsPanelLayout.setHorizontalGroup(
        gwDetailsPanelLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(gwDetailsPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(gwDetailsPanelLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(gwDetailsPanelLayout.createSequentialGroup()
                    .addComponent(jLabel11)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(loginField))
                .addGroup(gwDetailsPanelLayout.createSequentialGroup()
                    .addComponent(jLabel12)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gwDetailsPanelLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(gwDetailsPanelLayout.createSequentialGroup()
                            .addComponent(showPasswordCheckBox)
                            .addPreferredGap(ComponentPlacement.RELATED, 25, Short.MAX_VALUE))
                        .addComponent(passwordField))))
            .addGap(0, 0, 0))
    );

    gwDetailsPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {jLabel11, jLabel12});

    gwDetailsPanelLayout.setVerticalGroup(
        gwDetailsPanelLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(gwDetailsPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(gwDetailsPanelLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(jLabel11)
                .addComponent(loginField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(gwDetailsPanelLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(jLabel12)
                .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(showPasswordCheckBox)
            .addContainerGap())
    );

        GroupLayout gatewayPanelLayout = new GroupLayout(gatewayPanel);
    gatewayPanel.setLayout(gatewayPanelLayout);


    gatewayPanelLayout.setHorizontalGroup(
        gatewayPanelLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(gatewayPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(gatewayPanelLayout.createParallelGroup(Alignment.LEADING)
                .addComponent(gwTipLabel, GroupLayout.DEFAULT_SIZE, 734, Short.MAX_VALUE)
                .addGroup(Alignment.TRAILING, gatewayPanelLayout.createSequentialGroup()
                    .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 476, Short.MAX_VALUE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gatewayPanelLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(clearKeyringButton)
                        .addComponent(gwDetailsPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))))
            .addGap(0, 0, 0))
    );
    gatewayPanelLayout.setVerticalGroup(
        gatewayPanelLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(gatewayPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(gatewayPanelLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(gatewayPanelLayout.createSequentialGroup()
                    .addComponent(gwDetailsPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED, 98, Short.MAX_VALUE)
                    .addComponent(clearKeyringButton))
                .addComponent(jScrollPane1, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE))
            .addPreferredGap(ComponentPlacement.UNRELATED)
            .addComponent(gwTipLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addContainerGap())
    );

    tabbedPane.addTab(l10n.getString("ConfigFrame.gatewayPanel.TabConstraints.tabTitle"), new ImageIcon(getClass().getResource("/esmska/resources/gateway-16.png")), gatewayPanel); // NOI18N
        Mnemonics.setLocalizedText(reducedHistoryCheckBox, l10n.getString("ConfigFrame.reducedHistoryCheckBox.text"));
    reducedHistoryCheckBox.setToolTipText(l10n.getString("ConfigFrame.reducedHistoryCheckBox.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${reducedHistory}"), reducedHistoryCheckBox, BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);

    reducedHistoryCheckBox.setText(reducedHistoryCheckBox.getText().replaceFirst("\\{0\\}.*$", "").trim());

    reducedHistorySpinner.setToolTipText(reducedHistoryCheckBox.getToolTipText());

    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${reducedHistoryCount}"), reducedHistorySpinner, BeanProperty.create("value"));
    bindingGroup.addBinding(binding);
    binding = Bindings.createAutoBinding(UpdateStrategy.READ, reducedHistoryCheckBox, ELProperty.create("${selected}"), reducedHistorySpinner, BeanProperty.create("enabled"));
    bindingGroup.addBinding(binding);

        ((SpinnerNumberModel) reducedHistorySpinner.getModel()).setMinimum(new Integer(0)); // NOI18N
        Mnemonics.setLocalizedText(jLabel18, "<<days.>>");
    jLabel18.setToolTipText(reducedHistoryCheckBox.getToolTipText());
    jLabel18.setText(l10n.getString("ConfigFrame.reducedHistoryCheckBox.text").replaceFirst("^.*\\{0\\}", "").trim());

        GroupLayout privacyPanelLayout = new GroupLayout(privacyPanel);
    privacyPanel.setLayout(privacyPanelLayout);


    privacyPanelLayout.setHorizontalGroup(
        privacyPanelLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(privacyPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(reducedHistoryCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(reducedHistorySpinner, GroupLayout.PREFERRED_SIZE, 65, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(jLabel18)
            .addContainerGap(212, Short.MAX_VALUE))
    );
    privacyPanelLayout.setVerticalGroup(
        privacyPanelLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(privacyPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(privacyPanelLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(reducedHistoryCheckBox)
                .addComponent(reducedHistorySpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel18))
            .addContainerGap())
    );

    tabbedPane.addTab(l10n.getString("ConfigFrame.privacyPanel.TabConstraints.tabTitle"), new ImageIcon(getClass().getResource("/esmska/resources/lock-16.png")), privacyPanel); // NOI18N
        Mnemonics.setLocalizedText(useProxyCheckBox, l10n.getString("ConfigFrame.useProxyCheckBox.text"));
    useProxyCheckBox.setToolTipText(l10n.getString("ConfigFrame.useProxyCheckBox.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${useProxy}"), useProxyCheckBox, BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);

    useProxyCheckBox.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent evt) {
            useProxyCheckBoxItemStateChanged(evt);
        }
    });

    httpProxyTextField.setColumns(20);

    httpProxyTextField.setToolTipText(l10n.getString("ConfigFrame.httpProxyTextField.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${httpProxy}"), httpProxyTextField, BeanProperty.create("text"));
    bindingGroup.addBinding(binding);
    binding = Bindings.createAutoBinding(UpdateStrategy.READ, useProxyCheckBox, ELProperty.create("${selected}"), httpProxyTextField, BeanProperty.create("enabled"));
    bindingGroup.addBinding(binding);


    httpProxyTextField.getDocument().addDocumentListener(new AbstractDocumentListener() {
        @Override
        public void onUpdate(DocumentEvent e) {
            if (sameProxyCheckBox.isSelected()) {
                httpsProxyTextField.setText(httpProxyTextField.getText());
            }
            updateProxy();
        }
    });
        Mnemonics.setLocalizedText(sameProxyCheckBox, l10n.getString("ConfigFrame.sameProxyCheckBox.text"));
    sameProxyCheckBox.setToolTipText(l10n.getString("ConfigFrame.sameProxyCheckBox.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${sameProxy}"), sameProxyCheckBox, BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);
    binding = Bindings.createAutoBinding(UpdateStrategy.READ, useProxyCheckBox, ELProperty.create("${selected}"), sameProxyCheckBox, BeanProperty.create("enabled"));
    bindingGroup.addBinding(binding);


    sameProxyCheckBox.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent evt) {
            sameProxyCheckBoxItemStateChanged(evt);
        }
    });

    httpsProxyTextField.setToolTipText(l10n.getString("ConfigFrame.httpsProxyTextField.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${httpsProxy}"), httpsProxyTextField, BeanProperty.create("text"));
    bindingGroup.addBinding(binding);
    binding = Bindings.createAutoBinding(UpdateStrategy.READ, sameProxyCheckBox, ELProperty.create("${enabled && !selected}"), httpsProxyTextField, BeanProperty.create("enabled"));
    bindingGroup.addBinding(binding);


    httpsProxyTextField.getDocument().addDocumentListener(new AbstractDocumentListener() {
        @Override
        public void onUpdate(DocumentEvent e) {
            updateProxy();
        }
    });

    socksProxyTextField.setToolTipText(l10n.getString("ConfigFrame.socksProxyTextField.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${socksProxy}"), socksProxyTextField, BeanProperty.create("text"));
    bindingGroup.addBinding(binding);
    binding = Bindings.createAutoBinding(UpdateStrategy.READ, sameProxyCheckBox, ELProperty.create("${enabled}"), socksProxyTextField, BeanProperty.create("enabled"));
    bindingGroup.addBinding(binding);

    socksProxyTextField.getDocument().addDocumentListener(new AbstractDocumentListener() {
        @Override
        public void onUpdate(DocumentEvent e) {
            updateProxy();
        }
    });

    jLabel14.setLabelFor(httpProxyTextField);
        Mnemonics.setLocalizedText(jLabel14, l10n.getString("ConfigFrame.jLabel14.text")); // NOI18N
    jLabel14.setToolTipText(httpProxyTextField.getToolTipText());

    jLabel15.setLabelFor(httpsProxyTextField);
        Mnemonics.setLocalizedText(jLabel15, l10n.getString("ConfigFrame.jLabel15.text")); // NOI18N
    jLabel15.setToolTipText(httpsProxyTextField.getToolTipText());

    jLabel16.setLabelFor(socksProxyTextField);
        Mnemonics.setLocalizedText(jLabel16, l10n.getString("ConfigFrame.jLabel16.text")); // NOI18N
    jLabel16.setToolTipText(socksProxyTextField.getToolTipText());

        Mnemonics.setLocalizedText(jLabel17, l10n.getString("ConfigFrame.jLabel17.text"));
        GroupLayout connectionPanelLayout = new GroupLayout(connectionPanel);
    connectionPanel.setLayout(connectionPanelLayout);


    connectionPanelLayout.setHorizontalGroup(
        connectionPanelLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(connectionPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(connectionPanelLayout.createParallelGroup(Alignment.LEADING)
                .addComponent(useProxyCheckBox)
                .addGroup(connectionPanelLayout.createSequentialGroup()
                    .addGap(21, 21, 21)
                    .addGroup(connectionPanelLayout.createParallelGroup(Alignment.LEADING, false)
                        .addGroup(connectionPanelLayout.createSequentialGroup()
                            .addComponent(jLabel14)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(httpProxyTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addGroup(connectionPanelLayout.createSequentialGroup()
                            .addComponent(jLabel16)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(socksProxyTextField))
                        .addGroup(Alignment.TRAILING, connectionPanelLayout.createSequentialGroup()
                            .addComponent(jLabel15)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(httpsProxyTextField))
                        .addGroup(connectionPanelLayout.createSequentialGroup()
                            .addGap(6, 6, 6)
                            .addComponent(sameProxyCheckBox))))
                .addComponent(jLabel17, GroupLayout.DEFAULT_SIZE, 728, Short.MAX_VALUE))
            .addContainerGap())
    );

    connectionPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {jLabel14, jLabel15, jLabel16});

    connectionPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {httpProxyTextField, httpsProxyTextField, socksProxyTextField});

    connectionPanelLayout.setVerticalGroup(
        connectionPanelLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(connectionPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(useProxyCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(connectionPanelLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(jLabel14)
                .addComponent(httpProxyTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(connectionPanelLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(jLabel15)
                .addComponent(httpsProxyTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(sameProxyCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(connectionPanelLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(jLabel16)
                .addComponent(socksProxyTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED, 153, Short.MAX_VALUE)
            .addComponent(jLabel17, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addContainerGap())
    );

    connectionPanelLayout.linkSize(SwingConstants.VERTICAL, new Component[] {httpProxyTextField, httpsProxyTextField, socksProxyTextField});

    tabbedPane.addTab(l10n.getString("ConfigFrame.connectionPanel.TabConstraints.tabTitle"), new ImageIcon(getClass().getResource("/esmska/resources/connection-16.png")), connectionPanel); // NOI18N
    closeButton.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/close-22.png"))); // NOI18N
        Mnemonics.setLocalizedText(closeButton, l10n.getString("Close_"));
    closeButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            closeButtonActionPerformed(evt);
        }
    });
        Mnemonics.setLocalizedText(advancedCheckBox, l10n.getString("ConfigFrame.advancedCheckBox.text"));
    advancedCheckBox.setToolTipText(l10n.getString("ConfigFrame.advancedCheckBox.toolTipText")); // NOI18N
    binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, config, ELProperty.create("${showAdvancedSettings}"), advancedCheckBox, BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);

    advancedCheckBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            advancedCheckBoxActionPerformed(evt);
        }
    });

        Mnemonics.setLocalizedText(restartLabel, l10n.getString("ConfigFrame.restartLabel.text")); // NOI18N
    restartLabel.setVisible(false);

        GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(tabbedPane, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(advancedCheckBox)
                    .addPreferredGap(ComponentPlacement.RELATED, 514, Short.MAX_VALUE)
                    .addComponent(closeButton))
                .addComponent(restartLabel, GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE))
            .addContainerGap())
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(Alignment.LEADING)
        .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(restartLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(closeButton)
                .addComponent(advancedCheckBox))
            .addContainerGap())
    );

    bindingGroup.bind();

    pack();
    }// </editor-fold>//GEN-END:initComponents
        
    private void themeComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_themeComboBoxActionPerformed
        LAF laf = (LAF) lafComboBox.getSelectedItem();

        if (laf.equals(LAF.JGOODIES)) {
            config.setLafJGoodiesTheme((String) themeComboBox.getSelectedItem());
        } else if (laf.equals(LAF.SUBSTANCE)) {
            config.setLafSubstanceSkin((String) themeComboBox.getSelectedItem());
        }

        //update skin in realtime
        if (fullyInicialized && ThemeManager.getActiveLaF() == lafComboBox.getSelectedItem()) {
            logger.fine("Changing LaF theme in realtime...");
            try {
                ThemeManager.setLaF();
                SwingUtilities.updateComponentTreeUI(Context.mainFrame);
                SwingUtilities.updateComponentTreeUI(this);
            } catch (Throwable ex) {
                logger.log(Level.SEVERE, "Problem while live-updating the look&feel skin", ex);
            }
        }
    }//GEN-LAST:event_themeComboBoxActionPerformed
    
    private void lafComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_lafComboBoxActionPerformed
        if (!fullyInicialized) {
            return;
        }
        LAF laf = (LAF) lafComboBox.getSelectedItem();
        config.setLookAndFeel(laf);
        updateThemeComboBox();

        // show warning if restart required
        if (laf != ThemeManager.getActiveLaF()) {
            restartRequests.add("lafComboBox");
        } else {
            restartRequests.remove("lafComboBox");
        }
        updateInfoLabel();
    }//GEN-LAST:event_lafComboBoxActionPerformed
                            
    private void closeButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        formWindowClosing(null);
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_closeButtonActionPerformed

    private void clearKeyringButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_clearKeyringButtonActionPerformed
        String deleteOption = l10n.getString("Delete");
        String cancelOption = l10n.getString("Cancel");
        Object[] options = RuntimeUtils.sortDialogOptions(cancelOption, deleteOption);
        String message = l10n.getString("ConfigFrame.remove_credentials");

        //show dialog
        JOptionPane pane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, options, cancelOption);
        JDialog dialog = pane.createDialog(ConfigFrame.this, null);
        dialog.setResizable(true);
        RuntimeUtils.setDocumentModalDialog(dialog);
        dialog.pack();
        dialog.setVisible(true);

        //return if should not delete
        if (!deleteOption.equals(pane.getValue())) {
            return;
        }
        
        keyring.clearKeys();
        gwSelectionListener.valueChanged(new ListSelectionEvent(gatewayTable, 0, 0, false));
    }//GEN-LAST:event_clearKeyringButtonActionPerformed

    private void useProxyCheckBoxItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_useProxyCheckBoxItemStateChanged
        updateProxy();
    }//GEN-LAST:event_useProxyCheckBoxItemStateChanged

    private void sameProxyCheckBoxItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_sameProxyCheckBoxItemStateChanged
        updateProxy();
    }//GEN-LAST:event_sameProxyCheckBoxItemStateChanged

private void notificationAreaCheckBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_notificationAreaCheckBoxActionPerformed
        if (notificationAreaCheckBox.isSelected()) {
            NotificationIcon.install();
        } else {
            NotificationIcon.uninstall();
        }
}//GEN-LAST:event_notificationAreaCheckBoxActionPerformed

private void advancedCheckBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_advancedCheckBoxActionPerformed
    boolean showAdvanced = advancedCheckBox.isSelected();

    checkUpdatesCheckBox.setVisible(showAdvanced);
    updatePolicyComboBox.setVisible(showAdvanced);
    unstableUpdatesCheckBox.setVisible(showAdvanced);
    windowCenteredCheckBox.setVisible(showAdvanced);
    startMinimizedCheckBox.setVisible(showAdvanced);
    tipsCheckBox.setVisible(showAdvanced);
    debugCheckBox.setVisible(showAdvanced);
    advancedControlsCheckBox.setVisible(showAdvanced);
    toolbarVisibleCheckBox.setVisible(showAdvanced);
    
    tabbedPane.setEnabledAt(tabbedPane.indexOfComponent(privacyPanel), showAdvanced);
    tabbedPane.setEnabledAt(tabbedPane.indexOfComponent(connectionPanel), showAdvanced);

    invalidate();
    validate();
}//GEN-LAST:event_advancedCheckBoxActionPerformed

private void showPasswordCheckBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_showPasswordCheckBoxActionPerformed
    if (showPasswordCheckBox.isSelected()) {
        //set password to be displayed
        passwordEchoChar = passwordField.getEchoChar();
        passwordField.setEchoChar((char) 0);
    } else {
        //set password to be hidden
        passwordField.setEchoChar((Character) ObjectUtils.defaultIfNull(passwordEchoChar, '*'));
    }
}//GEN-LAST:event_showPasswordCheckBoxActionPerformed

private void checkUpdatesCheckBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_checkUpdatesCheckBoxActionPerformed
    updateUnstableUpdateCheckbox();
}//GEN-LAST:event_checkUpdatesCheckBoxActionPerformed

private void debugCheckBoxItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_debugCheckBoxItemStateChanged
    if (!fullyInicialized) {
        return;
    }
    // show warning if restart required
    if (debugCheckBox.isSelected() != (Boolean) originalSettings.get("debugMode")) {
        restartRequests.add("debugCheckBox");
    } else {
        restartRequests.remove("debugCheckBox");
    }
    updateInfoLabel();
}//GEN-LAST:event_debugCheckBoxItemStateChanged

private void formWindowClosing(WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
    //check validity of country prefix
    String prefix = countryPrefixPanel.getCountryPrefix();
    if (prefix != null) {
        config.setCountryPrefix(prefix);
    }
    countryPrefixPanel.setCountryPrefix(config.getCountryPrefix());
    
    //save config
    try {
        Context.persistenceManager.saveConfig();
    } catch (IOException ex) {
        logger.log(Level.WARNING, "Could not save config", ex);
    }
}//GEN-LAST:event_formWindowClosing

private void formWindowGainedFocus(WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
    closeButton.requestFocusInWindow();
}//GEN-LAST:event_formWindowGainedFocus
    
    private class LaFComboRenderer extends DefaultListCellRenderer {
        private final ListCellRenderer lafRenderer = new JList().getCellRenderer();
        private final String LAF_SYSTEM = l10n.getString("ConfigFrame.system_look");
        private final String LAF_CROSSPLATFORM = l10n.getString("ConfigFrame.multiplatform_look");
        private final String LAF_GTK = "GTK";
        private final String LAF_JGOODIES = "JGoodies";
        private final String LAF_SUBSTANCE = "Substance";
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) lafRenderer.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            
            if (!(value instanceof LAF)) {
                return label;
            }
            
            LAF laf = (LAF) value;
            String name = l10n.getString("ConfigFrame.unknown_look");
            switch (laf) {
                case SYSTEM: 
                    name = LAF_SYSTEM;
                    break;
                case CROSSPLATFORM:
                    name = LAF_CROSSPLATFORM; 
                    break;
                case GTK:
                    name = LAF_GTK;
                    break;
                case JGOODIES: 
                    name = LAF_JGOODIES;
                    break;
                case SUBSTANCE: 
                    name = LAF_SUBSTANCE;
                    break;
                default: 
                    logger.severe("Unknown LaF: " + laf);
                    break;
            }
            label.setText(name);
            
            return label;
        }
    }

    /** Renderer for updatePolicyComboBox */
    private class UpdatePolicyComboRenderer extends DefaultListCellRenderer {
        private final ListCellRenderer updateRenderer = new JList().getCellRenderer();
        private final String CHECK_PROGRAM = l10n.getString("ConfigFrame.updateCheckProgram");
        private final String CHECK_GW = l10n.getString("ConfigFrame.updateCheckGateway");
        private final String CHECK_ALL = l10n.getString("ConfigFrame.updateCheckAll");

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) updateRenderer.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            if (!(value instanceof CheckUpdatePolicy)) {
                return label;
            }

            CheckUpdatePolicy policy = (CheckUpdatePolicy) value;
            String name = null;
            switch (policy) {
                case CHECK_PROGRAM:
                    name = CHECK_PROGRAM;
                    break;
                case CHECK_GATEWAYS:
                    name = CHECK_GW;
                    break;
                case CHECK_ALL:
                    name = CHECK_ALL;
                    break;
                case CHECK_NONE:
                default:
                    assert false : "Unknown or forbidden update policy: " + policy;
                    logger.severe("Unknown or forbidden update policy: " + policy);
                    break;
            }
            label.setText(name);

            return label;
        }
    }

    /** Converter for updatePolicyComboBox */
    private class UpdatePolicyConverter extends Converter<CheckUpdatePolicy, CheckUpdatePolicy> {
        @Override
        public CheckUpdatePolicy convertForward(CheckUpdatePolicy value) {
            if (value == CheckUpdatePolicy.CHECK_NONE) {
                return null;
            }
            return value;
        }

        @Override
        public CheckUpdatePolicy convertReverse(CheckUpdatePolicy value) {
            if (value == null) {
                return CheckUpdatePolicy.CHECK_NONE;
            }
            return value;
        }
    }

    /** Converter for checkUpdatesCheckBox */
    private class CheckUpdateConverter extends Converter<CheckUpdatePolicy, Boolean> {
        @Override
        public Boolean convertForward(CheckUpdatePolicy value) {
            return value != CheckUpdatePolicy.CHECK_NONE;
        }

        @Override
        public CheckUpdatePolicy convertReverse(Boolean value) {
            if (!value) {
                return CheckUpdatePolicy.CHECK_NONE;
            } else {
                return (CheckUpdatePolicy) updatePolicyComboBox.getSelectedItem();
            }
        }
    }

    /** Table model for gatewayTable */
    private class GatewayTableModel extends AbstractTableModel {
        private ArrayList<Gateway> gws = new ArrayList<Gateway>(gateways.getAll());

        public GatewayTableModel() {
            super();
            gateways.addValuedListener(new ValuedListener<Gateways.Events, Gateway>() {
                @Override
                public void eventOccured(ValuedEvent<Events, Gateway> e) {
                    refreshGws();
                }
            });
        }

        /** Reload all displayed gateways */
        private void refreshGws() {
            gws = new ArrayList<Gateway>(gateways.getAll());
            fireTableDataChanged();
        }

        /** Get gateway showing at given row index */
        public Gateway getGateway(int rowIndex) {
            return gws.get(rowIndex);
        }

        @Override
        public int getRowCount() {
            return gws.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Gateway gw = gws.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return !gw.isHidden();
                case 1:
                    return gw.isFavorite();
                case 2:
                    return gw;
                default:
                    logger.warning("Index out of bounds!");
                    return null;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return l10n.getString("ConfigFrame.Show");
                case 1:
                    return l10n.getString("ConfigFrame.Favorite");
                case 2:
                    return l10n.getString("ConfigFrame.Gateway");
                default:
                    logger.warning("Index out of bounds!");
                    return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                case 1:
                    return Boolean.class;
                case 2:
                    return Gateway.class;
                default:
                    logger.warning("Index out of bounds!");
                    return Object.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Gateway gw = gws.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    HashSet<String> hidden = new HashSet<String>(Arrays.asList(config.getHiddenGateways()));
                    if ((Boolean)aValue) {
                        hidden.remove(gw.getName());
                    } else {
                        hidden.add(gw.getName());
                    }
                    config.setHiddenGateways(hidden.toArray(new String[]{}));
                    return;
                case 1:
                    HashSet<String> favorite = new HashSet<String>(Arrays.asList(config.getFavoriteGateways()));
                    if ((Boolean)aValue) {
                        favorite.add(gw.getName());
                    } else {
                        favorite.remove(gw.getName());
                    }
                    config.setFavoriteGateways(favorite.toArray(new String[]{}));
                    return;
                case 2:
                    logger.warning("Assignment not supported!");
                default:
                    logger.warning("Index out of bounds!");
                    return;
            }
        }
    }

    /** Render "favorite" column at gatewayTable */
    private class FavoriteGwRenderer extends SubstanceDefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setText(null);
            label.setHorizontalAlignment(JLabel.CENTER);
            if ((Boolean)value) {
                label.setIcon(Icons.get("star-full-16.png"));
            } else {
                label.setIcon(Icons.get("star-empty-16.png"));
            }
            return label;
        }
    }

    /** Render "gateway" column at gatewayTable */
    private class GatewayRenderer extends SubstanceDefaultTableCellRenderer {
        private GatewayComboBox.GatewayComboBoxRenderer renderer = new GatewayComboBox.GatewayComboBoxRenderer();
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            renderer.adjustLabel(label, (Gateway)value);
            return label;
        }
    }

    /** Listen for mouse or key events at "favorite" column at gateway table and change value if requested */
    private class FavoriteGwListener extends MouseAdapter {
        private KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getModifiers() != 0 || e.getKeyCode() != KeyEvent.VK_SPACE) {
                    return;
                }
                int row = gatewayTable.getSelectedRow();
                int column = gatewayTable.getSelectedColumn();
                if (changeValue(row, column)) {
                    e.consume();
                }
            }
        };

        @Override
        public void mouseClicked(MouseEvent e) {
            Point p = e.getPoint();
            int row = gatewayTable.rowAtPoint(p);
            int column = gatewayTable.columnAtPoint(p);
            changeValue(row, column);
        }

        /** Switch values if coordinates correct */
        private boolean changeValue(int row, int column) {
            if (!(column == 1 && row >= 0)) {
                //respond only to Favorite cells
                return false;
            }
            Boolean favorite = (Boolean) gwTableModel.getValueAt(row, column);
            favorite = !favorite;
            gwTableModel.setValueAt(favorite, row, column);
            return true;
        }
    }

    /** Update components if gateway selection changes */
    private class GatewaySelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = gatewayTable.getSelectedRow();
            boolean selected = (row >= 0);
            loginField.setEnabled(selected);
            passwordField.setEnabled(selected);
            if (!selected) {
                return;
            }

            boolean temp = fullyInicialized;
            fullyInicialized = false;
            Gateway gateway = gwTableModel.getGateway(row);
            Tuple<String, String> key = keyring.getKey(gateway != null ? gateway.getName() : null);
            if (key == null) {
                loginField.setText(null);
                passwordField.setText(null);
            } else {
                loginField.setText(key.get1());
                loginField.setCaretPosition(0);
                passwordField.setText(key.get2());
                passwordField.setCaretPosition(0);
            }
            fullyInicialized = temp;
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JCheckBox advancedCheckBox;
    private JCheckBox advancedControlsCheckBox;
    private JPanel appearancePanel;
    private JCheckBox checkUpdatesCheckBox;
    private JButton clearKeyringButton;
    private JButton closeButton;
    private Config config;
    private JPanel connectionPanel;
    private CountryPrefixPanel countryPrefixPanel;
    private JCheckBox debugCheckBox;
    private JCheckBox demandDeliveryReportCheckBox;
    private JPanel gatewayPanel;
    private JTable gatewayTable;
    private JPanel generalPanel;
    private JPanel gwDetailsPanel;
    private InfoLabel gwTipLabel;
    private JTextField httpProxyTextField;
    private JTextField httpsProxyTextField;
    private JLabel jLabel1;
    private JLabel jLabel11;
    private JLabel jLabel12;
    private JLabel jLabel14;
    private JLabel jLabel15;
    private JLabel jLabel16;
    private JLabel jLabel17;
    private JLabel jLabel18;
    private JLabel jLabel3;
    private JScrollPane jScrollPane1;
    private JComboBox lafComboBox;
    private JLabel logLocationLabel;
    private JTextField loginField;
    private JLabel lookLabel;
    private JCheckBox notificationAreaCheckBox;
    private JPasswordField passwordField;
    private JPanel privacyPanel;
    private JCheckBox reducedHistoryCheckBox;
    private JSpinner reducedHistorySpinner;
    private JCheckBox removeAccentsCheckBox;
    private InfoLabel restartLabel;
    private JCheckBox sameProxyCheckBox;
    private JTextField senderNameTextField;
    private JTextField senderNumberTextField;
    private JLabel senderNumberWarnLabel;
    private JCheckBox showPasswordCheckBox;
    private JTextField socksProxyTextField;
    private JCheckBox startMinimizedCheckBox;
    private JTabbedPane tabbedPane;
    private JComboBox themeComboBox;
    private JLabel themeLabel;
    private JCheckBox tipsCheckBox;
    private JCheckBox toolbarVisibleCheckBox;
    private JCheckBox unstableUpdatesCheckBox;
    private JComboBox updatePolicyComboBox;
    private JCheckBox useProxyCheckBox;
    private JCheckBox useSenderIDCheckBox;
    private JCheckBox windowCenteredCheckBox;
    private BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
    
}