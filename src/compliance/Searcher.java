/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compliance;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.DefaultCheckboxTreeCellRenderer;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingEvent;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingListener;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.kr.KoreanAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
/**
 *
 * @author hohkim
 */
public final class Searcher extends javax.swing.JFrame implements TreeCheckingListener, 
    TableModelListener, ListSelectionListener, ActionListener {

    private String queryString;
    private String indexDir;
    private Page page;
    private int pageSize = 10;
    private MyTableModel model;
    private IndexReader reader;
    private IndexSearcher searcher;
    private Analyzer analyzer;
    private TopDocs topDocs;
    private Query query;
    // Collector
    private CheckboxTree folderTree;
    private CheckboxTree fileTypeTree;
    private List<String> categories = Arrays.asList("Office", "Image", "A/V", "ETC"); 
    private List<String> offices = Arrays.asList("DOC","DOCX", "XLS", "XLSX", "PPT", "PPTX", "PDF");
    private List<String> images = Arrays.asList("JPEG","JPG", "GIF", "PNG", "TIFF"); 
    private List<String> avfiles = Arrays.asList("MP3","MP4"); 
    private List<String> etcfiles = Arrays.asList("TXT", "TEX", "CLS");
    private Register register;
    private MyTableData infoData;
    private String caseDir;
    private String fileDir;
    private String caseName = "";
    private File fileListFile;
    private File caseInfoFile;
    private String auditor = "";  
    private DocLang docLang = DocLang.English;
    private List<String> jobDirs = new ArrayList<String>();
    private List<String> jobFileTypes = new ArrayList<String>();
    private Stage stage;

    @Override
    public void actionPerformed(ActionEvent ae) {
       if("Make Case".equals(ae.getActionCommand())) {
             buttonNewActionPerformed(ae);
        } else if("Open Case".equals(ae.getActionCommand())) {
             buttonOpenActionPerformed(ae);
        } else if("Reset Case".equals(ae.getActionCommand())) {
             buttonResetActionPerformed(ae);
        } else if("Exit".equals(ae.getActionCommand())) {
             System.exit(0);
        } else if("Analyze".equals(ae.getActionCommand())) {  
            buttonAnalyzeActionPerformed(ae);
        } else if("Copy".equals(ae.getActionCommand())) {            
            buttonCopyActionPerformed(ae);
        } else if("Index".equals(ae.getActionCommand())) {
            buttonIndexActionPerformed(ae);
        } else if("About".equals(ae.getActionCommand())) {
            JOptionPane.showMessageDialog(this, "Field Audtor's Companion for Korean Air\n"
                    + "- File Collector & Indexer\n"
                    + "- Developed by H.H.Kim (SELBI)\n- Ver 1.0 (2012.11)");
        }        
    }

    @Override
    public void tableChanged(TableModelEvent tme) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public static enum DocLang {
        English, Korean, Japanese
    }
    public static enum Stage {
        BEFORE_STARTED, CASE_CREATED, ANALYZE_COMPLETED,
        COPY_COMPLETED, CASE_LOADED, INDEX_CREATED
    }
    
    /**
     * Creates new form Searcher
     */
    public Searcher()  {
        initComponents();
        initCollector();
        setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }
    
    public Searcher(String indexDir, DocLang docLang) throws IOException {
        this.indexDir = indexDir;
        initComponents();
        setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        hSP1s.setDividerLocation(450);
        
        // IndexSearcher
        reader = IndexReader.open(FSDirectory.open(new File(this.indexDir)));
        searcher = new IndexSearcher(reader);
        // Analizer
        if (docLang == DocLang.English) {
            analyzer = new StandardAnalyzer(Version.LUCENE_36);
        } else if (docLang == DocLang.Korean) {
            try {
                analyzer = new KoreanAnalyzer(Version.LUCENE_36);
            } catch (IOException ex) {
               System.out.println(ex);
            }
        } else if (docLang == DocLang.Japanese) {
            analyzer = new JapaneseAnalyzer(Version.LUCENE_36);
        }        
        // Page initialize
        page = new Page(1);
        page.setPageSize(pageSize);
        setVisible(true);
    }

    public void initCollector() {
        vSplitPane.setTopComponent(folderInitComponents());
        vSplitPane.setBottomComponent(fileTypeInitComponents());
        infoTable.getModel().addTableModelListener(this);
        infoTable.getSelectionModel().addListSelectionListener(this);
        initInfoTable();
        //hSP1s.setDividerLocation(450);
        setStage(Stage.BEFORE_STARTED);
                
    }
    
    public void initSearcher() {
        try {
            reader = IndexReader.open(FSDirectory.open(new File(this.indexDir)));
            searcher = new IndexSearcher(reader); 
        } catch (IOException e) {
            System.out.println(e);
        }       
        // Page initialize
        page = new Page(1);
        page.setPageSize(pageSize);
        //this.setIconImage(new ImageIcon("D:\\Software\\Icons\\audits.jpg").getImage());
        setNavigateButtonsDisable();  
        this.searchBotton.setEnabled(true);
        this.resetButton.setEnabled(true);
        resultTableColumnWidthAdjust();
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
        if (stage == Stage.ANALYZE_COMPLETED || stage == Stage.CASE_LOADED 
                || stage == Stage.COPY_COMPLETED) {
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
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainTabbedPane = new javax.swing.JTabbedPane();
        welcomeP = new javax.swing.JPanel();
        collectorP = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        statusLabel = new javax.swing.JLabel();
        lblStage = new javax.swing.JLabel();
        lblTargetDir = new javax.swing.JLabel();
        hSplitPane = new javax.swing.JSplitPane();
        jPanel4 = new javax.swing.JPanel();
        vSplitPane = new javax.swing.JSplitPane();
        jSPs = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel5 = new javax.swing.JPanel();
        lblCase = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        infoTable = new javax.swing.JTable();
        jPanel6 = new javax.swing.JPanel();
        buttonNew = new javax.swing.JButton();
        buttonOpen = new javax.swing.JButton();
        buttonAnalyze = new javax.swing.JButton();
        buttonCopy = new javax.swing.JButton();
        buttonIndex = new javax.swing.JButton();
        buttonReset = new javax.swing.JButton();
        searcherP = new javax.swing.JPanel();
        upperPs = new javax.swing.JPanel();
        jLabel2s = new javax.swing.JLabel();
        jSP2s = new javax.swing.JScrollPane();
        searchWords = new javax.swing.JTextArea();
        searchBotton = new javax.swing.JButton();
        indexInfoLabel = new javax.swing.JLabel();
        jLabel4s = new javax.swing.JLabel();
        resetButton = new javax.swing.JButton();
        midP = new javax.swing.JPanel();
        hSP1s = new javax.swing.JSplitPane();
        midLeftPanel = new javax.swing.JPanel();
        midLeftBottomP = new javax.swing.JPanel();
        firstButton = new javax.swing.JButton();
        beforeButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();
        lastButton = new javax.swing.JButton();
        pageText = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        resultTable = new javax.swing.JTable();
        pageLabel = new javax.swing.JLabel();
        tabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        contentsArea = new javax.swing.JEditorPane();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        metadataArea = new javax.swing.JEditorPane();
        bottomP = new javax.swing.JPanel();
        jLabel1s = new javax.swing.JLabel();
        mainMenuBar = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        exitMenuItem = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        makeCaseMenuItem = new javax.swing.JMenuItem();
        openCaseMenuItem = new javax.swing.JMenuItem();
        analyzeMenuItem = new javax.swing.JMenuItem();
        copyMenuItem = new javax.swing.JMenuItem();
        indexingMenuItem = new javax.swing.JMenuItem();
        resetCaseMenuItem = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        openIndexMenuItem = new javax.swing.JMenuItem();
        openKeywordMenuItem = new javax.swing.JMenuItem();
        saveKeywordMenuItem = new javax.swing.JMenuItem();
        resetSearchMenuItem = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        howToUseMenuItem = new javax.swing.JMenuItem();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Searcher");
        setBackground(new java.awt.Color(204, 204, 204));
        setPreferredSize(new java.awt.Dimension(1024, 768));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        mainTabbedPane.setFont(new java.awt.Font("맑은 고딕", 1, 12)); // NOI18N

        javax.swing.GroupLayout welcomePLayout = new javax.swing.GroupLayout(welcomeP);
        welcomeP.setLayout(welcomePLayout);
        welcomePLayout.setHorizontalGroup(
            welcomePLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1216, Short.MAX_VALUE)
        );
        welcomePLayout.setVerticalGroup(
            welcomePLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 869, Short.MAX_VALUE)
        );

        mainTabbedPane.addTab("Welcome", welcomeP);

        statusLabel.setText("jLabel2");

        lblStage.setForeground(new java.awt.Color(51, 153, 0));
        lblStage.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);

        lblTargetDir.setForeground(new java.awt.Color(0, 153, 0));
        lblTargetDir.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblTargetDir, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblStage, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGap(0, 10, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusLabel)
                    .addComponent(lblStage)
                    .addComponent(lblTargetDir)))
        );

        hSplitPane.setDividerLocation(300);

        vSplitPane.setDividerLocation(300);
        vSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jSPs.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(vSplitPane)
                    .addComponent(jSPs, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(vSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 784, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSPs, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        hSplitPane.setLeftComponent(jPanel4);

        lblCase.setFont(new java.awt.Font("맑은 고딕", 1, 20)); // NOI18N
        lblCase.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCase.setText("Case : Unknown");

        infoTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "SQ", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(infoTable);

        buttonNew.setText("New");
        buttonNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNewActionPerformed(evt);
            }
        });

        buttonOpen.setText("Open");
        buttonOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOpenActionPerformed(evt);
            }
        });

        buttonAnalyze.setText("Analyze");
        buttonAnalyze.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAnalyzeActionPerformed(evt);
            }
        });

        buttonCopy.setText("Copy");
        buttonCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCopyActionPerformed(evt);
            }
        });

        buttonIndex.setText("Index");
        buttonIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonIndexActionPerformed(evt);
            }
        });

        buttonReset.setText("Reset");
        buttonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap(134, Short.MAX_VALUE)
                .addComponent(buttonNew)
                .addGap(18, 18, 18)
                .addComponent(buttonOpen)
                .addGap(18, 18, 18)
                .addComponent(buttonAnalyze)
                .addGap(18, 18, 18)
                .addComponent(buttonCopy)
                .addGap(18, 18, 18)
                .addComponent(buttonIndex)
                .addGap(18, 18, 18)
                .addComponent(buttonReset)
                .addContainerGap(270, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonNew)
                    .addComponent(buttonOpen)
                    .addComponent(buttonAnalyze)
                    .addComponent(buttonCopy)
                    .addComponent(buttonIndex)
                    .addComponent(buttonReset))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblCase, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblCase, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 728, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        hSplitPane.setRightComponent(jPanel5);

        javax.swing.GroupLayout collectorPLayout = new javax.swing.GroupLayout(collectorP);
        collectorP.setLayout(collectorPLayout);
        collectorPLayout.setHorizontalGroup(
            collectorPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(hSplitPane)
        );
        collectorPLayout.setVerticalGroup(
            collectorPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, collectorPLayout.createSequentialGroup()
                .addComponent(hSplitPane)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        mainTabbedPane.addTab("Collector", collectorP);

        searcherP.setLayout(new java.awt.BorderLayout());

        upperPs.setBackground(new java.awt.Color(204, 204, 204));
        upperPs.setForeground(new java.awt.Color(255, 102, 102));

        jLabel2s.setFont(new java.awt.Font("맑은 고딕", 1, 12)); // NOI18N
        jLabel2s.setText("▶ 검색어");

        searchWords.setColumns(20);
        searchWords.setFont(new java.awt.Font("맑은 고딕", 0, 12)); // NOI18N
        searchWords.setRows(5);
        jSP2s.setViewportView(searchWords);

        searchBotton.setBackground(new java.awt.Color(204, 204, 204));
        searchBotton.setFont(new java.awt.Font("맑은 고딕", 1, 12)); // NOI18N
        searchBotton.setText("Search");
        searchBotton.setEnabled(false);
        searchBotton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchBottonActionPerformed(evt);
            }
        });

        indexInfoLabel.setBackground(new java.awt.Color(204, 204, 204));
        indexInfoLabel.setForeground(new java.awt.Color(51, 153, 0));
        indexInfoLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);

        jLabel4s.setFont(new java.awt.Font("맑은 고딕", 0, 12)); // NOI18N
        jLabel4s.setForeground(new java.awt.Color(0, 153, 0));
        jLabel4s.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel4s.setText("(예) 합의 회의 가격 인상 저지 +필수단어 -제외단어");

        resetButton.setBackground(new java.awt.Color(204, 204, 204));
        resetButton.setFont(new java.awt.Font("맑은 고딕", 1, 12)); // NOI18N
        resetButton.setText("Reset");
        resetButton.setEnabled(false);
        resetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout upperPsLayout = new javax.swing.GroupLayout(upperPs);
        upperPs.setLayout(upperPsLayout);
        upperPsLayout.setHorizontalGroup(
            upperPsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, upperPsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(upperPsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSP2s)
                    .addGroup(upperPsLayout.createSequentialGroup()
                        .addComponent(jLabel2s, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4s, javax.swing.GroupLayout.PREFERRED_SIZE, 287, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(searchBotton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(resetButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 174, Short.MAX_VALUE)
                        .addComponent(indexInfoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 510, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        upperPsLayout.setVerticalGroup(
            upperPsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(upperPsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(upperPsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(indexInfoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(upperPsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2s)
                        .addComponent(searchBotton)
                        .addComponent(jLabel4s)
                        .addComponent(resetButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSP2s, javax.swing.GroupLayout.DEFAULT_SIZE, 85, Short.MAX_VALUE))
        );

        searcherP.add(upperPs, java.awt.BorderLayout.PAGE_START);

        midP.setBackground(new java.awt.Color(204, 204, 204));
        midP.setPreferredSize(new java.awt.Dimension(1209, 600));

        hSP1s.setBackground(new java.awt.Color(0, 153, 204));
        hSP1s.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        hSP1s.setDividerLocation(450);

        midLeftBottomP.setBackground(new java.awt.Color(204, 204, 204));

        firstButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/first.jpg"))); // NOI18N
        firstButton.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 153, 0)));
        firstButton.setEnabled(false);
        firstButton.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/first1.jpg"))); // NOI18N
        firstButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                firstButtonActionPerformed(evt);
            }
        });

        beforeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/back.jpg"))); // NOI18N
        beforeButton.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 153, 51)));
        beforeButton.setEnabled(false);
        beforeButton.setMaximumSize(new java.awt.Dimension(77, 57));
        beforeButton.setMinimumSize(new java.awt.Dimension(77, 57));
        beforeButton.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/back1.jpg"))); // NOI18N
        beforeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                beforeButtonActionPerformed(evt);
            }
        });

        nextButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/foward.jpg"))); // NOI18N
        nextButton.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 153, 51)));
        nextButton.setEnabled(false);
        nextButton.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/foward1.jpg"))); // NOI18N
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });

        lastButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/last.jpg"))); // NOI18N
        lastButton.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 153, 0)));
        lastButton.setEnabled(false);
        lastButton.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/last1.jpg"))); // NOI18N
        lastButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lastButtonActionPerformed(evt);
            }
        });

        pageText.setFont(new java.awt.Font("맑은 고딕", 1, 14)); // NOI18N
        pageText.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pageText.setEnabled(false);
        pageText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pageTextActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout midLeftBottomPLayout = new javax.swing.GroupLayout(midLeftBottomP);
        midLeftBottomP.setLayout(midLeftBottomPLayout);
        midLeftBottomPLayout.setHorizontalGroup(
            midLeftBottomPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(midLeftBottomPLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(firstButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(beforeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(nextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lastButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pageText, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        midLeftBottomPLayout.setVerticalGroup(
            midLeftBottomPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, midLeftBottomPLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(midLeftBottomPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(firstButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pageText, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lastButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(nextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(beforeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        resultTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "SQ", "Path", "File Name"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        resultTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        resultTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        resultTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                resultTableMouseClicked(evt);
            }
        });
        resultTable.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                resultTableFocusGained(evt);
            }
        });
        resultTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                resultTablePropertyChange(evt);
            }
        });
        resultTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                resultTableKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                resultTableKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(resultTable);

        pageLabel.setBackground(new java.awt.Color(255, 153, 255));
        pageLabel.setFont(new java.awt.Font("맑은 고딕", 0, 12)); // NOI18N
        pageLabel.setText("Page:");

        javax.swing.GroupLayout midLeftPanelLayout = new javax.swing.GroupLayout(midLeftPanel);
        midLeftPanel.setLayout(midLeftPanelLayout);
        midLeftPanelLayout.setHorizontalGroup(
            midLeftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(midLeftPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(midLeftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(midLeftPanelLayout.createSequentialGroup()
                        .addComponent(midLeftBottomP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE))
                .addContainerGap())
        );
        midLeftPanelLayout.setVerticalGroup(
            midLeftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(midLeftPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 619, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(midLeftBottomP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        hSP1s.setLeftComponent(midLeftPanel);

        tabbedPane1.setBackground(new java.awt.Color(255, 255, 255));
        tabbedPane1.setFont(new java.awt.Font("맑은 고딕", 0, 12)); // NOI18N

        contentsArea.setContentType("text/html"); // NOI18N
        contentsArea.setFont(new java.awt.Font("맑은 고딕", 0, 12)); // NOI18N
        jScrollPane3.setViewportView(contentsArea);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 731, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 675, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabbedPane1.addTab("파일내용", jPanel1);

        metadataArea.setContentType("text/html"); // NOI18N
        metadataArea.setFont(new java.awt.Font("맑은 고딕", 0, 12)); // NOI18N
        jScrollPane4.setViewportView(metadataArea);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 731, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 731, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 685, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 685, Short.MAX_VALUE))
        );

        tabbedPane1.addTab("추가정보", jPanel2);

        hSP1s.setRightComponent(tabbedPane1);
        tabbedPane1.getAccessibleContext().setAccessibleName("");

        javax.swing.GroupLayout midPLayout = new javax.swing.GroupLayout(midP);
        midP.setLayout(midPLayout);
        midPLayout.setHorizontalGroup(
            midPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(midPLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(hSP1s)
                .addContainerGap())
        );
        midPLayout.setVerticalGroup(
            midPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(midPLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(hSP1s)
                .addGap(0, 0, 0))
        );

        searcherP.add(midP, java.awt.BorderLayout.CENTER);

        bottomP.setBackground(new java.awt.Color(204, 204, 204));
        bottomP.setLayout(new java.awt.BorderLayout());

        jLabel1s.setFont(new java.awt.Font("Showcard Gothic", 0, 12)); // NOI18N
        jLabel1s.setForeground(new java.awt.Color(0, 51, 102));
        jLabel1s.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1s.setText("Powered by Lucene");
        bottomP.add(jLabel1s, java.awt.BorderLayout.CENTER);

        searcherP.add(bottomP, java.awt.BorderLayout.PAGE_END);

        mainTabbedPane.addTab("Searcher", searcherP);

        jMenu1.setText("파일");

        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(exitMenuItem);

        mainMenuBar.add(jMenu1);

        jMenu5.setText("자료수집");

        makeCaseMenuItem.setText("Make Case");
        makeCaseMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                makeCaseMenuItemActionPerformed(evt);
            }
        });
        jMenu5.add(makeCaseMenuItem);

        openCaseMenuItem.setText("Open Case");
        openCaseMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openCaseMenuItemActionPerformed(evt);
            }
        });
        jMenu5.add(openCaseMenuItem);

        analyzeMenuItem.setText("Analyze");
        analyzeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                analyzeMenuItemActionPerformed(evt);
            }
        });
        jMenu5.add(analyzeMenuItem);

        copyMenuItem.setText("Copy");
        copyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyMenuItemActionPerformed(evt);
            }
        });
        jMenu5.add(copyMenuItem);

        indexingMenuItem.setText("Indexing");
        indexingMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                indexingMenuItemActionPerformed(evt);
            }
        });
        jMenu5.add(indexingMenuItem);

        resetCaseMenuItem.setText("Reset Case");
        resetCaseMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetCaseMenuItemActionPerformed(evt);
            }
        });
        jMenu5.add(resetCaseMenuItem);

        mainMenuBar.add(jMenu5);

        jMenu2.setText("검색");

        openIndexMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openIndexMenuItem.setText("Open Index");
        openIndexMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openIndexMenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(openIndexMenuItem);

        openKeywordMenuItem.setText("Open Keyword");
        jMenu2.add(openKeywordMenuItem);

        saveKeywordMenuItem.setText("Save Keyword");
        saveKeywordMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveKeywordMenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(saveKeywordMenuItem);

        resetSearchMenuItem.setText("Reset Search");
        resetSearchMenuItem.setToolTipText("");
        jMenu2.add(resetSearchMenuItem);

        mainMenuBar.add(jMenu2);

        jMenu3.setText("도움말");

        howToUseMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_H, java.awt.event.InputEvent.CTRL_MASK));
        howToUseMenuItem.setText("How to use");
        jMenu3.add(howToUseMenuItem);

        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(aboutMenuItem);

        mainMenuBar.add(jMenu3);

        setJMenuBar(mainMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainTabbedPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainTabbedPane)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void searchBottonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchBottonActionPerformed
        // TODO add your handling code here:
        page.setCurrentPageNo(1);
        queryString = searchWords.getText();
        try {
            search();
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }//GEN-LAST:event_searchBottonActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_formWindowClosed

    private void saveKeywordMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveKeywordMenuItemActionPerformed
        // TODO add your handling code here:
        resetButtonActionPerformed(evt);
    }//GEN-LAST:event_saveKeywordMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        JOptionPane.showMessageDialog(this, "Field Audtor's Companion for Korean Air\n"
                + "- Searcher\n"
                + "- Developed by H.H.Kim (SELBI)\n- Ver 1.0 (2012.11)");
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetButtonActionPerformed
        searchWords.setText(null);
        contentsArea.setText(null);
        metadataArea.setText(null);
        initResultTable();
        pageLabel.setText("Page:"); //resultTable 위의 Page표시
        pageText.setText(""); // navigation 버튼 우측의 Page 입력란
        setNavigateButtonsDisable();
        searchWords.requestFocus();
    }//GEN-LAST:event_resetButtonActionPerformed

    private void resultTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_resultTableMouseClicked
        int row = resultTable.getSelectedRow();
        if (evt.getClickCount() == 1) {
            resultTable.setSelectionForeground(Color.WHITE);
            setContentsAndMetaArea(row);
        } else if (evt.getClickCount() == 2) {            
            resultTable.setSelectionForeground(Color.YELLOW);
            openWithExternalProgram(row);
        }
    }//GEN-LAST:event_resultTableMouseClicked

    public void setContentsAndMetaArea(int row) {
        if (model.getValueAt(row, 0) == null) {
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        ScoreDoc scoreDoc = topDocs.scoreDocs[id - 1];
        Document d;
        try {
            d = searcher.doc(scoreDoc.doc);
            String contents = d.get("contents");
            String[] fragments = MyUtil.getFragmentsWithHighlightedTerms(analyzer, query, "contents", contents, 100, 100);
            String highlighted = "";
            for (String frag : fragments) {
                highlighted += frag;
            }
            contentsArea.setText(highlighted.replaceAll("\n", "<br/>"));
            // Metadata
            String metaString = "";
            String appName = d.get("application-name");
            String fileSize = d.get("file-size");
            String pageCount = d.get("page-count");
            String md5origin = d.get("md5");
            File currentFile = new File(new File(indexDir, d.get("path")),
                    d.get("file-name"));
            String md5current = MyUtil.getHashCode(currentFile);

            String author = d.get("author");
            String creationDate = d.get("creation-date");
            String lastAuthor = d.get("last-author");
            String lastModified = d.get("last-modified");

            metaString += String.format("<ul><li>작성 프로그램: %s</li>", appName);
            metaString += String.format("    <li>파일 크기 : %,d (KB)</li>", Long.parseLong(fileSize) / 1024);
            metaString += String.format("    <li>페이지 수 : %s</li></ul>", pageCount);

            metaString += String.format("<ul><li>원본 파일 MD5 해시값: %s</li>", md5origin);
            metaString += String.format("    <li>현재 파일 MD5 해시값: %s</li>", md5current);
            if (MyUtil.verifyHash(currentFile.getAbsolutePath(), md5origin)) {
                metaString += "<li>변동여부 검증: <b>OK! 원본과 일치</b></li></ul>";
            } else {
                metaString += "<li>변동여부 검증: <b>FAIL! 원본과 다름</b></li></ul>";
            }
            metaString += String.format("<ul><li>파일 작성자: %s</li>", author);
            metaString += String.format("    <li>최종 수정자: %s</li>", lastAuthor);
            metaString += String.format("    <li>파일 생성일: %s</li>", creationDate);
            metaString += String.format("    <li>최종 수정일: %s</li></ul>", lastModified);
            metadataArea.setText(metaString);
        } catch (CorruptIndexException ex) {
            System.out.println(ex);
        } catch (IOException ex) {
            System.out.println(ex);
        }
        contentsArea.setCaretPosition(0);
    }
    
    public void openWithExternalProgram(int row) {
        if (model.getValueAt(row, 0) == null) {
            return;
        }
        // Open external program
        String path = (String) model.getValueAt(row, 1);
        File file;
        try {
            if (path.equals("..\\Files")) {
                String docDir = indexDir.replace("Index", "Files");
                String fn = (String) model.getValueAt(row, 2);
                file = new File(docDir, fn);
            } else {
                String fn = (String) model.getValueAt(row, 2);
                file = new File(path, fn);
            }
            Desktop.getDesktop().open(file);
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }
    
    private void pageTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pageTextActionPerformed
        int pageNo = page.getCurrentPageNo();
        try {
            pageNo = Integer.parseInt(pageText.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Please input numbers.", "Numeric Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (pageNo < 1 || pageNo > page.getPageCount()) {
            JOptionPane.showMessageDialog(null, "Page range is out of bound.", "Page No Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        page.setCurrentPageNo(pageNo);
        try {
            search();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }//GEN-LAST:event_pageTextActionPerformed

    private void lastButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lastButtonActionPerformed
        page.setCurrentPageNo(page.getPageCount());
        try {
            search();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }//GEN-LAST:event_lastButtonActionPerformed

    private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
        page.setCurrentPageNo(page.getCurrentPageNo()+1);
        try {
            search();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }//GEN-LAST:event_nextButtonActionPerformed

    private void beforeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_beforeButtonActionPerformed
        page.setCurrentPageNo(page.getCurrentPageNo()-1);
        try {
            search();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }//GEN-LAST:event_beforeButtonActionPerformed

    private void firstButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_firstButtonActionPerformed
        page.setCurrentPageNo(1);
        try {
            search();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }//GEN-LAST:event_firstButtonActionPerformed

    private void resultTableKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_resultTableKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_resultTableKeyPressed

    private void resultTablePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_resultTablePropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_resultTablePropertyChange

    private void resultTableFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_resultTableFocusGained
        // TODO add your handling code here:
    }//GEN-LAST:event_resultTableFocusGained

    private void resultTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_resultTableKeyReleased
        // TODO add your handling code here:
        resultTable.setSelectionForeground(Color.WHITE);
        setContentsAndMetaArea(resultTable.getSelectedRow());
    }//GEN-LAST:event_resultTableKeyReleased

    private void buttonNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNewActionPerformed
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
    }//GEN-LAST:event_buttonNewActionPerformed

    private void buttonOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOpenActionPerformed
        // TODO add your handling code here:
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
    }//GEN-LAST:event_buttonOpenActionPerformed

    private void buttonAnalyzeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAnalyzeActionPerformed
        // TODO add your handling code here:
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
        compliance.Analyzer analyzer = new compliance.Analyzer(jobDirs, jobFileTypes, caseDir, fileListFile, this);   
        analyzer.setParent(this);
        //analyzer.addPropertyChangeListener(this);
        analyzer.execute();        
    }//GEN-LAST:event_buttonAnalyzeActionPerformed

    private void buttonCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCopyActionPerformed
       setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        model = (MyTableModel) infoTable.getModel();
        Copier copier = new Copier(model, this);
        //copier.addPropertyChangeListener(this);
        copier.execute();
    }//GEN-LAST:event_buttonCopyActionPerformed

    private void buttonIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonIndexActionPerformed
        // TODO add your handling code here:
       setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
       if (stage == Stage.COPY_COMPLETED) {
           Indexer indexer = new Indexer(indexDir, fileDir, docLang, this);
           //indexer.addPropertyChangeListener(this);
           indexer.execute();
       } else if (stage == Stage.ANALYZE_COMPLETED || stage == Stage.CASE_LOADED) {
           model = (MyTableModel) infoTable.getModel();
           IndexerB indexerB = new IndexerB(indexDir, model, docLang, this);
           //indexerB.addPropertyChangeListener(this);
           indexerB.execute();           
       }
    }//GEN-LAST:event_buttonIndexActionPerformed

    private void buttonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetActionPerformed
        // TODO add your handling code here:
        setStage(Stage.BEFORE_STARTED);
        Object[][] data = {};
        String[] columnNames = {};
        infoTable.setModel(new MyTableModel(data,columnNames));
        setAuditor("");
        setCaseName("");        
    }//GEN-LAST:event_buttonResetActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void makeCaseMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_makeCaseMenuItemActionPerformed
        buttonNewActionPerformed(evt);
        mainTabbedPane.setSelectedIndex(1);
    }//GEN-LAST:event_makeCaseMenuItemActionPerformed

    private void openCaseMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openCaseMenuItemActionPerformed
        buttonOpenActionPerformed(evt);
        mainTabbedPane.setSelectedIndex(1);
    }//GEN-LAST:event_openCaseMenuItemActionPerformed

    private void analyzeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_analyzeMenuItemActionPerformed
        buttonAnalyzeActionPerformed(evt);
    }//GEN-LAST:event_analyzeMenuItemActionPerformed

    private void copyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyMenuItemActionPerformed
        buttonCopyActionPerformed(evt);
    }//GEN-LAST:event_copyMenuItemActionPerformed

    private void indexingMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_indexingMenuItemActionPerformed
        buttonIndexActionPerformed(evt);
    }//GEN-LAST:event_indexingMenuItemActionPerformed

    private void resetCaseMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetCaseMenuItemActionPerformed
        buttonResetActionPerformed(evt);
    }//GEN-LAST:event_resetCaseMenuItemActionPerformed

    private void openIndexMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openIndexMenuItemActionPerformed
        // TODO add your handling code here:
        IndexOpenDialog dlg = new IndexOpenDialog(this, true);
        dlg.setLocation((this.getWidth()-dlg.getWidth())/2, 
                (this.getHeight()-dlg.getHeight())/2);
        dlg.setVisible(true);
        mainTabbedPane.setSelectedIndex(2);
    }//GEN-LAST:event_openIndexMenuItemActionPerformed

    public void search () throws IOException {
        QueryParser parser = new QueryParser(Version.LUCENE_36, "contents", analyzer);
        try {
            query = parser.parse(queryString);
        } catch (ParseException ex) {
            System.out.println(ex);
            JOptionPane.showMessageDialog(this, "질의어를 해석할 수 없습니다\n질의어를 다시 확인해 주세요.",
                    "질의어 해석 오류", JOptionPane.ERROR_MESSAGE);

            return;
        }
        
        try {
            topDocs = searcher.search(query, page.getEnd());
        } catch (IOException ex) {
            System.out.println(ex);
            return;
        }
        if (topDocs.totalHits == 0) {
            pageLabel.setText("Page: 0/0, (Total Hits: 0)"); 
            return;
        }
        page.setTotalCount(topDocs.totalHits);
        System.out.format("Total hits: %d, Total Page: %d\n",
                topDocs.totalHits, page.getPageCount());

        System.out.println("Current page: " + page.getCurrentPageNo());
        // for tablemodel
        int rowCount = (int) Math.min(page.getPageSize(), 
                (page.getTotalCount() - (page.getCurrentPageNo()-1) * page.getPageSize()));
        Object[][] data = new Object[rowCount][3];
        //Object[][] data = new Object[page.getPageSize()][3];
        String[] columnNames = {"SQ", "Path", "File Name" };
        for (int i = page.getStart(); i < page.getEnd(); i++) {
            ScoreDoc scoreDoc = topDocs.scoreDocs[i];
            Document d = searcher.doc(scoreDoc.doc);
            System.out.format(" %3d. (%.5f) %s\n", i + 1, scoreDoc.score, d.get("file-name"));
            //contents.append(String.format(" %3d. (%.5f) %s\n", i + 1, scoreDoc.score, d.get("filename")));
            data[i-page.getStart()][0] = i+1;
            data[i-page.getStart()][1] = d.get("path");
            data[i-page.getStart()][2] = d.get("file-name");
        }
        pageLabel.setText(String.format("Page: %d/%d, (Total Hits: %d)", 
                page.getCurrentPageNo(), page.getPageCount(), page.getTotalCount()));
        model = new MyTableModel(data, columnNames);
        resultTable.setModel(model);
        resultTable.requestFocus();
        resultTable.setRowSelectionInterval(0, 0);
        setContentsAndMetaArea(0);
        resultTableColumnWidthAdjust();
        setNavigateButtonsState();
	}
    
    public void initInfoTable() {
        Object[][] data = { {"", "", "", "", "" } };
        String[] columnNames = {"Select", "File Name", "Size(KB)", "Modified", "Hash Code"};
        model = new MyTableModel(data, columnNames);
        infoTable.setModel(model);
    }
    
    public void initResultTable() {
        Object[][] data = { {"", "", "" } };
        String[] columnNames = {"SQ", "Path", "File Name" };
        model = new MyTableModel(data, columnNames);
        resultTable.setModel(model);
        resultTableColumnWidthAdjust();
    }
    
    public void resultTableColumnWidthAdjust () {
        resultTable.getColumnModel().getColumn(0).setMaxWidth(40);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(250);
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
            java.util.logging.Logger.getLogger(Searcher.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Searcher.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Searcher.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Searcher.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
               // try {
                    //Searcher searcher = new Searcher("F:\\Binder\\Index", DocLang.Korean);
                    Searcher searcher = new Searcher();
                    Image icon = Toolkit.getDefaultToolkit().getImage(getClass()
                            .getResource("/compliance/images/search1.jpg"));
                    searcher.setIconImage(icon);
                    searcher.initResultTable();
                    searcher.setVisible(true);
               // } catch (IOException ex) {
               //     System.out.println(ex);
               // }
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JMenuItem analyzeMenuItem;
    private javax.swing.JButton beforeButton;
    private javax.swing.JPanel bottomP;
    private javax.swing.JButton buttonAnalyze;
    private javax.swing.JButton buttonCopy;
    private javax.swing.JButton buttonIndex;
    private javax.swing.JButton buttonNew;
    private javax.swing.JButton buttonOpen;
    private javax.swing.JButton buttonReset;
    private javax.swing.JPanel collectorP;
    private javax.swing.JEditorPane contentsArea;
    private javax.swing.JMenuItem copyMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JButton firstButton;
    private javax.swing.JSplitPane hSP1s;
    private javax.swing.JSplitPane hSplitPane;
    private javax.swing.JMenuItem howToUseMenuItem;
    private javax.swing.JLabel indexInfoLabel;
    private javax.swing.JMenuItem indexingMenuItem;
    private javax.swing.JTable infoTable;
    private javax.swing.JLabel jLabel1s;
    private javax.swing.JLabel jLabel2s;
    private javax.swing.JLabel jLabel4s;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jSP2s;
    private javax.swing.JScrollPane jSPs;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JButton lastButton;
    private javax.swing.JLabel lblCase;
    private javax.swing.JLabel lblStage;
    private javax.swing.JLabel lblTargetDir;
    private javax.swing.JMenuBar mainMenuBar;
    private javax.swing.JTabbedPane mainTabbedPane;
    private javax.swing.JMenuItem makeCaseMenuItem;
    private javax.swing.JEditorPane metadataArea;
    private javax.swing.JPanel midLeftBottomP;
    private javax.swing.JPanel midLeftPanel;
    private javax.swing.JPanel midP;
    private javax.swing.JButton nextButton;
    private javax.swing.JMenuItem openCaseMenuItem;
    private javax.swing.JMenuItem openIndexMenuItem;
    private javax.swing.JMenuItem openKeywordMenuItem;
    private javax.swing.JLabel pageLabel;
    private javax.swing.JTextField pageText;
    private javax.swing.JButton resetButton;
    private javax.swing.JMenuItem resetCaseMenuItem;
    private javax.swing.JMenuItem resetSearchMenuItem;
    private javax.swing.JTable resultTable;
    private javax.swing.JMenuItem saveKeywordMenuItem;
    private javax.swing.JButton searchBotton;
    private javax.swing.JTextArea searchWords;
    private javax.swing.JPanel searcherP;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JTabbedPane tabbedPane1;
    private javax.swing.JPanel upperPs;
    private javax.swing.JSplitPane vSplitPane;
    private javax.swing.JPanel welcomeP;
    // End of variables declaration//GEN-END:variables

    private void setNavigateButtonsState() {
        int curPage = page.getCurrentPageNo();
        if (curPage == 1) {  // first page
            firstButton.setEnabled(false);
            beforeButton.setEnabled(false);
            nextButton.setEnabled(true);
            lastButton.setEnabled(true);
            pageText.setEnabled(true);
        } else if (curPage == page.getPageCount()) { // last page
            firstButton.setEnabled(true);
            beforeButton.setEnabled(true);
            nextButton.setEnabled(false);
            lastButton.setEnabled(false);
            pageText.setEnabled(true);
        } else {
            firstButton.setEnabled(true);
            beforeButton.setEnabled(true);
            nextButton.setEnabled(true);
            lastButton.setEnabled(true);
            pageText.setEnabled(true);
        }
    }
    
    private void setNavigateButtonsDisable() {
        firstButton.setEnabled(false);
        beforeButton.setEnabled(false);
        nextButton.setEnabled(false);
        lastButton.setEnabled(false);
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public void setAnalyzer (String language) {
        try {
            switch (language) {
                case "Korean":
                    analyzer = new KoreanAnalyzer(Version.LUCENE_36);
                    break;
                case "English":
                    analyzer = new StandardAnalyzer(Version.LUCENE_36);
                    break;
                case "Japanese":
                    analyzer = new JapaneseAnalyzer(Version.LUCENE_36);
                    break;
            }
        } catch (IOException e) {
            System.out.println(e);
            return;
        }
    }
    
    public void setIndexInfolabel(String info) {
        this.indexInfoLabel.setText(info);
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
