/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compliance;

import compliance.AuditCompanion.DocLang;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.kr.KoreanAnalyzer;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
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
public class Indexer extends SwingWorker <Void, Void> {

    File docDir;
    Directory indexDir;
    Analyzer analyzer;
    ContentHandler contentHandler;
    Metadata metadata;
    Parser parser;
    int counter;
    AuditCompanion parentApp;

    public Indexer(String indexPath, String docsPath, DocLang lang, AuditCompanion parent) {
        docDir = new File(docsPath);
        try {
            indexDir = FSDirectory.open(new File(indexPath));
            if (lang == DocLang.English) {
                analyzer = new StandardAnalyzer(Version.LUCENE_36);
            } else if (lang == DocLang.Korean) {
                analyzer = new KoreanAnalyzer(Version.LUCENE_36);
            } else if (lang == DocLang.Japanease) {
                analyzer = new JapaneseAnalyzer(Version.LUCENE_36);
            }
        } catch (IOException ex) {
            System.out.println(ex);
        }
        counter = 0;
        parentApp = parent;
    }

    public void makeIndex() {
        System.out.println("Indexing to directiory");
        Date start = new Date();

        try {
            IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_36, analyzer);
            iwc.setOpenMode(OpenMode.CREATE);
            IndexWriter writer;
            writer = new IndexWriter(indexDir, iwc);
            indexDocs(writer, docDir);
            writer.close();
        } catch (CorruptIndexException | LockObtainFailedException ex) {
            System.out.println(ex.getMessage());
        }
        catch (SAXException | TikaException | IOException ex) {
            System.out.println(ex.getMessage());
        }
        Date end = new Date();
	parentApp.setMessage(String.format(" Indexing completed : total %d files", counter));
        //System.out.println(String.format("Indexing completed : %d files", counter) + 
        //        (end.getTime() - start.getTime()/1000) + " seconds");
    }

    public void indexDocs(IndexWriter writer, File file) throws IOException, SAXException, TikaException {
        if (file.canRead()) {
            if (file.isDirectory()) {
                String[] files = file.list();
                // an IO error could occur
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        indexDocs(writer, new File(file, files[i]));
                    }
                }
            } else {
                FileInputStream fis;
                try {
                    fis = new FileInputStream(file);
                    // Tika --------------------------------------------------------
                    contentHandler = new BodyContentHandler();
                    metadata = new Metadata();
                    parser = new AutoDetectParser();
                    metadata.set(Metadata.RESOURCE_NAME_KEY, file.getName());
                    parser.parse(fis, contentHandler, metadata, new ParseContext());
                    // -------------------------------------------------------------
                } catch (FileNotFoundException e) {
                    // some temporary files raise this exception with an "access denied" message
                    System.out.println(e);
                    return;
                }
                try {
                    Document doc = new Document();
                    doc.add(new Field("filename", file.getName(), Store.YES, Index.ANALYZED));
                    // Tika ---------------------------------------------------------
                    doc.add(new Field("contents", contentHandler.toString(), Store.YES, 
                            Index.ANALYZED));
                    //doc.add(new Field("contents", contentHandler.toString(), Store.YES, 
                     //       Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
                    // --------------------------------------------------------------
                    writer.addDocument(doc);
                    counter++;
                    // System.out.format("%5d. adding %s\n", counter, file);
                    parentApp.setMessage(String.format("%5d. adding %s\n", counter, file));
                } finally {
                    fis.close();
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
        parentApp.setCursor(null);
        parentApp.setStage(AuditCompanion.Stage.INDEX_CREATED);
    }
}
