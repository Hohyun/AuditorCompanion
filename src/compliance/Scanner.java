/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compliance;

import compliance.Companion.Stage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import javax.swing.*;

/**
 *
 * @author hohkim
 */
public class Scanner extends SwingWorker<Void,Void>{
 
    Companion collector;
    double fileSize;
    int extractFileCount;
    int totalFileCount;
    int dirCount;
    List<String> jobDirs;
    float lapTime;
    List<String> okFileExtention;
    String targetDir;
    BufferedWriter bw;
    File fileList;
    int completedTopDir = 0;
    int progress = 0;
    Date start;

    public Scanner(List<String> dirs, List<String> fileTypes, String targetFolder, File fileList, JFrame parent) {
        jobDirs = dirs;
        okFileExtention = fileTypes;
        targetDir = targetFolder.replace("\\", "\\\\");
        this.fileList = fileList;
        collector = (Companion) parent;
        // counter 초기화
        fileSize = 0;
        extractFileCount = 0;
        totalFileCount = 0;
        dirCount = 0;
        lapTime = 0;        
    }
    
    public void setParent(JFrame parent) {
        collector = (Companion) parent;
    }
    
    @Override
    public Void doInBackground() {

        start = new Date();
        try {
            bw = new BufferedWriter(new FileWriter(fileList));
               
            for (String jobDir : jobDirs) {
                if (isSubDirChecked(jobDir)) {
                // need to process for files in the directory not sub directory.
                    processSubFilesAndSubDirs(new File(jobDir), false);
                } else {
                // Do recursive scan for the directory.
                    processSubFilesAndSubDirs(new File(jobDir), true);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            collector.logger.log(Level.SEVERE, e.getMessage());
        }
        return null;
    }

    @Override
    public void done() {
        collector.setCursor(null);
        try {
            bw.close();
            collector.setInfoTable(fileList);
        } catch (IOException ex) {
            System.out.println(ex);
            Companion.logger.log(Level.SEVERE, ex.getMessage());
        }
        collector.setStage(Stage.ANALYZE_COMPLETED);
    }
     
    public void processSubFilesAndSubDirs(File file, boolean checkDir) throws IOException {
        // make list containing only office files and directory.
        try {
            if (file.isDirectory() && checkDir) {
                dirCount += 1;
                File[] files = file.listFiles();
                // Null if System Volumn Information
                if (files != null) {
                    for (File f : files) {
                        processSubFilesAndSubDirs(f, true);
                    }
                }
            }
        } catch (NullPointerException e) {
            // it happens when directory is $RECYCLE.BIN, System Volume Information, etc.
            Companion.logger.log(Level.SEVERE, e.getMessage());
            return;
        } 
        
        if (file.isFile() && file.canRead()) {
            if (isOfficeFile(file)) {
                String hashValue = MyUtil.getHashCode(file); 
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    bw.write(file.getAbsolutePath() + "/" + file.length() + "/" + 
                            formatter.format(file.lastModified()) + "/" + hashValue);
                    bw.newLine();
                } catch (IOException e) {
                    Companion.logger.log(Level.SEVERE, "{0}: {1}", new Object[]{e.getMessage(), file.getAbsolutePath()});
                }
                fileSize += file.length();
                extractFileCount++;
            }
            totalFileCount++;
            if (totalFileCount % 10 == 0) {
                Date end = new Date();
                lapTime = (end.getTime() - start.getTime()) / 1000;
                collector.setMessage(String.format("Found: %,d Files (%.1f MB) in %,d Files. "
                        + "(%.1f Seconds) ...", 
                        getExtractFileCount(), getFileSize() / 1048567, 
                        getTotalFileCount(), lapTime));
            }
        }
    }

    public boolean isSubDirChecked(String dir) {
        int cnt = 0;
        for (String jobDir : jobDirs) {
            if (jobDir.startsWith(dir)) {
                cnt++;
            }
        }
        if (cnt > 1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isOfficeFile(File file) {
        for (String extension : okFileExtention) {
            if (file.getName().toLowerCase().endsWith(extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
	  
    public double getFileSize() {
        return fileSize;
    }

    public int getExtractFileCount() {
        return extractFileCount;
    }

    public int getTotalFileCount() {
        return totalFileCount;
    }

    public int getDirCount() {
        return dirCount;
    }  
}
