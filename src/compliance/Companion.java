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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
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
import sun.swing.table.DefaultTableCellHeaderRenderer;
/**
 *
 * @author hohkim
 */
public final class Companion extends javax.swing.JFrame implements TreeCheckingListener, 
    TableModelListener, ListSelectionListener {

    public Searcher searcher;
    public final static Logger logger = Logger.getLogger(Companion.class.getName());
    //private String queryString;
    private String indexDir;
    private MyTableModel model;
    //private Scanner analyzer;
    // Collector
    private CheckboxTree folderTree;
    private CheckboxTree fileTypeTree;
    private List<String> categories = Arrays.asList("Office", "Image", "A/V", "ETC"); 
    private List<String> offices = Arrays.asList("DOC","DOCX", "XLS", "XLSX", "PPT", "PPTX", "PDF");
    private List<String> images = Arrays.asList("BMP", "GIF", "JPEG","JPG", "PNG", "TIFF"); 
    private List<String> avfiles = Arrays.asList("AVI", "MP3","MP4", "WAV", "WMV", "WMA"); 
    private List<String> etcfiles = Arrays.asList("HTML","HTM", "HWP", "MDB", "RTF","TEX","TXT","XML","PST","ZIP");
    private Register register;
    private MyTableData infoData;
    private String caseDir;
    private String fileDir;
    private String caseName = "";
    private File fileListFile;
    private File caseInfoFile;
    private String auditor = "";  
    private DocLang docLang;
    private List<String> jobDirs = new ArrayList<>();
    private List<String> jobFileTypes = new ArrayList<>();
    private Stage stage;

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
     * Creates new form Companion
     */
    public Companion()  {
        searcher = new Searcher(this);
        initComponents();
        initCollector();
        initAdvisor();
        setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }
    
    // 화면 초기화 (Treeview) 시작
    public void initCollector() {
        vSplitPane.setTopComponent(folderInitComponents());
        vSplitPane.setBottomComponent(fileTypeInitComponents());
        infoTable.getModel().addTableModelListener(this);
        infoTable.getSelectionModel().addListSelectionListener(this);
        initInfoTable();
        //hSP1s.setDividerLocation(450);
        setStage(Stage.BEFORE_STARTED);
                
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
    // 화면 초기화 (Treeview) 끝
    
    // Initialize Logger
    public void initLogger(String dir) throws IOException {
        logger.setLevel(Level.WARNING);
        FileHandler handler = new FileHandler(dir + "\\log.txt");
        SimpleFormatter formatter = new SimpleFormatter();
        handler.setFormatter(formatter);
        logger.addHandler(handler);
    }
    
    public void initAdvisor() {
        java.net.URL helpURL1 = Companion.class.getResource("HowToUseBasic.html");
        java.net.URL helpURL2 = Companion.class.getResource("HowToUseCollect.html");
        java.net.URL helpURL3 = Companion.class.getResource("HowToUseSearch.html");
        java.net.URL helpURL4 = Companion.class.getResource("HowToUseQuerySyntax.html");
        if (helpURL1 != null && helpURL2 != null) {
            try {
                advisorBasic.setPage(helpURL1);
                advisorCollect.setPage(helpURL2);
                advisorSearch.setPage(helpURL3);
                advisorQuerySyntax.setPage(helpURL4);                    
            } catch (IOException e) {
                System.err.println("Attempted to read a bad URL: " + e);
            }
        } else {
            System.err.println("Couldn't find help file");
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
        searcherP = new javax.swing.JPanel();
        upperPs = new javax.swing.JPanel();
        jLabel2s = new javax.swing.JLabel();
        jSP2s = new javax.swing.JScrollPane();
        searchWords = new javax.swing.JTextArea();
        buttonSearch = new javax.swing.JButton();
        indexInfoLabel = new javax.swing.JLabel();
        jLabel4s = new javax.swing.JLabel();
        buttonClear = new javax.swing.JButton();
        buttonOpenIndex = new javax.swing.JButton();
        buttonKeywordLoad = new javax.swing.JButton();
        buttonKeywordSave = new javax.swing.JButton();
        midP = new javax.swing.JPanel();
        hSP1s = new javax.swing.JSplitPane();
        midLeftPanel = new javax.swing.JPanel();
        midLeftBottomP = new javax.swing.JPanel();
        buttonFirst = new javax.swing.JButton();
        buttonBefore = new javax.swing.JButton();
        buttonNext = new javax.swing.JButton();
        buttonLast = new javax.swing.JButton();
        pageText = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        resultTable = new javax.swing.JTable();
        pageLabel = new javax.swing.JLabel();
        tabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        contentsArea = new javax.swing.JEditorPane();
        jScrollPane9 = new javax.swing.JScrollPane();
        fragmentTable = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        metadataArea = new javax.swing.JEditorPane();
        bottomP = new javax.swing.JPanel();
        jLabel1s = new javax.swing.JLabel();
        collectorP = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        statusLabel = new javax.swing.JLabel();
        lblStage = new javax.swing.JLabel();
        lblTargetDir = new javax.swing.JLabel();
        hSplitPane = new javax.swing.JSplitPane();
        jPanel4 = new javax.swing.JPanel();
        vSplitPane = new javax.swing.JSplitPane();
        jSPs = new javax.swing.JScrollPane();
        etcFileTypes = new javax.swing.JTextArea();
        jPanel5 = new javax.swing.JPanel();
        lblCase = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        infoTable = new javax.swing.JTable();
        jPanel6 = new javax.swing.JPanel();
        buttonNew = new javax.swing.JButton();
        buttonOpen = new javax.swing.JButton();
        buttonScan = new javax.swing.JButton();
        buttonCopy = new javax.swing.JButton();
        buttonIndex = new javax.swing.JButton();
        buttonReset = new javax.swing.JButton();
        advisorP = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        advisorBasic = new javax.swing.JEditorPane();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        advisorCollect = new javax.swing.JEditorPane();
        jPanel9 = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        advisorSearch = new javax.swing.JEditorPane();
        jPanel10 = new javax.swing.JPanel();
        jScrollPane8 = new javax.swing.JScrollPane();
        advisorQuerySyntax = new javax.swing.JEditorPane();
        mainMenuBar = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        menuItemExit = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        menuItemMakeCase = new javax.swing.JMenuItem();
        menuItemOpenCase = new javax.swing.JMenuItem();
        menuItemAnalyze = new javax.swing.JMenuItem();
        menuItemCopy = new javax.swing.JMenuItem();
        menuItemIndexing = new javax.swing.JMenuItem();
        menuItemResetCase = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        menuItemOpenIndex = new javax.swing.JMenuItem();
        menuItemSearch = new javax.swing.JMenuItem();
        menuItemClear = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        menuItemKeywordLoad = new javax.swing.JMenuItem();
        menuItemKeywordSave = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        menuItemAbout = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Auditor's Companion for Korean Air");
        setBackground(new java.awt.Color(204, 204, 204));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        mainTabbedPane.setFont(new java.awt.Font("맑은 고딕", 1, 12)); // NOI18N

        searcherP.setLayout(new java.awt.BorderLayout());

        upperPs.setBackground(new java.awt.Color(204, 204, 204));
        upperPs.setForeground(new java.awt.Color(255, 102, 102));

        jLabel2s.setFont(new java.awt.Font("맑은 고딕", 1, 12)); // NOI18N
        jLabel2s.setText("▶ 검색어");

        searchWords.setColumns(20);
        searchWords.setFont(new java.awt.Font("맑은 고딕", 0, 12)); // NOI18N
        searchWords.setRows(5);
        jSP2s.setViewportView(searchWords);

        buttonSearch.setFont(new java.awt.Font("맑은 고딕", 1, 12)); // NOI18N
        buttonSearch.setText("Search");
        buttonSearch.setEnabled(false);
        buttonSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSearchActionPerformed(evt);
            }
        });

        indexInfoLabel.setBackground(new java.awt.Color(204, 204, 204));
        indexInfoLabel.setForeground(new java.awt.Color(0, 153, 51));
        indexInfoLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        indexInfoLabel.setText("INDEX: NOT OPENED");

        jLabel4s.setFont(new java.awt.Font("맑은 고딕", 0, 12)); // NOI18N
        jLabel4s.setForeground(new java.awt.Color(0, 153, 0));
        jLabel4s.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel4s.setText("(예) 합의 회의 가격 인상 저지 +필수단어 -제외단어 필드이름:검색어");

        buttonClear.setFont(new java.awt.Font("맑은 고딕", 1, 12)); // NOI18N
        buttonClear.setText("Clear");
        buttonClear.setEnabled(false);
        buttonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonClearActionPerformed(evt);
            }
        });

        buttonOpenIndex.setFont(new java.awt.Font("맑은 고딕", 1, 12)); // NOI18N
        buttonOpenIndex.setText("Open Index");
        buttonOpenIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOpenIndexActionPerformed(evt);
            }
        });

        buttonKeywordLoad.setFont(new java.awt.Font("맑은 고딕", 1, 12)); // NOI18N
        buttonKeywordLoad.setText("Load");
        buttonKeywordLoad.setEnabled(false);
        buttonKeywordLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonKeywordLoadActionPerformed(evt);
            }
        });

        buttonKeywordSave.setFont(new java.awt.Font("맑은 고딕", 1, 12)); // NOI18N
        buttonKeywordSave.setText("Save");
        buttonKeywordSave.setEnabled(false);
        buttonKeywordSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonKeywordSaveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout upperPsLayout = new javax.swing.GroupLayout(upperPs);
        upperPs.setLayout(upperPsLayout);
        upperPsLayout.setHorizontalGroup(
            upperPsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, upperPsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(upperPsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSP2s)
                    .addGroup(upperPsLayout.createSequentialGroup()
                        .addComponent(buttonSearch)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonClear)
                        .addGap(6, 6, 6)
                        .addComponent(buttonOpenIndex)
                        .addGap(27, 27, 27)
                        .addComponent(jLabel2s, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4s, javax.swing.GroupLayout.PREFERRED_SIZE, 382, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonKeywordLoad)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonKeywordSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 53, Short.MAX_VALUE)
                        .addComponent(indexInfoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        upperPsLayout.setVerticalGroup(
            upperPsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(upperPsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(upperPsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(indexInfoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(upperPsLayout.createSequentialGroup()
                        .addGroup(upperPsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2s)
                            .addComponent(buttonSearch)
                            .addComponent(jLabel4s)
                            .addComponent(buttonOpenIndex)
                            .addComponent(buttonClear)
                            .addComponent(buttonKeywordLoad)
                            .addComponent(buttonKeywordSave))
                        .addGap(0, 0, Short.MAX_VALUE)))
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

        buttonFirst.setIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/first.jpg"))); // NOI18N
        buttonFirst.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 153, 0)));
        buttonFirst.setEnabled(false);
        buttonFirst.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/first1.jpg"))); // NOI18N
        buttonFirst.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFirstActionPerformed(evt);
            }
        });

        buttonBefore.setIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/back.jpg"))); // NOI18N
        buttonBefore.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 153, 51)));
        buttonBefore.setEnabled(false);
        buttonBefore.setMaximumSize(new java.awt.Dimension(77, 57));
        buttonBefore.setMinimumSize(new java.awt.Dimension(77, 57));
        buttonBefore.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/back1.jpg"))); // NOI18N
        buttonBefore.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonBeforeActionPerformed(evt);
            }
        });

        buttonNext.setIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/foward.jpg"))); // NOI18N
        buttonNext.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 153, 51)));
        buttonNext.setEnabled(false);
        buttonNext.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/foward1.jpg"))); // NOI18N
        buttonNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNextActionPerformed(evt);
            }
        });

        buttonLast.setIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/last.jpg"))); // NOI18N
        buttonLast.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 153, 0)));
        buttonLast.setEnabled(false);
        buttonLast.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/compliance/images/last1.jpg"))); // NOI18N
        buttonLast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLastActionPerformed(evt);
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
                .addComponent(buttonFirst, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(buttonBefore, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(buttonNext, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(buttonLast, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pageText, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        midLeftBottomPLayout.setVerticalGroup(
            midLeftBottomPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, midLeftBottomPLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(midLeftBottomPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(buttonFirst, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pageText, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonLast, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonNext, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonBefore, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
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
        contentsArea.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n    <p style=\"margin-top: 0\">\r\n\n    </p>\r\n  </body>\r\n</html>\r\n");
        jScrollPane3.setViewportView(contentsArea);

        fragmentTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "찾은 글 조각"
            }
        ));
        fragmentTable.setCellSelectionEnabled(true);
        fragmentTable.setName(""); // NOI18N
        fragmentTable.setRowHeight(70);
        fragmentTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fragmentTableMouseClicked(evt);
            }
        });
        jScrollPane9.setViewportView(fragmentTable);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 493, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane9, javax.swing.GroupLayout.PREFERRED_SIZE, 261, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 675, Short.MAX_VALUE)
                    .addComponent(jScrollPane9))
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
            .addGap(0, 761, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 761, Short.MAX_VALUE))
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

        statusLabel.setText("WELCOME!");

        lblStage.setForeground(new java.awt.Color(51, 153, 0));
        lblStage.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblStage.setText("BEFORE_STARTED");

        lblTargetDir.setForeground(new java.awt.Color(0, 153, 0));
        lblTargetDir.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblTargetDir.setText("DIR: N/A");

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
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusLabel)
                    .addComponent(lblStage)
                    .addComponent(lblTargetDir)))
        );

        hSplitPane.setDividerLocation(300);

        vSplitPane.setDividerLocation(300);
        vSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        etcFileTypes.setColumns(20);
        etcFileTypes.setRows(5);
        etcFileTypes.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                etcFileTypesFocusLost(evt);
            }
        });
        jSPs.setViewportView(etcFileTypes);

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
                .addComponent(vSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 794, Short.MAX_VALUE)
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

        buttonScan.setText("Scan");
        buttonScan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonScanActionPerformed(evt);
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
                .addContainerGap(158, Short.MAX_VALUE)
                .addComponent(buttonNew)
                .addGap(18, 18, 18)
                .addComponent(buttonOpen)
                .addGap(18, 18, 18)
                .addComponent(buttonScan)
                .addGap(18, 18, 18)
                .addComponent(buttonCopy)
                .addGap(18, 18, 18)
                .addComponent(buttonIndex)
                .addGap(18, 18, 18)
                .addComponent(buttonReset)
                .addContainerGap(294, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonNew)
                    .addComponent(buttonOpen)
                    .addComponent(buttonScan)
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
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(lblCase, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblCase, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 738, Short.MAX_VALUE)
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

        advisorBasic.setContentType("text/html"); // NOI18N
        jScrollPane5.setViewportView(advisorBasic);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 1193, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("기본 정보", jPanel7);

        advisorCollect.setContentType("text/html"); // NOI18N
        jScrollPane6.setViewportView(advisorCollect);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 1193, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("문서수집 방법", jPanel8);

        advisorSearch.setContentType("text/html"); // NOI18N
        jScrollPane7.setViewportView(advisorSearch);

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 1193, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("텍스트검색 방법", jPanel9);

        advisorQuerySyntax.setContentType("text/html"); // NOI18N
        advisorQuerySyntax.setToolTipText("");
        jScrollPane8.setViewportView(advisorQuerySyntax);

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 1193, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("질의어 문법", jPanel10);

        javax.swing.GroupLayout advisorPLayout = new javax.swing.GroupLayout(advisorP);
        advisorP.setLayout(advisorPLayout);
        advisorPLayout.setHorizontalGroup(
            advisorPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(advisorPLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );
        advisorPLayout.setVerticalGroup(
            advisorPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(advisorPLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );

        mainTabbedPane.addTab("Advisor", advisorP);

        mainTabbedPane.setSelectedIndex(2);

        jMenu1.setText("파일");

        menuItemExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        menuItemExit.setText("Exit");
        menuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemExitActionPerformed(evt);
            }
        });
        jMenu1.add(menuItemExit);

        mainMenuBar.add(jMenu1);

        jMenu5.setText("수집");

        menuItemMakeCase.setText("New");
        menuItemMakeCase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemMakeCaseActionPerformed(evt);
            }
        });
        jMenu5.add(menuItemMakeCase);

        menuItemOpenCase.setText("Open");
        menuItemOpenCase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemOpenCaseActionPerformed(evt);
            }
        });
        jMenu5.add(menuItemOpenCase);

        menuItemAnalyze.setText("Analyze");
        menuItemAnalyze.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemAnalyzeActionPerformed(evt);
            }
        });
        jMenu5.add(menuItemAnalyze);

        menuItemCopy.setText("Copy");
        menuItemCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemCopyActionPerformed(evt);
            }
        });
        jMenu5.add(menuItemCopy);

        menuItemIndexing.setText("Index");
        menuItemIndexing.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemIndexingActionPerformed(evt);
            }
        });
        jMenu5.add(menuItemIndexing);

        menuItemResetCase.setText("Reset");
        menuItemResetCase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemResetCaseActionPerformed(evt);
            }
        });
        jMenu5.add(menuItemResetCase);

        mainMenuBar.add(jMenu5);

        jMenu2.setText("검색");

        menuItemOpenIndex.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        menuItemOpenIndex.setText("Open Index");
        menuItemOpenIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemOpenIndexActionPerformed(evt);
            }
        });
        jMenu2.add(menuItemOpenIndex);

        menuItemSearch.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        menuItemSearch.setText("Search");
        menuItemSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemSearchActionPerformed(evt);
            }
        });
        jMenu2.add(menuItemSearch);

        menuItemClear.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemClear.setText("Clear");
        menuItemClear.setToolTipText("");
        menuItemClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemClearActionPerformed(evt);
            }
        });
        jMenu2.add(menuItemClear);

        jMenu4.setText("Keyword");

        menuItemKeywordLoad.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        menuItemKeywordLoad.setText("Load");
        menuItemKeywordLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemKeywordLoadActionPerformed(evt);
            }
        });
        jMenu4.add(menuItemKeywordLoad);

        menuItemKeywordSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        menuItemKeywordSave.setText("Save As");
        menuItemKeywordSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemKeywordSaveActionPerformed(evt);
            }
        });
        jMenu4.add(menuItemKeywordSave);

        jMenu2.add(jMenu4);

        mainMenuBar.add(jMenu2);

        jMenu3.setText("도움말");

        menuItemAbout.setText("About");
        menuItemAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemAboutActionPerformed(evt);
            }
        });
        jMenu3.add(menuItemAbout);

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

    private void buttonSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSearchActionPerformed
        searcher.setPageNo(1);
        searcher.setQueryString(searchWords.getText());
        try {
            searcher.search();
            buttonClear.setEnabled(true);
            buttonSearch.setEnabled(true);
            buttonOpenIndex.setEnabled(false);
            buttonKeywordLoad.setEnabled(true);
            buttonKeywordSave.setEnabled(true);
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }//GEN-LAST:event_buttonSearchActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_formWindowClosed

    private void menuItemKeywordSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemKeywordSaveActionPerformed
        buttonKeywordSaveActionPerformed(evt);
    }//GEN-LAST:event_menuItemKeywordSaveActionPerformed

    private void menuItemAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemAboutActionPerformed
        JOptionPane.showMessageDialog(this, "Auditor's Companion for Korean Air\n\n"
                + "- 주요기능 : 문서 자동 수집, 키워드 검색\n"
                + "- Developed by H.H.Kim (SELBI)\n"
                + "- Ver 1.0 (2012.11)");
    }//GEN-LAST:event_menuItemAboutActionPerformed

    private void buttonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonClearActionPerformed
        searchWords.setText(null);
        contentsArea.setText(null);
        metadataArea.setText(null);
        initResultTable();
        initFragmentTable();
        pageLabel.setText("Page:"); //resultTable 위의 Page표시
        pageText.setText(""); // navigation 버튼 우측의 Page 입력란
        searcher.setPageNo(1);
        setNavigateButtonsDisable();
        searchWords.requestFocus();
        
        buttonSearch.setEnabled(true);
        buttonOpenIndex.setEnabled(true);
        buttonKeywordLoad.setEnabled(true);
        buttonKeywordSave.setEnabled(false);
    }//GEN-LAST:event_buttonClearActionPerformed

    private void resultTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_resultTableMouseClicked
        int row = resultTable.getSelectedRow();
        if (evt.getClickCount() == 1) {
            resultTable.setSelectionForeground(Color.WHITE);
            searcher.setContentsAndMetaArea(row);
        } else if (evt.getClickCount() == 2) {            
            resultTable.setSelectionForeground(Color.YELLOW);
            openWithExternalProgram(row);
        }
    }//GEN-LAST:event_resultTableMouseClicked

    public void openWithExternalProgram(int row) {
        model = (MyTableModel) resultTable.getModel();
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
        int pageNo = searcher.page.getCurrentPageNo();
        try {
            pageNo = Integer.parseInt(pageText.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Please input numbers.", "Numeric Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (pageNo < 1 || pageNo > searcher.page.getPageCount()) {
            JOptionPane.showMessageDialog(null, "Page range is out of bound.", "Page No Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        searcher.page.setCurrentPageNo(pageNo);
        try {
            searcher.search();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }//GEN-LAST:event_pageTextActionPerformed

    private void buttonLastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLastActionPerformed
        searcher.page.setCurrentPageNo(searcher.page.getPageCount());
        try {
            searcher.search();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }//GEN-LAST:event_buttonLastActionPerformed

    private void buttonNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNextActionPerformed
        searcher.page.setCurrentPageNo(searcher.page.getCurrentPageNo()+1);
        try {
            searcher.search();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }//GEN-LAST:event_buttonNextActionPerformed

    private void buttonBeforeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonBeforeActionPerformed
        searcher.page.setCurrentPageNo(searcher.page.getCurrentPageNo()-1);
        try {
            searcher.search();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }//GEN-LAST:event_buttonBeforeActionPerformed

    private void buttonFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFirstActionPerformed
        searcher.page.setCurrentPageNo(1);
        try {
            searcher.search();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }//GEN-LAST:event_buttonFirstActionPerformed

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
        searcher.setContentsAndMetaArea(resultTable.getSelectedRow());
    }//GEN-LAST:event_resultTableKeyReleased

    private void buttonNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNewActionPerformed
        register = new Register(this, true);
        register.setLocation((this.getWidth()-register.getWidth())/2, 
                (this.getHeight()-register.getHeight())/2);
        register.setVisible(true);
        if (caseName.equals("") || auditor.equals("") || caseDir.equals("")) {
            // setMessage(" Case was not created. Case Name, Auditor and Target Directory information should be supplied");
        } else {
            try {
                setStage(Stage.CASE_CREATED);
                initLogger(caseDir);
//                System.out.println(caseName);
//                System.out.println(caseInfoFile.getAbsolutePath());
//                System.out.println(fileListFile.getAbsolutePath());
//                System.out.println(docLang);
            } catch (IOException ex) {
                Logger.getLogger(Companion.class.getName()).log(Level.SEVERE, null, ex);
            }
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

    private void buttonScanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonScanActionPerformed
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
        checkManualFileTypes();
        compliance.Scanner analyzer = new compliance.Scanner(jobDirs, jobFileTypes, caseDir, fileListFile, this);   
        analyzer.setParent(this);
        //analyzer.addPropertyChangeListener(this);
        analyzer.execute();        
    }//GEN-LAST:event_buttonScanActionPerformed

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

    private void menuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemExitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_menuItemExitActionPerformed

    private void menuItemMakeCaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemMakeCaseActionPerformed
        buttonNewActionPerformed(evt);
        mainTabbedPane.setSelectedIndex(1);
    }//GEN-LAST:event_menuItemMakeCaseActionPerformed

    private void menuItemOpenCaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemOpenCaseActionPerformed
        buttonOpenActionPerformed(evt);
        mainTabbedPane.setSelectedIndex(1);
    }//GEN-LAST:event_menuItemOpenCaseActionPerformed

    private void menuItemAnalyzeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemAnalyzeActionPerformed
        buttonScanActionPerformed(evt);
    }//GEN-LAST:event_menuItemAnalyzeActionPerformed

    private void menuItemCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemCopyActionPerformed
        buttonCopyActionPerformed(evt);
    }//GEN-LAST:event_menuItemCopyActionPerformed

    private void menuItemIndexingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemIndexingActionPerformed
        buttonIndexActionPerformed(evt);
    }//GEN-LAST:event_menuItemIndexingActionPerformed

    private void menuItemResetCaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemResetCaseActionPerformed
        buttonResetActionPerformed(evt);
    }//GEN-LAST:event_menuItemResetCaseActionPerformed

    private void menuItemOpenIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemOpenIndexActionPerformed
        buttonOpenIndexActionPerformed(evt);
    }//GEN-LAST:event_menuItemOpenIndexActionPerformed

    private void buttonOpenIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOpenIndexActionPerformed
        IndexOpenDialog dlg = new IndexOpenDialog(this, true);
        dlg.setLocation((this.getWidth()-dlg.getWidth())/2, 
                (this.getHeight()-dlg.getHeight())/2);
        dlg.setVisible(true);
        if (indexDir != null) {
            mainTabbedPane.setSelectedIndex(0);
            searchWords.requestFocus();
            searchWords.setCaretPosition(0);
            try {
                initLogger(new File(indexDir).getParent());
            } catch (IOException ex) {
                Companion.logger.log(Level.SEVERE, ex.getMessage());
            }
        }
    }//GEN-LAST:event_buttonOpenIndexActionPerformed

    private void menuItemClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemClearActionPerformed
       buttonClearActionPerformed(evt);
    }//GEN-LAST:event_menuItemClearActionPerformed

    private void menuItemSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemSearchActionPerformed
        buttonSearchActionPerformed(evt);
    }//GEN-LAST:event_menuItemSearchActionPerformed

    private void menuItemKeywordLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemKeywordLoadActionPerformed
        buttonKeywordLoadActionPerformed(evt);
    }//GEN-LAST:event_menuItemKeywordLoadActionPerformed

    private void buttonKeywordLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonKeywordLoadActionPerformed
        JFileChooser chooser = new JFileChooser();       
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Search Keyword file (.kwd)", "kwd");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(new File(new File(indexDir).getParent(), "Keywords"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 
            try {
                //setInfoTableWithVerify(chooser.getSelectedFile().getAbsolutePath());    
                File file = chooser.getSelectedFile();   
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        new FileInputStream(file), "euc-kr"));
                while ((line = br.readLine()) != null) {
                    searchWords.append(line);
                }      
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }                   
    }//GEN-LAST:event_buttonKeywordLoadActionPerformed

    private void buttonKeywordSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonKeywordSaveActionPerformed
        // TODO add your handling code here:
        JFileChooser chooser = new JFileChooser();       
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Search Keyword file (.kwd)", "kwd");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(new File(new File(indexDir).getParent(), "Keywords"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            //setInfoTableWithVerify(chooser.getSelectedFile().getAbsolutePath());    
            File file = chooser.getSelectedFile();
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(file), "euc-kr"))) {
                bw.write(searchWords.getText());
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }                           
    }//GEN-LAST:event_buttonKeywordSaveActionPerformed

    private void etcFileTypesFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_etcFileTypesFocusLost
        checkTreeNodesAndManualFileTypes();
    }//GEN-LAST:event_etcFileTypesFocusLost

    private void fragmentTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fragmentTableMouseClicked
        int row = fragmentTable.getSelectedRow();
        MyTableModel myModel = (MyTableModel)fragmentTable.getModel();
        
        HTMLEditorKit htmlKit = new HTMLEditorKit();
        HTMLDocument htmlFragDoc = (HTMLDocument) htmlKit.createDefaultDocument();
        HTMLDocument htmlContentsDoc = (HTMLDocument) contentsArea.getDocument();
        String searchText = "";
        String contentsText = "";
        try {
            String fragSource = (String) myModel.getValueAt(row, 0);
            fragSource = fragSource.replaceAll("\n", "<br>");
            htmlKit.read(new StringReader(fragSource), htmlFragDoc, 0);
            searchText = htmlFragDoc.getText(0, htmlFragDoc.getLength()).trim();
            contentsText = htmlContentsDoc.getText(0, htmlContentsDoc.getLength());
        } catch (BadLocationException | IOException ex) {
            Companion.logger.log(Level.SEVERE, ex.getMessage());
        } 
        
        int pos = contentsText.indexOf(searchText);      
        if (pos > 0) {
            contentsArea.requestFocus();
            contentsArea.setCaretPosition(pos);  
            contentsArea.setSelectionStart(pos);
            contentsArea.setSelectionEnd(pos + searchText.length());
        }
    }//GEN-LAST:event_fragmentTableMouseClicked
    
    @Override
    public void valueChanged(TreeCheckingEvent e) {
        checkTreeNodesAndManualFileTypes();
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
            java.util.logging.Logger.getLogger(Companion.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Companion.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Companion.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Companion.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                    Companion searcher = new Companion();
                    Image icon = Toolkit.getDefaultToolkit().getImage(getClass()
                            .getResource("/compliance/images/search1.jpg"));
                    searcher.setIconImage(icon);
                    searcher.initResultTable();
                    searcher.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JEditorPane advisorBasic;
    private javax.swing.JEditorPane advisorCollect;
    private javax.swing.JPanel advisorP;
    private javax.swing.JEditorPane advisorQuerySyntax;
    private javax.swing.JEditorPane advisorSearch;
    private javax.swing.JPanel bottomP;
    private javax.swing.JButton buttonBefore;
    private javax.swing.JButton buttonClear;
    private javax.swing.JButton buttonCopy;
    private javax.swing.JButton buttonFirst;
    private javax.swing.JButton buttonIndex;
    private javax.swing.JButton buttonKeywordLoad;
    private javax.swing.JButton buttonKeywordSave;
    private javax.swing.JButton buttonLast;
    private javax.swing.JButton buttonNew;
    private javax.swing.JButton buttonNext;
    private javax.swing.JButton buttonOpen;
    private javax.swing.JButton buttonOpenIndex;
    private javax.swing.JButton buttonReset;
    private javax.swing.JButton buttonScan;
    private javax.swing.JButton buttonSearch;
    private javax.swing.JPanel collectorP;
    private javax.swing.JEditorPane contentsArea;
    private javax.swing.JTextArea etcFileTypes;
    private javax.swing.JTable fragmentTable;
    private javax.swing.JSplitPane hSP1s;
    private javax.swing.JSplitPane hSplitPane;
    private javax.swing.JLabel indexInfoLabel;
    private javax.swing.JTable infoTable;
    private javax.swing.JLabel jLabel1s;
    private javax.swing.JLabel jLabel2s;
    private javax.swing.JLabel jLabel4s;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jSP2s;
    private javax.swing.JScrollPane jSPs;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblCase;
    private javax.swing.JLabel lblStage;
    private javax.swing.JLabel lblTargetDir;
    private javax.swing.JMenuBar mainMenuBar;
    private javax.swing.JTabbedPane mainTabbedPane;
    private javax.swing.JMenuItem menuItemAbout;
    private javax.swing.JMenuItem menuItemAnalyze;
    private javax.swing.JMenuItem menuItemClear;
    private javax.swing.JMenuItem menuItemCopy;
    private javax.swing.JMenuItem menuItemExit;
    private javax.swing.JMenuItem menuItemIndexing;
    private javax.swing.JMenuItem menuItemKeywordLoad;
    private javax.swing.JMenuItem menuItemKeywordSave;
    private javax.swing.JMenuItem menuItemMakeCase;
    private javax.swing.JMenuItem menuItemOpenCase;
    private javax.swing.JMenuItem menuItemOpenIndex;
    private javax.swing.JMenuItem menuItemResetCase;
    private javax.swing.JMenuItem menuItemSearch;
    private javax.swing.JEditorPane metadataArea;
    private javax.swing.JPanel midLeftBottomP;
    private javax.swing.JPanel midLeftPanel;
    private javax.swing.JPanel midP;
    private javax.swing.JLabel pageLabel;
    private javax.swing.JTextField pageText;
    private javax.swing.JTable resultTable;
    private javax.swing.JTextArea searchWords;
    private javax.swing.JPanel searcherP;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JTabbedPane tabbedPane1;
    private javax.swing.JPanel upperPs;
    private javax.swing.JSplitPane vSplitPane;
    // End of variables declaration//GEN-END:variables

    // Helper methods for Analyze, Copy, Index
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
   
    public void initFragmentTable() {
        Object[][] data = {{""}};
        String[] columnNames = {"찾은 글 조각" };
        model = new MyTableModel(data, columnNames);
        fragmentTable.setModel(model);
    }
    
    public void setNavigateButtonsState() {
        int curPage = searcher.page.getCurrentPageNo();
        int totalPage = searcher.page.getPageCount();
        if (totalPage == 1) {
            setNavigateButtonsDisable();
        } else if (totalPage > 1) {
            if (curPage == 1) {  // first page
                buttonFirst.setEnabled(false);
                buttonBefore.setEnabled(false);
                buttonNext.setEnabled(true);
                buttonLast.setEnabled(true);
                pageText.setEnabled(true);
            } else if (curPage == searcher.page.getPageCount()) { // last page
                buttonFirst.setEnabled(true);
                buttonBefore.setEnabled(true);
                buttonNext.setEnabled(false);
                buttonLast.setEnabled(false);
                pageText.setEnabled(true);
            } else {
                buttonFirst.setEnabled(true);
                buttonBefore.setEnabled(true);
                buttonNext.setEnabled(true);
                buttonLast.setEnabled(true);
                pageText.setEnabled(true);
            }
        }
    }
    
    public void setNavigateButtonsDisable() {
        buttonFirst.setEnabled(false);
        buttonBefore.setEnabled(false);
        buttonNext.setEnabled(false);
        buttonLast.setEnabled(false);
        pageText.setEnabled(false);
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
        lblTargetDir.setText(dir);
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
           
    public void setFragmentTable(MyTableModel model) {
        fragmentTable.setModel(model);
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
            buttonScan.setEnabled(true);
        } else { 
            buttonScan.setEnabled(false); 
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
  
    // Helper methods for Search
    public void setPageLabel(String msg) {
        pageLabel.setText(msg);
    }
    
    public void setResultTableModel(MyTableModel model) {
        resultTable.setModel(model);
    }
    
    public void setFocusToResultTable() {
        resultTable.requestFocus();
    }
    
    public void selectFirstRowOfResultTable(){
        resultTable.setRowSelectionInterval(0, 0);
    }
    
    public void setEnableSearchButton(){
        buttonSearch.setEnabled(true);
    }
    
    public void setEnableResetButton(){
        buttonClear.setEnabled(true);
    }
    
    public void setEnableKeywordLoadButton() {
        buttonKeywordLoad.setEnabled(true);
    }
    
    public void setEnableKeywordSaveButton() {
        buttonKeywordSave.setEnabled(true);
    }
    
    public void setContentsArea(String text) {
        //contentsArea.setContentType("text/html");
        contentsArea.setText(text);
    }
    
    public void setMetadataArea(String text) {
        metadataArea.setText(text);
    }
    
    public void setContentsAreaCaretPosition(int i) {
        contentsArea.setCaretPosition(i);
    }
    
    public void resultTableColumnWidthAdjust() {
        resultTable.getColumnModel().getColumn(0).setMaxWidth(40);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(250);
    }
    
    public void checkManualFileTypes() {
        if ("".equals(etcFileTypes.getText())) {
            return;
        }
        String[] types = etcFileTypes.getText().split("\\s");
        for (String type : types) {
            if (!jobFileTypes.contains(type)) {
                jobFileTypes.add(type);
            }
        }                
    }
    
    public void checkTreeNodesAndManualFileTypes() {
        jobDirs.clear();
        jobFileTypes.clear();    
        TreePath[] folderNodes = folderTree.getCheckingPaths();
        TreePath[] fileTypeNodes = fileTypeTree.getCheckingPaths();
        if (folderNodes == null || fileTypeNodes == null) {
            return;
        }    
 
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
        checkManualFileTypes();
        Collections.sort(jobFileTypes);
        if (stage == Stage.ANALYZE_COMPLETED || stage == Stage.CASE_LOADED 
                || stage == Stage.COPY_COMPLETED) {
            try {
                updateInfoTable(jobFileTypes);       
            } catch (IOException evt) {
                System.out.println(evt.getMessage());
            }
        }         
    }
    
    public void setFragmentTableRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellHeaderRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        renderer.setVerticalAlignment(SwingConstants.TOP);
        fragmentTable.getColumnModel().getColumn(0).setCellRenderer(renderer);
    }
 
}
