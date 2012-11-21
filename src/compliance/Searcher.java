/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compliance;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.JOptionPane;
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
    
    public Searcher(Companion companion) {
        this.companion = companion;
        page = new Page(1);
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

        try {
            topDocs = indexSearcher.search(query, page.getEnd());
        } catch (IOException ex) {
            System.out.println(ex);
            return;
        }
        page.setTotalCount(topDocs.totalHits);
//        System.out.format("Total hits: %d, Total Page: %d\n",
//                topDocs.totalHits, page.getPageCount());
        
        if (topDocs.totalHits == 0) {
            companion.setPageLabel("Page: 0/0, (Total Hits: 0)");
            companion.initResultTable();
            companion.setContentsArea("");
            companion.setMetadataArea("");
            return;
        }

//        System.out.println("Current page: " + page.getCurrentPageNo());
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

    public void initSearcher() {
        try {
            reader = IndexReader.open(FSDirectory.open(new File(this.indexDir)));
            indexSearcher = new IndexSearcher(reader);
        } catch (IOException e) {
            System.out.println(e);
            Companion.logger.log(Level.SEVERE, e.getMessage());
        }
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
    
    public void setContentsAndMetaArea(int row) {
        if (model.getValueAt(row, 0) == null) {
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        ScoreDoc scoreDoc = topDocs.scoreDocs[id - 1];
        Document d;
        try {
            d = indexSearcher.doc(scoreDoc.doc);
            String contents = d.get("contents");
            String[] fragments = MyUtil.getFragmentsWithHighlightedTerms(analyzer, query, "contents", contents, 100, 100);
            String highlighted = "";
            for (String frag : fragments) {
                highlighted += frag;
            }
            companion.setContentsArea(highlighted.replaceAll("\n", "<br/>"));
            // Metadata
            String metaString = "";
            String fullFileName = d.get("full-file-name");
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
        } catch (CorruptIndexException ex) {
            System.out.println(ex);
            Companion.logger.log(Level.SEVERE, ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex);
            Companion.logger.log(Level.SEVERE, ex.getMessage());
        }
        companion.setContentsAreaCaretPosition(0);
    }   
}
