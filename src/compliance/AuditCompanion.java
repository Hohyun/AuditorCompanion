/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compliance;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.*;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.tree.*;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 *
 * @author hohkim
 */
public final class AuditCompanion extends javax.swing.JFrame 
    implements TreeCheckingListener, PropertyChangeListener, 
    TableModelListener, ListSelectionListener, ActionListener {

    //private JTree tree;
    private CheckboxTree folderTree;
    private CheckboxTree fileTypeTree;
    private JSplitPane vSplitPane;
    private JSplitPane hSplitPane;
    private JPanel mainPanel;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private JButton buttonMakeNew;
    private JButton buttonAnalyze;
    private JButton buttonLoadCase;
    private JButton buttonCopy;
    private JButton buttonIndex;
    private JButton buttonSearch;
    private JButton buttonReset;
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenuItem openMenuItem;
    private JMenuItem newMenuItem;
    private JMenuItem resetMenuItem;
    private JMenuItem exitMenuItem;
    private JMenu workMenu;
    private JMenuItem analyzeMenuItem;
    private JMenuItem copyMenuItem;
    private JMenuItem indexMenuItem;
    private JMenu searchMenu;
    private JMenuItem searchMenuItem;
    private JMenuItem keywordMenuItem;
    private JMenu helpMenu;
    private JMenuItem aboutMenuItem;
    //private JProgressBar progressBar;
    private List<String> categories = Arrays.asList("Office", "Image", "A/V", "ETC"); 
    private List<String> offices = Arrays.asList("DOC","DOCX", "XLS", "XLSX", "PPT", "PPTX", "PDF");
    private List<String> images = Arrays.asList("JPEG","JPG", "GIF", "PNG", "TIFF"); 
    private List<String> avfiles = Arrays.asList("MP3","MP4"); 
    private List<String> etcfiles = Arrays.asList("TXT", "TEX", "CLS");
    private Register register;
    private JLabel lblCase;
    private JTable infoTable;
    private MyTableData infoData;
    private String targetDir;
    private String targetFileDir;
    private String targetIndexDir;
    private String caseName = "";
    private File fileListFile;
    private File caseInfoFile;
    private String auditor = "";  
    private DocLang docLang = DocLang.English;
    private Analyzer analyzer;
    private Copier copier;
    private Indexer indexer;
    private List<String> jobDirs = new ArrayList<String>();
    private List<String> jobFileTypes = new ArrayList<String>();
    private Stage stage;
    private JLabel lblStage;

    public static enum DocLang {
        English, Korean, Japanease
    }
    public static enum Stage {
        BEFORE_STARTED, CASE_CREATED, ANALYZE_COMPLETED,
        COPY_COMPLETED, CASE_LOADED, INDEX_CREATED
    }
    /**
     * Creates new form AuditCompanion
     */
    public AuditCompanion() throws IOException {
        //initComponents();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        vSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        vSplitPane.setTopComponent(folderInitComponents());
        vSplitPane.setBottomComponent(fileTypeInitComponents());
        
        hSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        hSplitPane.setLeftComponent(vSplitPane);
        hSplitPane.setRightComponent(mainAreaInitComponents());

        getContentPane().add(hSplitPane, BorderLayout.CENTER);
        getContentPane().add(statusPanelInitComponents(), BorderLayout.PAGE_END);
        
        menuInitComponents();
        pack();
        setVisible(true);
        //setPreferredSize(new Dimension(1024,768));
        setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        vSplitPane.setDividerLocation(0.5);
        hSplitPane.setDividerLocation(170);
        setStage(Stage.BEFORE_STARTED);
    }
    
    private JPanel statusPanelInitComponents() {
        statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusPanel.setPreferredSize(new Dimension(this.getWidth(),20));
        statusLabel = new JLabel(" Welcome!", SwingConstants.LEFT);
        statusPanel.add(statusLabel, BorderLayout.CENTER );
        JPanel rightPanel = new JPanel(new BorderLayout());
        //rightPanel.setPreferredSize(new Dimension(400,16));
        lblStage = new JLabel("", SwingConstants.CENTER);
        lblStage.setFont(new Font("TimesRoman",Font.PLAIN,10));
        rightPanel.add(lblStage, BorderLayout.LINE_END);
        statusPanel.add(rightPanel, BorderLayout.LINE_END);
        return statusPanel;
    }
            
    private JPanel mainAreaInitComponents() throws IOException {
        mainPanel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        JPanel midPanel = new JPanel(new BorderLayout());
        JPanel bottomPanel = new JPanel(new FlowLayout());  
        // top   
        lblCase = new JLabel("[ Case : Unknown ]");
        lblCase.setFont(new Font("맑은 고딕",Font.BOLD,20));
        topPanel.add(lblCase, BorderLayout.CENTER);
        // middle
        infoTable = new JTable();
        infoTable.getModel().addTableModelListener(this);
        infoTable.getSelectionModel().addListSelectionListener(this);
        JScrollPane scrollPane = new JScrollPane(infoTable);
        midPanel.add(scrollPane, BorderLayout.CENTER);
        // bottom
        buttonMakeNew = new JButton("Create Case");
        buttonMakeNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMakeNewActionPerformed(evt);
            }
        });
        buttonAnalyze = new JButton("Analyze");
        buttonAnalyze.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAnalyzeActionPerformed(evt);
            }
        });
        buttonCopy = new JButton("Copy");
        buttonCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCopyActionPerformed(evt);
            }
        });        
        buttonLoadCase = new JButton("Load Case");
        buttonLoadCase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLoadCaseActionPerformed(evt);
            }
        });
        buttonIndex = new JButton("Index");
        buttonIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonIndexActionPerformed(evt);
            }
        });
        buttonSearch = new JButton("Search");
        buttonReset = new JButton("Reset Case");
        buttonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetActionPerformed(evt);
            }
        });
        bottomPanel.add(buttonMakeNew);
        bottomPanel.add(buttonAnalyze);
        bottomPanel.add(buttonCopy);
        bottomPanel.add(buttonLoadCase);
        bottomPanel.add(buttonIndex);
        bottomPanel.add(buttonSearch);
        bottomPanel.add(buttonReset);    
        mainPanel.add(topPanel, BorderLayout.PAGE_START);
        mainPanel.add(midPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.PAGE_END);  
        return mainPanel;
    }
    
    private JPanel folderInitComponents() {
        JPanel folderPanel = new JPanel(new BorderLayout());
        DefaultMutableTreeNode top =  new DefaultMutableTreeNode("Computer");
        createNodes(top);
        folderTree = new CheckboxTree(top);
        folderTree.getCheckingModel().setCheckingMode(TreeCheckingModel.CheckingMode.PROPAGATE);
        folderTree.addTreeCheckingListener(this);        
        ImageIcon icon = new ImageIcon("image/leaf.gif");
        if (icon != null) {
            DefaultCheckboxTreeCellRenderer renderer = new DefaultCheckboxTreeCellRenderer();
            renderer.setLeafIcon(icon);
            folderTree.setCellRenderer(renderer);      
        }     
        JScrollPane sp = new JScrollPane(folderTree);
        folderPanel.add(sp);
        return folderPanel;
    }
    
    private void createSubFileTypeNode(DefaultMutableTreeNode parent, List<String> types) {
        for (String type: types) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(type);
            parent.add(node);
        }
    }
    
    private JPanel fileTypeInitComponents() {  
        JPanel fileTypePanel = new JPanel(new BorderLayout());
        DefaultMutableTreeNode top =  new DefaultMutableTreeNode("Types");
        for (String category: categories) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(category);
            top.add(node);
            switch (category) {
                case "Office":
                    createSubFileTypeNode(node, offices);
                    break;  
                case "Image":
                    createSubFileTypeNode(node, images);
                    break;     
                case "A/V":
                    createSubFileTypeNode(node, avfiles);
                    break;                        
                case "ETC":
                    createSubFileTypeNode(node, etcfiles);
                    break;
            }
        }
        fileTypeTree = new CheckboxTree(top);
        fileTypeTree.getCheckingModel().setCheckingMode(TreeCheckingModel.CheckingMode.PROPAGATE);
        fileTypeTree.addTreeCheckingListener(this);
        ImageIcon icon = new ImageIcon("image/leaf.gif");
        if (icon != null) {
            DefaultCheckboxTreeCellRenderer renderer = new DefaultCheckboxTreeCellRenderer();
            renderer.setLeafIcon(icon);
            fileTypeTree.setCellRenderer(renderer);      
        }             
        JScrollPane sp = new JScrollPane(fileTypeTree);
        fileTypePanel.add(sp);
        return fileTypePanel;
    }
   
    private void menuInitComponents() {
        menuBar = new JMenuBar();
        // File Menu
        fileMenu = new JMenu("File");
        fileMenu.setMnemonic('f');
        newMenuItem = new JMenuItem("New");
        newMenuItem.setMnemonic('n');
        newMenuItem.addActionListener(this);
        openMenuItem = new JMenuItem("Open");
        openMenuItem.setMnemonic('o');
        openMenuItem.addActionListener(this);
        resetMenuItem = new JMenuItem("Reset");
        resetMenuItem.setMnemonic('r');
        resetMenuItem.addActionListener(this);        
        exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.setMnemonic('x');
        exitMenuItem.addActionListener(this);
        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(resetMenuItem);
        fileMenu.add(exitMenuItem);
        // Work Menu
        workMenu = new JMenu("Work");
        workMenu.setMnemonic('w');
        analyzeMenuItem = new JMenuItem("Analyze");
        analyzeMenuItem.setMnemonic('a');
        analyzeMenuItem.addActionListener(this);
        copyMenuItem = new JMenuItem("Copy");
        copyMenuItem.setMnemonic('c');
        copyMenuItem.addActionListener(this);
        indexMenuItem = new JMenuItem("Index");
        indexMenuItem.setMnemonic('i');
        indexMenuItem.addActionListener(this);
        workMenu.add(analyzeMenuItem);
        workMenu.add(copyMenuItem);
        workMenu.add(indexMenuItem);
        // Search Menu
        searchMenu = new JMenu("Search");
        searchMenu.setMnemonic('s');
        searchMenuItem = new JMenuItem("Search");
        searchMenuItem.setMnemonic('s');
        searchMenuItem.addActionListener(this);
        keywordMenuItem = new JMenuItem("Keyword");
        keywordMenuItem.setMnemonic('k');
        keywordMenuItem.addActionListener(this);
        searchMenu.add(searchMenuItem);
        searchMenu.add(keywordMenuItem);
        // Help Menu
        helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('h');
        aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.setMnemonic('a');
        aboutMenuItem.addActionListener(this);
        helpMenu.add(aboutMenuItem);
        menuBar.add(fileMenu);
        menuBar.add(workMenu);
        menuBar.add(searchMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }
    
    private void createNodes(DefaultMutableTreeNode top) {         
        File roots[] = File.listRoots();
        for (File f: roots) {
            if (f.canRead()) {
                DefaultMutableTreeNode drive = new DefaultMutableTreeNode(f.getPath());
                top.add(drive);
                
                File[] dirs = f.listFiles();
                for (File dir: dirs) {
                    if (dir.isDirectory()) {
                        try {
                            //createSubNode(drive, dir);
                            DefaultMutableTreeNode node = new DefaultMutableTreeNode(dir.getName());
                            drive.add(node);
                        } catch (NullPointerException e) {
                            System.err.println(e.getMessage() + ": " + dir.getName());
                        }
                    }
                }
            }
        }
    }
    
    public void createSubNode(DefaultMutableTreeNode parent, File dir) {
       DefaultMutableTreeNode child = new DefaultMutableTreeNode(dir.getName());
       parent.add(child);
       
       File[] files = dir.listFiles();
       for (File file: files) {
           if (file.isDirectory()) {
               try {
                   DefaultMutableTreeNode grandchild = new DefaultMutableTreeNode(file.getName());
                   child.add(grandchild);
                   //createSubNode(child, file);
               } catch (NullPointerException e) {
                   System.err.println(e.getMessage() + ": " + file.getName());            
               }
           }
       }
   }
      
    @Override
    public void valueChanged(TreeCheckingEvent e) {
        jobDirs.clear();
        jobFileTypes.clear();
//        if ( e.getSource() == fileTypeTree ) {
//            System.out.println("fileTypeTree clicked");
//        }      
        TreePath[] folderNodes = folderTree.getCheckingPaths();
        TreePath[] fileTypeNodes = fileTypeTree.getCheckingPaths();
        if (folderNodes == null || fileTypeNodes == null) {
            return;
        }    
        System.out.print("\n");
             
        for (TreePath node : folderNodes) {
            String pathString = "";
            for (int i = 1; i < node.getPath().length; i++) {
                pathString += node.getPath()[i].toString();
            }
            jobDirs.add(pathString);
        }
        for (TreePath node: fileTypeNodes) {
            if (node.getPathCount()>2) {
                jobFileTypes.add(node.getLastPathComponent().toString());
            }
        } 
        Collections.sort(jobDirs);
        Collections.sort(jobFileTypes);
        if (stage == Stage.ANALYZE_COMPLETED || stage == Stage.CASE_LOADED || stage == Stage.COPY_COMPLETED) {
            try {
                updateInfoTable(jobFileTypes);       
            } catch (IOException evt) {
                System.out.println(evt.getMessage());
            }
        } 
        // System.out.println(jobDirs);
        // System.out.println(jobFileTypes);
    }
    
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        TableModel model = infoTable.getModel();
        int[] selected_rows = infoTable.getSelectedRows();
        // 한 줄 선택 시는 동작 않도록
        if (selected_rows.length == 1) {
            return;
        }
        for (int row: selected_rows) {       
           boolean onOff = (Boolean) model.getValueAt(row, 0);
           model.setValueAt(!onOff, row, 0);
           //String fn = model.getValueAt(row, 1).toString();
           //System.out.println(fn);
        }
    }
   
    @Override
    public void tableChanged(TableModelEvent e) {
        //
   }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        //
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

   
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) throws IOException {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                // Metal, Nimbus, CDE/Motif, Windows, Windows Classic
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                } else {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            System.out.println(ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new AuditCompanion().setVisible(true);
                } catch (IOException ex) {
                    Logger.getLogger(AuditCompanion.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    public void buttonMakeNewActionPerformed(ActionEvent evt) {
        register = new Register(this, true);
        register.setVisible(true);
        if (caseName.equals("") || auditor.equals("") || targetDir.equals("")) {
            // setMessage(" Case was not created. Case Name, Auditor and Target Directory information should be supplied");
        } else {
            setStage(Stage.CASE_CREATED);
            System.out.println(caseName);
            System.out.println(caseInfoFile.getAbsolutePath());
            System.out.println(fileListFile.getAbsolutePath());
            System.out.println(docLang);
        }
    }
    
    public void buttonCopyActionPerformed(ActionEvent evt) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        MyTableModel model = (MyTableModel) infoTable.getModel();
        copier = new Copier(model, this);
        copier.addPropertyChangeListener(this);
        copier.execute();
    }
    
   public void buttonIndexActionPerformed(ActionEvent evt) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        indexer = new Indexer(targetIndexDir, targetFileDir, docLang, this);
        //indexer.makeIndex();
        //setCursor(null);
        //setStage(Stage.INDEX_CREATED);
        indexer.addPropertyChangeListener(this);
        indexer.execute();
    }
   
    public void buttonAnalyzeActionPerformed(ActionEvent evt) {
        if (caseName.equals("")) {
            Toolkit.getDefaultToolkit().beep();
            setMessage("Please do \"Case Create\" before analyzing!");
            return;
        }
        if (jobDirs.isEmpty() || jobFileTypes.isEmpty() ) {
            setMessage("Please select [Source Directories] and [File Types].");
            return;
        }
        //progressBar.setStringPainted(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        analyzer = new Analyzer(jobDirs, jobFileTypes, targetDir, fileListFile, this);   
        analyzer.setParent(this);
        analyzer.addPropertyChangeListener(this);
        analyzer.execute();
    }
    
    public void buttonLoadCaseActionPerformed(ActionEvent evt) {
        //throw new UnsupportedOperationException("Not yet implemented");
        JFileChooser chooser = new JFileChooser();
        
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Compliance Audit File", "txt");
         chooser.setFileFilter(filter);

        // chooser.setCurrentDirectory(new File(targetDir)); 
        // 나중에 수정할 것
        chooser.setCurrentDirectory(new File("C:\\"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 
            try {
                //setInfoTableWithVerify(chooser.getSelectedFile().getAbsolutePath());
                File file = chooser.getSelectedFile();
                setTargetDir(file.getParent());
                setInfoTable(file);
                
                String fn = file.getName();
                String s1 = fn.split("\\.")[0];
                setAuditor(s1.substring(s1.indexOf("[")+1, s1.indexOf("]")));
                setCaseName(s1.substring(s1.indexOf("]")+1));
                setCaseLabel(String.format("[ %s ] %s", auditor, caseName));
                setFileListFile(file);
            } catch (IOException ex) {
                System.out.println(ex);
            }
            setStage(Stage.CASE_LOADED);
        } else {
            System.out.println("No Selection ");
            setMessage(" No case was loaded!");
        }
        
    }
    
    @Override
    public void actionPerformed(ActionEvent ae) {
        if("New".equals(ae.getActionCommand())) {
             buttonMakeNewActionPerformed(ae);
        } else if("Open".equals(ae.getActionCommand())) {
             buttonLoadCaseActionPerformed(ae);
        } else if("Reset".equals(ae.getActionCommand())) {
             buttonResetActionPerformed(ae);
        } else if("Exit".equals(ae.getActionCommand())) {
             System.exit(0);
        } else if("Analyze".equals(ae.getActionCommand())) {  
            buttonAnalyzeActionPerformed(ae);
        } else if("Copy".equals(ae.getActionCommand())) {            
            buttonCopyActionPerformed(ae);
        } else if("Index".equals(ae.getActionCommand())) {
            buttonIndexActionPerformed(ae);
        } else if("Search".equals(ae.getActionCommand())) {
            JOptionPane.showMessageDialog(this,"Search function is under construction!");
        } else if("Keyword".equals(ae.getActionCommand())) {
            JOptionPane.showMessageDialog(this,"Keyword management function is under construction!");
        } else if("About".equals(ae.getActionCommand())) {
            JOptionPane.showMessageDialog(this, "Compliance Audtor's Companion"
                    + "\n- Developed by H.H.Kim (SELBI)\n- Ver 1.0 (2012.11.7)");
        }
    }
     
    public void buttonResetActionPerformed(ActionEvent evt) {
        setStage(Stage.BEFORE_STARTED);
      //  try {
            //setInfoTable("");
        Object[][] data = {};
        String[] columnNames = {};
        infoTable.setModel(new MyTableModel(data,columnNames));
        setAuditor("");
        setCaseName("");
        setCaseLabel("[ Case : Unknown ]");
       // } catch (IOException e) {
         //   System.err.println(e.getMessage());
       // }
    }
            
    public void setCaseLabel(String caseName) {
        lblCase.setText(caseName);
    }
    
    public String getCaseLabel() {
       return lblCase.getText();
    }
    
    public void setTargetDir(String dir) {
        targetDir = dir;
        targetIndexDir = targetDir + "\\Index";
        targetFileDir = targetDir + "\\Files";
       // File f = new File(targetFileDir.replace("\\","\\\\"));
        File f = new File(targetFileDir);
        if (!f.exists()) {
            boolean isFileDirCreated = f.mkdir();     
            targetIndexDir = targetDir + "\\Index";
            if (!isFileDirCreated) {
                setMessage(String.format(" Error: couldn't create %s\\Files and %s\\index", 
                    targetDir, targetDir));
            }
        }
        System.out.println(targetDir + ":" + targetIndexDir + ":" + targetFileDir);
    }   
    
    public String getTargetDir() {
        return targetDir;
    }
    
    public String getTargetFileDir() {
        return targetFileDir;
    }
    
    public String getTargetIndexDir() {
        return targetIndexDir;
    }
    
    public void setAuditor(String name) {
        auditor = name;
    }
    
    public String getAuditor() {
        return auditor;
    }    
    
    public void setCaseName(String caseName) {
        this.caseName = caseName;
        setCaseLabel(caseName);
    }
    
    public String getCaseName() {
        return caseName;
    }
    
    public void setFileListFile(File file) {
        fileListFile = file;
    }
    
    public File getFileListFile() {
        return fileListFile;
    }

    public void setCaseInfoFile(File file) {
        caseInfoFile = file;
    }
    
    public File getCaseInfoFile() {
        return caseInfoFile;
    }
    
    public void setDocLang(DocLang lang) {
        docLang = lang;
    }
    
    public DocLang getDoclang() {
        return docLang;
    }
    
    public void setStage(Stage stage) {
        this.stage = stage;
        lblStage.setText(stage.toString());
        
        switch (stage) {
            // make case, analyze, copy, load case, index, search, reset
            case BEFORE_STARTED:
                enableButtons(true, false, false, true, false, false, false);
                break;
            case CASE_CREATED:
                enableButtons(false, true, false, false, false, false, true);
                break;
            case ANALYZE_COMPLETED:
                enableButtons(false, false, true, false, false, false, true);
                break;
            case COPY_COMPLETED:
                enableButtons(false, false, false, false, true, false, true);
                break;
            case CASE_LOADED:
                enableButtons(false, false, true, false, true, false, true);
                break;
            case INDEX_CREATED:
                enableButtons(true, false, false, false, false, true, true);
                break;
        }
    }
   
    public Stage getStage() {
        return stage;
    }
           
    public void setInfoDataAndHeader (Object[][] data, String[] header) {
        infoData.setData(data);
        infoData.setHeader(header);
        infoData.setColumnCount(header.length);
    }
   
    public void setInfoTable(File file) throws IOException {  
        infoData = new MyTableData(file);
        infoTable.setModel(new MyTableModel(infoData.getData(), infoData.getHeader()));
        infoTableColumnWidthAdjust();
        String msg = String.format("File Count: %d, Total Size: %,.0f KB", infoData.getRowCount(), infoData.getTotalSize());
        setMessage(msg);
    }
    
    public void setInfoTable(MyTableModel model) {
        infoTable.setModel(model);
        infoTableColumnWidthAdjust();
    }
    
    public void updateInfoTable(List<String> exts) throws IOException {  
        infoTable.setModel(new MyTableModel(infoData.getDataWithType(exts), infoData.getHeader()));
        infoTableColumnWidthAdjust();
        String msg = String.format("File Count: %d, Total Size: %,.0f KB", infoData.getCountType(), infoData.getSizeType());
        setMessage(msg);
    }
    
    public void updateInfoTable(List<String> exts, boolean verify) throws IOException { 
        if (verify){
            infoTable.setModel(new MyTableModel(infoData.getDataWithVerify(exts), infoData.getHeaderWithVerify()));     
            infoTableColumnWidthAdjust();
            String msg = String.format("File Count: %d, Total Size: %,.0f KB", infoData.getCountType(), infoData.getSizeType());
            setMessage(msg);
        }
    }    
     
    public void infoTableColumnWidthAdjust () {
        TableColumn column;
        for (int i = 0; i < infoData.getColumnCount(); i++) {
            column = infoTable.getColumnModel().getColumn(i);
            if (i == 0) {
                column.setPreferredWidth(20);
            } else if (i == 1) {
                column.setPreferredWidth(500);
            } else {
                column.setPreferredWidth(50);
            }
        }   
    }
    
    public void enableButtons(boolean b1, boolean b2, boolean b3, boolean b4,
            boolean b5, boolean b6, boolean b7) {       
        if (b1 == true) { 
            buttonMakeNew.setEnabled(true);
        } else { 
            buttonMakeNew.setEnabled(false); 
        }
        if (b2 == true) { 
            buttonAnalyze.setEnabled(true);
        } else { 
            buttonAnalyze.setEnabled(false); 
        }
        if (b3 == true) { 
            buttonCopy.setEnabled(true);
        } else { 
            buttonCopy.setEnabled(false); 
        }
         if (b4 == true) { 
            buttonLoadCase.setEnabled(true);
        } else { 
            buttonLoadCase.setEnabled(false); 
        }       
        if (b5 == true) { 
            buttonIndex.setEnabled(true);
        } else { 
            buttonIndex.setEnabled(false); 
        }         
        if (b6 == true) { 
            buttonSearch.setEnabled(true);
        } else { 
            buttonSearch.setEnabled(false); 
        }    
        if (b7 == true) { 
            buttonReset.setEnabled(true);
        } else { 
            buttonReset.setEnabled(false); 
        }        
    }
 
    public void setMessage(String msg) {
        statusLabel.setText(msg);
    }
}
