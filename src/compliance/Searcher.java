/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compliance;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.kr.KoreanAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * @author hohkim
 */
public class Searcher {
    
    private Companion companion;
    private String queryString;
    private String indexDir;
    public Page page;
    private MyTableModel model;
    private IndexReader reader;
    private IndexSearcher indexSearcher;
    private Analyzer analyzer;
    private TopDocs topDocs;
    private Query query;
    private SimpleHTMLFormatter formatter;
    private QueryScorer queryScorer;
    private Highlighter highlighter;
    private final int FRAGMENT_SIZE = 50;
    private final int MAX_FRAGMENT_COUNT = 100;
    private final String htmlHeader = "<html><head><style>b {color:black; font-weight:bold; background-color:yellow}</style></head><body>";
    private final String htmlFooter = "</body></html>";
    private String highlighted;
    
    public Searcher(Companion companion) {
        this.companion = companion;
        page = new Page(1);
    }
 
    public void initSearcher() {
        try {
            reader = IndexReader.open(FSDirectory.open(new File(this.indexDir)));
            indexSearcher = new IndexSearcher(reader);
        } catch (IndexNotFoundException e) {
            Companion.logger.log(Level.SEVERE, e.getMessage());
        } catch (IOException e) {
            Companion.logger.log(Level.SEVERE, e.getMessage());
        }
    }
    
    public void search() throws IOException {
        QueryParser parser = new QueryParser(Version.LUCENE_36, "contents", analyzer);
        try {
            query = parser.parse(queryString);
        } catch (ParseException ex) {
            System.out.println(ex);
            JOptionPane.showMessageDialog(companion, "질의어를 해석할 수 없습니다\n질의어를 다시 확인해 주세요.",
                    "질의어 해석 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        formatter = new SimpleHTMLFormatter("<b>", "</b>");
        queryScorer = new QueryScorer(query);
        highlighter = new Highlighter(formatter, queryScorer);
        highlighter.setTextFragmenter(new SimpleSpanFragmenter(queryScorer, FRAGMENT_SIZE));
        highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
            
        try {
            topDocs = indexSearcher.search(query, page.getEnd());
        } catch (IOException ex) {
            System.out.println(ex);
            return;
        }
        page.setTotalCount(topDocs.totalHits);       
        if (topDocs.totalHits == 0) {
            companion.setPageLabel("Page: 0/0, (Total Hits: 0)");
            companion.initResultTable();
            companion.setContentsArea("");
            companion.setMetadataArea("");
            return;
        }

        // for tablemodel
        int rowCount = (int) Math.min(page.getPageSize(),
                (page.getTotalCount() - (page.getCurrentPageNo() - 1) * page.getPageSize()));
        Object[][] data = new Object[rowCount][3];
        //Object[][] data = new Object[page.getPageSize()][3];
        String[] columnNames = {"SQ", "Path", "File Name"};
        for (int i = page.getStart(); i < page.getEnd(); i++) {
            ScoreDoc scoreDoc = topDocs.scoreDocs[i];
            Document d = indexSearcher.doc(scoreDoc.doc);
            //System.out.format(" %3d. (%.5f) %s\n", i + 1, scoreDoc.score, d.get("file-name"));
            data[i - page.getStart()][0] = i + 1;
            data[i - page.getStart()][1] = d.get("path");
            data[i - page.getStart()][2] = d.get("file-name");
        }
        companion.setPageLabel(String.format("Page: %d/%d, (Total Hits: %d)",
                page.getCurrentPageNo(), page.getPageCount(), page.getTotalCount()));
        model = new MyTableModel(data, columnNames);
        companion.setResultTableModel(model);
        companion.setFocusToResultTable();
        companion.selectFirstRowOfResultTable();
        setContentsAndMetaArea(0);
        companion.resultTableColumnWidthAdjust();
        companion.setNavigateButtonsState();
    }

    public String getHighlightedText(int docId, String fieldName, Document doc) 
            throws IOException, InvalidTokenOffsetsException {
        TokenStream stream = TokenSources.getAnyTokenStream(reader, docId, fieldName, doc, analyzer);
        QueryScorer scorer = new QueryScorer(query, fieldName);
        // fragment의 size를 내용전체 크기와 같게 하여 1개로 잘리게 한다.
        Fragmenter fragmenter = new SimpleFragmenter(doc.get("contents").length());
        highlighter = new Highlighter(formatter, scorer);
        highlighter.setTextFragmenter(fragmenter);
        highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
        // 한개로 잘렸으므로 세번째 인수 fragment 갯수를 어떻게 주더라도, 내부의 모든 일치단어가 highlight 된다.
        try {
            highlighted = highlighter.getBestFragments(stream, doc.get("contents"), 2, "...");
        } catch (NoClassDefFoundError | IOException | InvalidTokenOffsetsException ex) {
            //
        }
        highlighted = htmlHeader + highlighted + htmlFooter;
        return highlighted.replaceAll("\n", "<br>");
    }    
     
    public TextFragment[] getHighlightedFragments(int docId, String fieldName, Document doc) throws IOException, InvalidTokenOffsetsException {			    
	    TokenStream tokenStream = TokenSources.getAnyTokenStream(reader, docId, fieldName, doc, analyzer);
	    return highlighter.getBestTextFragments(tokenStream, doc.get(fieldName), false, MAX_FRAGMENT_COUNT);
	}
    
    public MyTableModel getModelForFragment(int docId, String fieldName, Document doc)
            throws IOException, InvalidTokenOffsetsException {
        String[] columnNames = {"찾은 글 조각"};
        Object[][] data = {};
        int rowCount = 0;
  
        TokenStream tokenStream = TokenSources.getAnyTokenStream(reader, docId, fieldName, doc, analyzer);
        QueryScorer scorer = new QueryScorer(query, fieldName);
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, FRAGMENT_SIZE);
        highlighter = new Highlighter(formatter, scorer);
        highlighter.setTextFragmenter(fragmenter);
        highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);

        TextFragment[] frags = null;      
        try {
            frags = highlighter.getBestTextFragments(tokenStream, doc.get(fieldName), false, MAX_FRAGMENT_COUNT);
        } catch (NoClassDefFoundError | IOException | InvalidTokenOffsetsException ex) {
            // do nothing
            MyTableModel model1 = new MyTableModel(data, columnNames);
            return model1;
        }
        
        if (frags != null) {
            for (int i = 0; i < frags.length; i++) {
                if ((frags[i] != null) && (frags[i].getScore() > 0)) {
                    rowCount++;
                }
            }
            data = new Object[rowCount][1];
            for (int i = 0; i < frags.length; i++) {
                if ((frags[i] != null) && (frags[i].getScore() > 0)) {
                    data[i][0] = htmlHeader + frags[i].toString() + htmlFooter;
                }
            }
        }
        MyTableModel model1 = new MyTableModel(data, columnNames);
        return model1;
    }
    
