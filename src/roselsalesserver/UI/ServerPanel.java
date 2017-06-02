/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package roselsalesserver.UI;

import java.util.Properties;
import roselsalesserver.RoselSalesServer;
import roselsalesserver.ServerSettings;

/**
 *
 * @author nikiforovnikita
 */
public class ServerPanel extends javax.swing.JPanel {

    RoselSalesServer roselSalesServer;           
    
    /**
     * Creates new form serverPanel
     * @param roselSalesServer
     */
    public ServerPanel(RoselSalesServer roselSalesServer) {
        this.roselSalesServer = roselSalesServer;
        initComponents();
        loadSettingsToUI(roselSalesServer.getServerSettings());        
    }
    
    public final void loadSettingsToUI(Properties settings){     
        //settings.getProperty(TOOL_TIP_TEXT_KEY)
        dbTypeComboBox.setSelectedItem(settings.getProperty(ServerSettings.DB_TYPE).toString());
        serverTextField.setText(settings.getProperty(ServerSettings.DB_SERVER).toString());
        dbNameTextField.setText(settings.getProperty(ServerSettings.DB_NAME).toString());
        loginTextField.setText(settings.getProperty(ServerSettings.DB_LOGIN).toString());
        passwordField.setText(settings.getProperty(ServerSettings.DB_PASSWORD).toString());
        hostTextField.setText(settings.getProperty(ServerSettings.EMAIL_HOST));
        portTextField.setText(settings.getProperty(ServerSettings.EMAIL_PORT).toString());
        fromTextField.setText(settings.getProperty(ServerSettings.EMAIL_FROM).toString());
        emailLoginTextField.setText(settings.getProperty(ServerSettings.EMAIL_LOGIN).toString());
        emailPwdTextField.setText(settings.getProperty(ServerSettings.EMAIL_PASSWORD).toString());        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        initButton = new javax.swing.JButton();
        mailPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        hostTextField = new javax.swing.JTextField();
        portTextField = new javax.swing.JTextField();
        emailLoginTextField = new javax.swing.JTextField();
        emailPwdTextField = new javax.swing.JPasswordField();
        saveEmailSettingsButton = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        fromTextField = new javax.swing.JTextField();
        dbPanel = new javax.swing.JPanel();
        dbTypeComboBox = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        saveDbSettingsButton = new javax.swing.JButton();
        serverTextField = new javax.swing.JTextField();
        dbNameTextField = new javax.swing.JTextField();
        loginTextField = new javax.swing.JTextField();
        passwordField = new javax.swing.JPasswordField();

        initButton.setText("Инициализация");
        initButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                initButtonActionPerformed(evt);
            }
        });

        mailPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("e-mail"));
        mailPanel.setForeground(new java.awt.Color(204, 255, 204));

        jLabel1.setText("Host:");

        jLabel2.setText("Port:");

        jLabel3.setText("Login:");

        jLabel4.setText("Pwd:");

        hostTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hostTextFieldActionPerformed(evt);
            }
        });

        portTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portTextFieldActionPerformed(evt);
            }
        });

        emailPwdTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                emailPwdTextFieldActionPerformed(evt);
            }
        });

        saveEmailSettingsButton.setText("Save");
        saveEmailSettingsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveEmailSettingsButtonActionPerformed(evt);
            }
        });

        jLabel10.setText("From:");

        fromTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fromTextFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout mailPanelLayout = new javax.swing.GroupLayout(mailPanel);
        mailPanel.setLayout(mailPanelLayout);
        mailPanelLayout.setHorizontalGroup(
            mailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mailPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(hostTextField, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(portTextField, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(fromTextField, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(emailLoginTextField, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(emailPwdTextField, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
            .addGroup(mailPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(saveEmailSettingsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(90, 90, 90))
        );
        mailPanelLayout.setVerticalGroup(
            mailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mailPanelLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(mailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(hostTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(portTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(mailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(fromTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(emailLoginTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(emailPwdTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveEmailSettingsButton)
                .addGap(37, 37, 37))
        );

        mailPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {hostTextField, jLabel1});

        mailPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jLabel2, portTextField});

        mailPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {emailLoginTextField, jLabel3});

        dbPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Database"));
        dbPanel.setForeground(new java.awt.Color(153, 255, 255));
        dbPanel.setToolTipText("");
        dbPanel.setName(""); // NOI18N

        dbTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "MS SQL Server", "PostgreSQL", "SQLite" }));

        jLabel5.setText("Type:");

        jLabel6.setText("Server:");

        jLabel7.setText("Name:");

        jLabel8.setText("Login:");

        jLabel9.setText("Pwd:");

        saveDbSettingsButton.setText("Save");
        saveDbSettingsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveDbSettingsButtonActionPerformed(evt);
            }
        });

        serverTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverTextFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout dbPanelLayout = new javax.swing.GroupLayout(dbPanel);
        dbPanel.setLayout(dbPanelLayout);
        dbPanelLayout.setHorizontalGroup(
            dbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dbPanelLayout.createSequentialGroup()
                .addGroup(dbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(dbPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(saveDbSettingsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(dbPanelLayout.createSequentialGroup()
                        .addGroup(dbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(dbPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(dbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6)
                                    .addComponent(jLabel7)
                                    .addComponent(jLabel8)
                                    .addComponent(jLabel9)))
                            .addGroup(dbPanelLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(jLabel5)))
                        .addGap(18, 18, 18)
                        .addGroup(dbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(dbTypeComboBox, 0, 126, Short.MAX_VALUE)
                            .addComponent(serverTextField)
                            .addComponent(dbNameTextField)
                            .addComponent(loginTextField)
                            .addComponent(passwordField))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        dbPanelLayout.setVerticalGroup(
            dbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dbPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(dbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dbTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(dbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(serverTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(dbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(dbNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(dbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(loginTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(dbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(passwordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(saveDbSettingsButton))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(initButton)
                    .addComponent(dbPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(mailPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(395, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(initButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(dbPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mailPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(43, 43, 43))
        );

        dbPanel.getAccessibleContext().setAccessibleName("");
    }// </editor-fold>//GEN-END:initComponents

    private void initButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_initButtonActionPerformed
        // connect to DB and init it
        roselSalesServer.initDB();
    }//GEN-LAST:event_initButtonActionPerformed

    private void hostTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hostTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_hostTextFieldActionPerformed

    private void portTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_portTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_portTextFieldActionPerformed

    private void saveDbSettingsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveDbSettingsButtonActionPerformed
        Properties settings = new Properties();        
        settings.setProperty(ServerSettings.DB_TYPE, (String) dbTypeComboBox.getSelectedItem());
        settings.setProperty(ServerSettings.DB_SERVER, serverTextField.getText());
        settings.setProperty(ServerSettings.DB_NAME, dbNameTextField.getText());
        settings.setProperty(ServerSettings.DB_LOGIN, loginTextField.getText());
        settings.put(ServerSettings.DB_PASSWORD, String.valueOf(passwordField.getPassword()));        
        roselSalesServer.saveSettings(settings);
    }//GEN-LAST:event_saveDbSettingsButtonActionPerformed

    private void serverTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_serverTextFieldActionPerformed

    private void saveEmailSettingsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveEmailSettingsButtonActionPerformed
        Properties settings = new Properties();        
        settings.setProperty(ServerSettings.EMAIL_HOST, hostTextField.getText());
        settings.setProperty(ServerSettings.EMAIL_PORT, portTextField.getText());
        settings.setProperty(ServerSettings.EMAIL_FROM, fromTextField.getText());
        settings.setProperty(ServerSettings.EMAIL_LOGIN, emailLoginTextField.getText());
        settings.put(ServerSettings.EMAIL_PASSWORD, String.valueOf(emailPwdTextField.getPassword()));        
        roselSalesServer.saveEmailSettings(settings);
    }//GEN-LAST:event_saveEmailSettingsButtonActionPerformed

    private void fromTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fromTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_fromTextFieldActionPerformed

    private void emailPwdTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_emailPwdTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_emailPwdTextFieldActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField dbNameTextField;
    private javax.swing.JPanel dbPanel;
    private javax.swing.JComboBox<String> dbTypeComboBox;
    private javax.swing.JTextField emailLoginTextField;
    private javax.swing.JPasswordField emailPwdTextField;
    private javax.swing.JTextField fromTextField;
    private javax.swing.JTextField hostTextField;
    private javax.swing.JButton initButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JTextField loginTextField;
    private javax.swing.JPanel mailPanel;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JTextField portTextField;
    private javax.swing.JButton saveDbSettingsButton;
    private javax.swing.JButton saveEmailSettingsButton;
    private javax.swing.JTextField serverTextField;
    // End of variables declaration//GEN-END:variables
}
