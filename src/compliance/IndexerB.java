/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compliance;

import compliance.AuditCompanion.DocLang;
import java.io.*;
import java.util.*;
import javax.swing.SwingWorker;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.kr.KoreanAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
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
public class IndexerB extends SwingWorker <Void, Void> {
    MyTableModel model;
    //File docDir;
    Directory indexDir;
    Analyzer analyzer;
    ContentHandler contentHandler;
    Metadata metadata;
    Parser parser;
    int counter;
    AuditCompanion companion;

    public IndexerB(String indexPath, MyTableModel model, DocLang lang, AuditCompanion companion) {
        //docDir = new File(docsPath);
        try {
            indexDir = FSDirectory.open(new File(indexPath));
            if (lang == DocLang.English) {
                analyzer = new StandardAnalyzer(Version.LUCENE_36);
            } else if (lang == DocLang.Korean) {
                analyzer = new KoreanAnalyzer(Version.LUCENE_36);
            } else if (lang == DocLang.Japanese) {
                analyzer = new JapaneseAnalyzer(Version.LUCENE_36);
            }
        } catch (IOException ex) {
            System.out.println(ex);
        }
        counter = 0;
        this.companion = companion;
        this.model = model;
    }

    @Override
    protected Void doInBackground() throws Exception {     
        makeIndex();
        return null;
    }
    
    @Override
    public void done() {
        companion.setCursor(null);
        companion.setStage(AuditCompanion.Stage.INDEX_CREATED);
    }    
   
    public void makeIndex() {
        System.out.println("Indexing to directiory");
        try {
            IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_36, analyzer);
            iwc.setOpenMode(OpenMode.CREATE);
            IndexWriter writer;
            writer = new IndexWriter(indexDir, iwc);
            indexDocs(writer);
            writer.close();
        } catch (CorruptIndexException | LockObtainFailedException ex) {
            System.out.println(ex.getMessage());
        }
        catch (SAXException | TikaException | IOException ex) {
            System.out.println(ex.getMessage());
        }
        Date end = new Date();
	companion.setMessage(String.format("Indexing completed : total %d files", counter));
    }

    public void indexDocs(IndexWriter writer) throws IOException, SAXException, TikaException {
        
        int row_cnt = model.getRowCount();
        
        for (int i = 0; i < row_cnt; i++) {
            if ((Boolean) model.getValueAt(i, 0)) {
                String src = model.getValueAt(i, 1).toString();
                File file = new File(src);
                FileInputStream fis;
                try {
                    fis = new FileInputStream(file);
                    // Tika --------------------------------------------------------
                    contentHandler = new BodyContentHandler();
                    metadata = new Metadata();
                    parser = new AutoDetectParser();
                    metadata.set(Metadata.RESOURCE_NAME_KEY, file.getAbsolutePath());
                    parser.parse(fis, contentHandler, metadata, new ParseContext());
                    
                    Document doc = new Document();
                    doc.add(new Field("path", file.getParent(), Store.YES, Index.NOT_ANALYZED));
                    doc.add(new Field("filename", file.getName(), Store.YES, Index.ANALYZED));
                    // Tika ---------------------------------------------------------
                    doc.add(new Field("contents", contentHandler.toString(), Store.YES, 
                            Index.ANALYZED));
                    // --------------------------------------------------------------
                    writer.addDocument(doc);
                    counter++;
                    // System.out.format("%5d. adding %s\n", counter, file);
                    companion.setMessage(String.format("%5d. adding %s\n", counter, file));                             
                    // -------------------------------------------------------------
                } catch (FileNotFoundException e) {
                    // some temporary files raise this exception with an "access denied" message
                    System.out.println(e);
                    return;
                }
            }
        } 
    }

}
