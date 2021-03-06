/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compliance;

import compliance.Companion.DocLang;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author hohkim
 */
public final class Register extends javax.swing.JDialog {

    private Companion collector;
    private Properties properties;
    /**
     * Creates new form Register
     */
    public Register(JFrame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        collector = (Companion)parent;
        initFields();
    }
    
    public void initFields() {
        properties = collector.propManager.properties;
        textCaseName.setText(properties.getProperty("caseName"));
        textAuditor.setText(properties.getProperty("auditor"));
        textTarget.setText(properties.getProperty("caseDir"));       
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooser1 = new javax.swing.JFileChooser();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        textCaseName = new javax.swing.JTextField();
        textAuditor = new javax.swing.JTextField();
        buttonTarget = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        textTarget = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        comboLanguage = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Make New Case");
        setResizable(false);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("Case Name:");

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("Auditor Name:");

        buttonTarget.setText("Case Directory:");
        buttonTarget.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonTargetActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Cooper Black", 1, 18)); // NOI18N
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("[ Case Registration ]");

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonOK.setText("OK");
        buttonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOKActionPerformed(evt);
            }
        });

        textTarget.setToolTipText("");

        jLabel2.setText("Document Language:");

        comboLanguage.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Korean", "English", "Japanese", "Chinese" }));
        comboLanguage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboLanguageActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonTarget, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(textCaseName, javax.swing.GroupLayout.PREFERRED_SIZE, 520, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(buttonCancel)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(buttonOK))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(comboLanguage, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(textTarget, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 519, Short.MAX_VALUE)
                                    .addComponent(textAuditor, javax.swing.GroupLayout.Alignment.LEADING))))))
                .addGap(24, 24, 24))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel5)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textCaseName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(textAuditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonTarget)
                    .addComponent(textTarget, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(comboLanguage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonOK)
                    .addComponent(buttonCancel))
                .addContainerGap(23, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonTargetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonTargetActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File(properties.getProperty("caseDir")));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 
            textTarget.setText(chooser.getSelectedFile().getAbsolutePath());
        } else {
           // System.out.println("No Selection ");
        }
    }//GEN-LAST:event_buttonTargetActionPerformed

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        if ("".equals(textCaseName.getText()) || "".equals(textAuditor.getText())
                || "".equals(textTarget.getText())) {
            JOptionPane.showMessageDialog(null, "Case name, Auditor, Target directorty information should be supplied.",
                    "Case Registration Error", JOptionPane.ERROR_MESSAGE);
        } else {
            collector.setAuditor(textAuditor.getText());
            properties.setProperty("auditor", textAuditor.getText());
            
            collector.setCaseDir(textTarget.getText());
            properties.setProperty("caseDir", textTarget.getText());
            
            collector.setFileDir(textTarget.getText() + "\\Files");
            properties.setProperty("filesDir", textTarget.getText() + "\\Files");
            
            collector.setIndexDir(textTarget.getText() + "\\Index");
            properties.setProperty("indexDir", textTarget.getText() + "\\Index");
             
            // Case directory creation
            File f = new File(textTarget.getText());
            if (!f.exists()) {
                 boolean isFileDirCreated = f.mkdir();
                if (!isFileDirCreated) {
                    collector.setMessage(String.format(" Error: couldn't create %s\\Files",
                            f.getAbsolutePath()));
                }               
            }
            // File directory creation
            f = new File(textTarget.getText(), "Files");
            if (!f.exists()) {
                boolean isFileDirCreated = f.mkdir();
                if (!isFileDirCreated) {
                    collector.setMessage(String.format(" Error: couldn't create %s\\Files",
                            f.getAbsolutePath()));
                }
            }
            // Keyword directory creation
            f = new File(textTarget.getText(), "Keywords");
            if (!f.exists()) {
                boolean isFileDirCreated = f.mkdir();
                if (!isFileDirCreated) {
                    collector.setMessage(String.format(" Error: couldn't create %s\\Keywords",
                            f.getAbsolutePath()));
                }
            }
            URL resourceUrl = Companion.class.getResource("Sample.kwd");
            if (resourceUrl != null) {
                byte[] buffer = new byte[1024];
                int byteCount = 0;
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    inputStream = resourceUrl.openStream();
                    outputStream = new FileOutputStream(new File(f, "Sample.kwd"));
                    while ((byteCount = inputStream.read(buffer)) >= 0) {
                        outputStream.write(buffer, 0, byteCount);
                    }
                    inputStream.close();
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException | NullPointerException ex) {
                    Companion.logger.log(Level.SEVERE, null, ex);
                }
            }    
            String caseName = String.format("[ %s ] - %s", textCaseName.getText(), textAuditor.getText());
            collector.setCaseName(caseName);
            properties.setProperty("caseName", textCaseName.getText());
            
//            String caseInfo = String.format("%s (%s) Case.info", textCaseName.getText(), 
//                    textAuditor.getText());
//            File caseInfoFile = new File(textTarget.getText(), caseInfo);
//            collector.setCaseInfoFile(caseInfoFile);   
            
            File fileListFile = new File(textTarget.getText(), "scan_file_list.txt");
            collector.setFileListFile(fileListFile);
            properties.setProperty("scannedFile", fileListFile.getAbsolutePath());
            // Document Language Setting
            if (comboLanguage.getSelectedIndex() == 0) {
                collector.setDocLang(DocLang.Korean);
                properties.setProperty("language", "Korean");
            } else if (comboLanguage.getSelectedIndex() == 1) {
                collector.setDocLang(DocLang.English);
                properties.setProperty("language", "English");
            } else if (comboLanguage.getSelectedIndex() == 2) {
                collector.setDocLang(DocLang.Japanese);
                properties.setProperty("language", "Japanese");
            } else if (comboLanguage.getSelectedIndex() == 3) {
                collector.setDocLang(DocLang.Chinese);
                properties.setProperty("language", "Chinese");
            } 
//            createCaseInfoFile(caseName, caseInfoFile, fileListFile);
            this.dispose();
        }
    }//GEN-LAST:event_buttonOKActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void comboLanguageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboLanguageActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_comboLanguageActionPerformed

    public void createCaseInfoFile(String caseName, File caseInfo, File fileList) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(collector.getCaseInfoFile()))) {
            bw.write("Case      : " + caseName); 
            bw.newLine();
            bw.write("Language  : " + (String)comboLanguage.getSelectedItem());
            bw.newLine();
            bw.write("File List : " + fileList.getName());
            bw.newLine();
            bw.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Register.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Register.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Register.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Register.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Register dialog = new Register(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOK;
    private javax.swing.JButton buttonTarget;
    private javax.swing.JComboBox comboLanguage;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JTextField textAuditor;
    private javax.swing.JTextField textCaseName;
    private javax.swing.JTextField textTarget;
    // End of variables declaration//GEN-END:variables
}
