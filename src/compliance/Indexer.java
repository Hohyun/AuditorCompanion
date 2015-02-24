/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compliance;

import compliance.Companion.DocLang;
import compliance.Companion.Stage;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import javax.swing.SwingWorker;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.kr.KoreanAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author hohkim
 */
public class Indexer extends SwingWorker <Void, Void> {

    File docDir;
    Directory indexDir;
    Analyzer analyzer;
    ContentHandler contentHandler;
    Metadata metadata;
    Parser parser;
    int counter;
    Companion collector;

    public Indexer(String indexPath, String docsPath, DocLang lang, Companion companion) {
        docDir = new File(docsPath);
        try {
            indexDir = FSDirectory.open(new File(indexPath));
            if (lang == DocLang.Korean) {
                analyzer = new KoreanAnalyzer(Version.LUCENE_36);
            } else if (lang == DocLang.English) {
                analyzer = new StandardAnalyzer(Version.LUCENE_36);
            } else if (lang == DocLang.Japanese) {
                analyzer = new JapaneseAnalyzer(Version.LUCENE_36);
            } else if (lang == DocLang.Chinese) {
                analyzer = new CJKAnalyzer(Version.LUCENE_36);
            }
        } catch (IOException ex) {
            System.out.println(ex);
        }
        counter = 0;
        this.collector = companion;
    }

    public void makeIndex() {
        System.out.println("Indexing to directiory");
        try {
            IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_36, analyzer);
            iwc.setOpenMode(OpenMode.CREATE);
            IndexWriter writer;
            writer = new IndexWriter(indexDir, iwc);
            indexDocs(writer, docDir);
            writer.close();
        } catch (CorruptIndexException | LockObtainFailedException ex) {
            System.out.println(ex.getMessage());
            Companion.logger.log(Level.SEVERE, ex.getMessage());
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
            Companion.logger.log(Level.SEVERE, ex.getMessage());
        }
        Date end = new Date();
	collector.setMessage(String.format("Indexing completed : total %d files", counter));
    }

    public void indexDocs(IndexWriter writer, File file) throws IOException{
        if (file.canRead() && file.exists()) {
            if (file.isDirectory()) {
                String[] files = file.list();
                // an IO error could occur
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        indexDocs(writer, new File(file, files[i]));
                    }
                }
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    // Tika --------------------------------------------------------
                    contentHandler = new BodyContentHandler();
                    metadata = new Metadata();
                    parser = new AutoDetectParser();
                    metadata.set(Metadata.RESOURCE_NAME_KEY, file.getName());
                    parser.parse(fis, contentHandler, metadata, new ParseContext());
                    // -------------------------------------------------------------
                    String hashValue = MyUtil.getHashCode(file); 
                    Document doc = new Document();
                    doc.add(new Field("path", "..\\Files", Store.YES, Index.NOT_ANALYZED));
                    doc.add(new Field("file-name", file.getName(), Store.YES, Index.ANALYZED));
                    doc.add(new Field("full-file-name", "..\\Files\\" + file.getName(), Store.YES, Index.NOT_ANALYZED));
                    doc.add(new Field("file-size", Long.toString(file.length()), Store.YES, Index.NOT_ANALYZED));
                    doc.add(new Field("md5", hashValue, Store.YES, Index.NOT_ANALYZED));                    
                    String[] metaNames = metadata.names();
                    for (String metaName : metaNames) {
                        //System.out.println(metaName + ": " + metadata.get(metaName) );
                        switch (metaName) {
                            case "Author":
                                doc.add(new Field("author", metadata.get("Author"), Store.YES, Index.NOT_ANALYZED));
                                break;
                            case "Application-Name":
                                doc.add(new Field("application-name", metadata.get("Application-Name"), Store.YES, Index.NOT_ANALYZED));
                                break;                                
                            case "Creation-Date":
                                doc.add(new Field("creation-date", metadata.get("Creation-Date"), Store.YES, Index.NOT_ANALYZED));
                                break;
                            case "Last-Modified":
                                doc.add(new Field("last-modified", metadata.get("Last-Modified"), Store.YES, Index.NOT_ANALYZED));                       
                                break;
                            case "meta:last-author":
                                doc.add(new Field("last-author", metadata.get("meta:last-author"), Store.YES, Index.NOT_ANALYZED));
                                break;
                            case "Page-Count":
                                doc.add(new Field("page-count", metadata.get("Page-Count"), Store.YES, Index.NOT_ANALYZED));
                                break;
                        }
                    }
                    // Tika ---------------------------------------------------------
                    doc.add(new Field("contents", contentHandler.toString(), Store.YES, 
                            Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
                    // --------------------------------------------------------------
                    writer.updateDocument(new Term("filename", file.getName()), doc);
                    //writer.addDocument(doc);
                    counter++;
                    // System.out.format("%5d. adding %s\n", counter, file);
                    collector.setMessage(String.format("%5d. adding %s\n", counter, file));
                } catch (SAXException | TikaException | FileNotFoundException ex) {
                    Companion.logger.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getMessage(), file.getAbsolutePath()});
                    System.out.println(ex + " : " + file.getAbsolutePath());
                }
            }
        }
    }

    @Override
    protected Void doInBackground() throws Exception {     
        makeIndex();
        return null;
    }
    
    @Override
    public void done() {
        collector.setCursor(null);
        collector.setStage(Stage.INDEX_CREATED);
        initSearcher();
    }
    
    public void initSearcher() {
        Properties prop = collector.propManager.properties;
        collector.setIndexDir(prop.getProperty("indexDir"));
        collector.searcher.setIndexDir(prop.getProperty("indexDir"));
        collector.searcher.initSearcher();           
        collector.searcher.setAnalyzer(prop.getProperty("language"));
        collector.setIndexInfolabel(String.format("%s (%s Analyzer)", 
                prop.getProperty("indexDir"), 
                prop.getProperty("language")));
        collector.searcher.setPageSize(Integer.parseInt(prop.getProperty("countPerPage")));
        
        collector.setNavigateButtonsDisable();
        collector.setEnableSearchButton();
        collector.setEnableKeywordLoadButton();
        collector.setEnableKeywordSaveButton();
    }
}
