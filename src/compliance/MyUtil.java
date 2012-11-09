/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compliance;

import com.google.common.hash.*;
import java.io.*;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TokenSources;
/**
 *
 * @author hohkim
 */
public class MyUtil {

    public static String getHashCode(File file) {
        HashFunction hf = Hashing.md5();
        Hasher hs = hf.newHasher();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage());
        }
        byte[] ar = new byte[256];
        try {
            while (in.read(ar) != -1) {
                hs.putBytes(ar);
            }
        } catch (IOException ex) {
             System.out.println(ex.getMessage());
        }
        //HashCode hc = hs.hash();
        return hs.hash().toString();
    }
      
    public static boolean verifyHash(String fileName, String hashVal) {
        String hs1 = MyUtil.getHashCode(new File(fileName));
        if (hs1.equals(hashVal)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Generates contextual fragments. Assumes term vectors not stored in the
     * index.
     *
     * @param analyzer - analyzer used for both indexing and searching
     * @param query - query object created from user's input
     * @param fieldName - name of the field in the lucene doc containing the
     * text to be fragmented
     * @param fieldContents - contents of fieldName
     * @param fragmentNumber - max number of sentence fragments to return
     * @param fragmentSize - the max number of characters for each fragment
     * @return array of fragments from fieldContents with terms used in query in
     * <b> </b> tags
     * @throws IOException
     */
    public static String[] getFragmentsWithHighlightedTerms(org.apache.lucene.analysis.Analyzer analyzer, Query query,
            String fieldName, String fieldContents, int fragmentNumber, int fragmentSize) throws IOException {

        TokenStream stream = TokenSources.getTokenStream(fieldName, fieldContents, analyzer);    
        // Check next line is working
        QueryScorer scorer = new QueryScorer(query, fieldName);
        Fragmenter fragmenter = new SimpleFragmenter(fragmentSize);
        Highlighter highlighter = new Highlighter(scorer);
        highlighter.setTextFragmenter(fragmenter);
        highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);

        String[] fragments = null;
        try {
            fragments = highlighter.getBestFragments(stream, fieldContents, fragmentNumber);
        } catch (InvalidTokenOffsetsException ex) {
            System.out.println(ex);
        }
        return fragments;
    }    
    
  /**
     * Generates contextual fragments.
     *
     * @param termPosVector - Term Position Vector for fieldName
     * @param query - query object created from user's input
     * @param fieldName - name of the field containing the text to be fragmented
     * @param fieldContents - contents of fieldName
     * @param fragmentNumber - max number of sentence fragments to return
     * @param fragmentSize - the max number of characters for each fragment
     * @return array of fragments from fieldContents with terms used in query in
     * <b> </b> tags
     * @return
     * @throws IOException
     */
    public static String[] getFragmentsWithHighlightedTerms(TermPositionVector termPosVector, Query query,
            String fieldName, String fieldContents, int fragmentNumber, int fragmentSize) throws IOException {

        TokenStream stream = TokenSources.getTokenStream(termPosVector);
        QueryScorer scorer = new QueryScorer(query, fieldName);
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, fragmentSize);
        Highlighter highlighter = new Highlighter(scorer);
        highlighter.setTextFragmenter(fragmenter);
        highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);

        String[] fragments = null;
        try {
            fragments = highlighter.getBestFragments(stream, fieldContents, fragmentNumber);
        } catch (InvalidTokenOffsetsException ex) {
            System.out.println(ex);
        }
        return fragments;
    }    
}
