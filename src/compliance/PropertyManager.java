/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compliance;

import java.io.*;
import java.util.*;

/**
 *
 * @author hohkim
 */
public class PropertyManager {
    public Properties properties;
    private String caseHome;
    
    public PropertyManager() {
	caseHome = System.getProperty("user.home") + "\\Documents\\Search";
	properties = new Properties();		
    }
    
    public void setDefault() throws IOException {
        // Default setting
        properties.setProperty("caseName", "Field Audit");
        properties.setProperty("auditor", "SELBI");
        properties.setProperty("language", "Korean");
        properties.setProperty("caseDir", caseHome);
        properties.setProperty("filesDir", caseHome + "\\Files");
        properties.setProperty("indexDir", caseHome + "\\Index");
        properties.setProperty("scannedFile", "");
        properties.setProperty("queryString", "");
        properties.setProperty("currentPageNo", "1");
        properties.setProperty("countPerPage", "30");
        // if properties file exists, load it.
        loadProperties();
    }

    public void loadProperties() throws IOException {
        File file = new File(System.getProperty("user.home"), "auditor_companion.properties");
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(
                         new InputStreamReader(new FileInputStream(file), "euc-kr"))) {
                properties.load(br);
            }
        }
    }

    public void writeProperties() throws IOException {
//        properties.setProperty("caseName", "현장점검");
//        properties.setProperty("auditor", "김호현");
//        properties.setProperty("language", "Korean");
//        properties.setProperty("baseDirectory", caseHome);
//        properties.setProperty("queryString", "공정");
//        properties.setProperty("currentPageNo", "1");
//        properties.setProperty("countPerPage", "20");

        File file = new File(System.getProperty("user.home"), "auditor_companion.properties");
        if (!file.exists()) {
            file.createNewFile();
        }
        try (BufferedWriter bw = new BufferedWriter(
                     new OutputStreamWriter(new FileOutputStream(file), "euc-kr"))) {
            properties.store(bw, "no comment");
            bw.close();
        }
    }
    
}
