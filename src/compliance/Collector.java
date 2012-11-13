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
import java.awt.Frame;
import java.awt.Image;
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

/**
 *
 * @author hohkim
 */
public final class Collector extends javax.swing.JFrame 
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
    private JButton buttonNew;
    private JButton buttonAnalyze;
    private JButton buttonOpen;
    private JButton buttonCopy;
    private JButton buttonIndex;
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
    private String caseDir;
    private String fileDir;
    private String indexDir;
    private String caseName = "";
    private File fileListFile;
    private File caseInfoFile;
    private String auditor = "";  
    private DocLang docLang = DocLang.English;
//    private Analyzer analyzer;
//    private Copier copier;
//    private Indexer indexer;
    private List<String> jobDirs = new ArrayList<String>();
    private List<String> jobFileTypes = new ArrayList<String>();
    private Stage stage;
    private JLabel lblStage;

    public static enum DocLang {
        English, Korean, Japanese
    }
    public static enum Stage {
        BEFORE_STARTED, CASE_CREATED, ANALYZE_COMPLETED,
        COPY_COMPLETED, CASE_LOADED, INDEX_CREATED
    }
    /**
     * Creates new form Collector
     */
    public Collector() throws IOException {
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
        lblCase = new JLabel("");
        lblCase.setFont(new Font("맑은 고딕",Font.BOLD,20));
        topPanel.add(lblCase, BorderLayout.CENTER);
        // middle
        infoTable = new JTable();
        infoTable.getModel().addTableModelListener(this);
        infoTable.getSelectionModel().addListSelectionListener(this);
        JScrollPane scrollPane = new JScrollPane(infoTable);
        midPanel.add(scrollPane, BorderLayout.CENTER);
        // bottom
        buttonNew = new JButton("New");
        buttonNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNewActionPerformed(evt);
            }
        });
        buttonOpen = new JButton("Open");
        buttonOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOpenActionPerformed(evt);
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
        buttonIndex = new JButton("Index");
        buttonIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonIndexActionPerformed(evt);
            }
        });
//        buttonSearch = new JButton("Search");
//        buttonSearch.addActionListener(new java.awt.event.ActionListener() {
//            public void actionPerformed(java.awt.event.ActionEvent evt) {
//                buttonSearchActionPerformed(evt);
//            }
//        });        
        buttonReset = new JButton("Reset");
        buttonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetActionPerformed(evt);
            }
        });
        bottomPanel.add(buttonNew);
        bottomPanel.add(buttonOpen);
        bottomPanel.add(buttonAnalyze);
        bottomPanel.add(buttonCopy);
        bottomPanel.add(buttonIndex);