    public String getHighlightedFragmentsText(int docId, String fieldName, Document doc) throws IOException, InvalidTokenOffsetsException {
        TokenStream tokenStream = TokenSources.getAnyTokenStream(reader, docId, fieldName, doc, analyzer);
        QueryScorer scorer = new QueryScorer(query, fieldName);
//        Fragmenter fragmenter = new SimpleFragmenter(FRAGMENT_SIZE);
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, FRAGMENT_SIZE);
        highlighter = new Highlighter(formatter, scorer);
        highlighter.setTextFragmenter(fragmenter);
        highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
                
        TextFragment[] frags = highlighter.getBestTextFragments(tokenStream, doc.get(fieldName), false, MAX_FRAGMENT_COUNT);
        StringBuilder sb = new StringBuilder();
        
	for (int i = 0; i < frags.length; i++) {
           if ((frags[i] != null) && (frags[i].getScore() > 0)) {
               sb.append(frags[i].toString()).append("...<hr>");
           }
       }      
       highlighted = htmlHeader + sb.toString() + htmlFooter;
       return highlighted.replaceAll("\n", "<br>");
    }
               
    public void setContentsAndMetaArea(int row) {
        highlighted = "";
        if (model.getValueAt(row, 0) == null) {
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        ScoreDoc scoreDoc = topDocs.scoreDocs[id - 1];
        Document doc;
        try {
            doc = indexSearcher.doc(scoreDoc.doc);
            highlighted = getHighlightedText(scoreDoc.doc, "contents", doc);
            
            if ((htmlHeader + htmlFooter).equals(highlighted)) {
                highlighted = htmlHeader + doc.get("contents") + htmlFooter;
                companion.setContentsArea(highlighted.replaceAll("\n", "<br>"));
                companion.initFragmentTable();
            } else { 
                companion.setContentsArea(highlighted);
                companion.setFragmentTable(getModelForFragment(scoreDoc.doc, "contents", doc));
            }                
            
            // Metadata
            String metaString = "";
            String fullFileName = doc.get("full-file-name");
            String appName = doc.get("application-name");
            String fileSize = doc.get("file-size");
            String pageCount = doc.get("page-count");
            String md5origin = doc.get("md5");
            //
            File currentFile = null;
            if ("..\\Files".equals(doc.get("path"))) {
                currentFile = new File(new File(indexDir, doc.get("path")),
                    doc.get("file-name"));
            } else {
                currentFile = new File(doc.get("full-file-name"));
            }
            String md5current = MyUtil.getHashCode(currentFile);

            String author = doc.get("author");
            String creationDate = doc.get("creation-date");
            String lastAuthor = doc.get("last-author");
            String lastModified = doc.get("last-modified");

            metaString += String.format("<ul><li>파일 경로 : %s</li>", fullFileName);
            metaString += String.format("    <li>작성 프로그램: %s</li>", appName);
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
            metaString += String.format("    <li><font color=\"red\">최종 수정자: %s</font></li>", lastAuthor);
            metaString += String.format("    <li>파일 생성일: %s</li>", creationDate);
            metaString += String.format("    <li><font color=\"red\">최종 수정일: %s</li></font></ul>", lastModified);
            companion.setMetadataArea(metaString);
        } catch (InvalidTokenOffsetsException ex) {
            Logger.getLogger(Searcher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CorruptIndexException ex) {
            System.out.println(ex);
            Companion.logger.log(Level.SEVERE, ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex);
            Companion.logger.log(Level.SEVERE, ex.getMessage());
        }
        companion.setContentsAreaCaretPosition(0);
    }   
    
       public void setIndexDir(String dir) {
        indexDir = dir;
    }
    
    public String getIndexDir() {
        return indexDir;
    }
    
    public void setPageSize(int pageSize) {
        page.setPageSize(pageSize);
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
            Companion.logger.log(Level.SEVERE, e.getMessage());
        }
    }
    
    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }
    
    public void setPageNo(int pageNo) {
        page.setCurrentPageNo(pageNo);
    }
    
    public int getFragmentSize() {
        return FRAGMENT_SIZE;
    }
}