//        bottomPanel.add(buttonSearch);
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
                    Collector collector = new Collector();
                    Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/compliance/images/search3.jpg"));
                    collector.setIconImage(icon);
                    collector.setTitle("Field Audiot's Companion for Korean Air");
                    collector.setVisible(true);
                    //new Collector().setVisible(true);
                } catch (IOException ex) {
                    Logger.getLogger(Collector.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    public void buttonNewActionPerformed(ActionEvent evt) {
        register = new Register(this, true);
        register.setLocation((this.getWidth()-register.getWidth())/2, 
                (this.getHeight()-register.getHeight())/2);
        register.setVisible(true);
        if (caseName.equals("") || auditor.equals("") || caseDir.equals("")) {
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
        Copier copier = new Copier(model, this);
        copier.addPropertyChangeListener(this);
        copier.execute();
    }
    
   public void buttonIndexActionPerformed(ActionEvent evt) {
       setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
       if (stage == Stage.COPY_COMPLETED) {
           Indexer indexer = new Indexer(indexDir, fileDir, docLang, this);
           //indexer.addPropertyChangeListener(this);
           indexer.execute();
       } else if (stage == Stage.ANALYZE_COMPLETED || stage == Stage.CASE_LOADED) {
           MyTableModel model = (MyTableModel) infoTable.getModel();
           IndexerB indexerB = new IndexerB(indexDir, model, docLang, this);
           //indexerB.addPropertyChangeListener(this);
           indexerB.execute();           
       }
    }
   
    public void buttonAnalyzeActionPerformed(ActionEvent evt) {
        if (caseName.equals("")) {
            JOptionPane.showMessageDialog(null, "Please create \"New case\" before analyzing! ", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (jobDirs.isEmpty() || jobFileTypes.isEmpty() ) {
            setMessage("Please select [Source Directories] and [File Types].");
            JOptionPane.showMessageDialog(null, "Please select [Source Directories] and [File Types].", 
                    "Error", JOptionPane.ERROR_MESSAGE);            
            return;
        }
        //progressBar.setStringPainted(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Analyzer analyzer = new Analyzer(jobDirs, jobFileTypes, caseDir, fileListFile, this);   
        analyzer.setParent(this);
        analyzer.addPropertyChangeListener(this);
        analyzer.execute();
    }
    
    public void buttonOpenActionPerformed(ActionEvent evt) {
        //throw new UnsupportedOperationException("Not yet implemented");
        JFileChooser chooser = new JFileChooser();
        
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Companion information file", "info");
         chooser.setFileFilter(filter);

        chooser.setCurrentDirectory(new File("C:\\"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 
            try {
                //setInfoTableWithVerify(chooser.getSelectedFile().getAbsolutePath());    
                File file = chooser.getSelectedFile();
                setCaseDir(file.getParent());
                setFileDir(file.getParent() + "\\Files");
                setIndexDir(file.getParent() + "\\Index");
                
                String line;
                BufferedReader br = new BufferedReader(new FileReader(file));
                while ((line = br.readLine()) != null) {
                    String value = line.substring(line.indexOf(":")+1).trim();
                    if (line.startsWith("Case")) {
                        setCaseName(value);
                        setCaseLabel(value);
                    } else if (line.startsWith("Language")) {
                        switch (value) {
                            case "English":
                                setDocLang(DocLang.English);
                                break;
                            case "Korean":
                                setDocLang(DocLang.Korean);
                                break;
                            case "Japanese":
                                setDocLang(DocLang.Japanese);
                                break;
                        }
                    } else if (line.startsWith("File List")) {
                        File f = new File(file.getParent(), value);
                        setInfoTable(f);
                        setFileListFile(f);
                    }
                }      
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
             buttonNewActionPerformed(ae);
        } else if("Open".equals(ae.getActionCommand())) {
             buttonOpenActionPerformed(ae);
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
            buttonSearchActionPerformed(ae);
        } else if("Keyword".equals(ae.getActionCommand())) {
            JOptionPane.showMessageDialog(this,"Keyword management function is under construction!");
        } else if("About".equals(ae.getActionCommand())) {
            JOptionPane.showMessageDialog(this, "Field Audtor's Companion for Korean Air\n"
                    + "- File Collector & Indexer\n"
                    + "- Developed by H.H.Kim (SELBI)\n- Ver 1.0 (2012.11)");
        }
    }
     
    public void buttonSearchActionPerformed(ActionEvent evt) {
        //Searcher searcher = new Searcher(indexDir, docLang);
        Searcher searcher = new Searcher();
        Image icon = Toolkit.getDefaultToolkit().getImage(getClass()
                .getResource("/compliance/images/search1.jpg"));
        searcher.setIconImage(icon);
        this.setState(Frame.ICONIFIED);
    }
    
    public void buttonResetActionPerformed(ActionEvent evt) {
        setStage(Stage.BEFORE_STARTED);
        Object[][] data = {};
        String[] columnNames = {};
        infoTable.setModel(new MyTableModel(data,columnNames));
        setAuditor("");
        setCaseName("");
    }
            
    public void setCaseLabel(String caseName) {
        lblCase.setText(caseName);
    }
    
    public String getCaseLabel() {
       return lblCase.getText();
    }
    
    public void setCaseDir(String dir) {
        caseDir = dir;
    }   
    
    public String getCaseDir() {
        return caseDir;
    }
    
    public void setFileDir(String dir) {
        fileDir = dir;
    }
    
    public String getFileDir() {
        return fileDir;
    }
    
    public void setIndexDir(String dir) {
        indexDir = dir;
    }
    
    public String getIndexDir() {
        return indexDir;
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
        lblStage.setText(stage.toString() + " ");
        
        switch (stage) {
            // new case, load case, analyze, copy, index, reset
            case BEFORE_STARTED:
                enableButtons(true, true, false, false, false, false);
                break;
            case CASE_CREATED:
                enableButtons(false, false, true, false, false, true);
                break;
            case CASE_LOADED:
                enableButtons(false, false, false, true, true, true);
                break;                
            case ANALYZE_COMPLETED:
                enableButtons(false, false, false, true, true, true);
                break;
            case COPY_COMPLETED:
                enableButtons(false, false, false, false, true, true);
                break;
            case INDEX_CREATED:
                enableButtons(false, false, false, false, false, true);
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
            boolean b5, boolean b6) {       
        if (b1 == true) { 
            buttonNew.setEnabled(true);
        } else { 
            buttonNew.setEnabled(false); 
        }
        if (b2 == true) { 
            buttonOpen.setEnabled(true);
        } else { 
            buttonOpen.setEnabled(false); 
        }               
        if (b3 == true) { 
            buttonAnalyze.setEnabled(true);
        } else { 
            buttonAnalyze.setEnabled(false); 
        }
        if (b4 == true) { 
            buttonCopy.setEnabled(true);
        } else { 
            buttonCopy.setEnabled(false); 
        }
 
        if (b5 == true) { 
            buttonIndex.setEnabled(true);
        } else { 
            buttonIndex.setEnabled(false); 
        }         
        if (b6 == true) { 
            buttonReset.setEnabled(true);
        } else { 
            buttonReset.setEnabled(false); 
        }        
    }
 
    public void setMessage(String msg) {
        statusLabel.setText(" " + msg);
    }
}
