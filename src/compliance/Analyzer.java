/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compliance;

import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import java.util.List;
import compliance.AuditCompanion.Stage;

/**
 *
 * @author hohkim
 */
public class Analyzer extends SwingWorker<Void,Void>{
 
    AuditCompanion parentApp;
    double fileSize;
    int extractFileCount;
    int totalFileCount;
    int dirCount;
    List<String> topDir;
    float lapTime;
    List<String> okFileExtention;
    String targetDir;
    BufferedWriter bw;
    File fileList;
    int completedTopDir = 0;
    int numberOfTopDir = 0;
    int progress = 0;

    public Analyzer(List<String> dirs, List<String> fileTypes, String targetFolder, File fileList, JFrame parent) {
        topDir = dirs;
        okFileExtention = fileTypes;
        targetDir = targetFolder.replace("\\", "\\\\");
        this.fileList = fileList;
        parentApp = (AuditCompanion) parent;
        // counter 초기화
        fileSize = 0;
        extractFileCount = 0;
        totalFileCount = 0;
        dirCount = 0;
        lapTime = 0;        
    }
    
    public void setParent(JFrame parent) {
        parentApp = (AuditCompanion) parent;
    }

    @Override
    public Void doInBackground() {

        numberOfTopDir = getTopDirCount();
        setProgress(0);
        try {
            //start();
            Date start = new Date();
            bw = new BufferedWriter(new FileWriter(fileList));
         
            File currentDir;
            File[] files;
            for (String dir : topDir) {
                dir = dir.replace("\\", "\\\\");
                
                if (dir.endsWith("\\")) {
                    // C:\, D:\ ... Drive인 경우
                    currentDir = new File(dir);
                    files = currentDir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.isFile()) {
                                processFile(f);
                            }
                        }
                    }
                } 
                else {  // Directory 인 경우
                    currentDir = new File(dir);                    
                    files = currentDir.listFiles();
                    // Null if System Volumn Information
                    if (files != null) {
                        for (File f : files) {
                            try {
                                if (f.isDirectory()) {
                                    Date end = new Date();
                                    lapTime = (end.getTime() - start.getTime()) / 1000;
                                    //printProcessingInfo(f);
                                    parentApp.setMessage(String.format("%,d Files (%.1f MB) found in %,d Dirs, "
                                       + "%,d Files --> next folder [%s] : %.1f Seconds ...%n",
                                        getExtractFileCount(), getFileSize() / 1048567,
                                        getDirCount(), getTotalFileCount(), f.getAbsolutePath(), lapTime));                         
                                    completedTopDir++;
                                    progress = (completedTopDir / numberOfTopDir) * 100;
                                    setProgress(Math.min(progress, 100));
                                    processFile(f);
                                } else if (f.isFile()) {
                                    processFile(f);
                                }
                            } catch (NullPointerException e) {
                                System.out.println("Null Poniter EX: " + e.getMessage());
                                continue;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    @Override
    public void done() {
        parentApp.setCursor(null);
        parentApp.setMessage(String.format("Completed : %d files found. (Information file : %s)%n", 
                extractFileCount, fileList.getAbsolutePath()));
        // System.out.format("Please refer to the processing info file : %s%n", fileList.getAbsolutePath());
        try {
            bw.close();
            parentApp.setInfoTable(fileList);
        } catch (IOException ex) {
            System.out.println(ex);
        }
        parentApp.setStage(Stage.ANALYZE_COMPLETED);
    }
    
     public int getTopDirCount (){
        return topDir.size();
    }
     
    public void processFile(File file) throws IOException {
        // make list containing only office files and directory.
        try {
            if (file.isDirectory()) {
                dirCount += 1;
                File[] files = file.listFiles();
                for (File f : files) {
                    processFile(f);
                }
            }
        } catch (NullPointerException e) {
            // it happens when directory is $RECYCLE.BIN, System Volume Information, etc.
            System.out.println("Null Poniter: " + e.getMessage());
            return;
        } 
        
        if (file.isFile()) {
            if (isOfficeFile(file)) {
                String hashValue = MyUtil.getHashCode(file); 
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    bw.write(file.getAbsolutePath() + "/" + file.length() + "/" + 
                            formatter.format(file.lastModified()) + "/" + hashValue);
                    bw.newLine();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
                fileSize += file.length();
                extractFileCount++;
            }
            totalFileCount++;
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
	
    public void printProcessingInfo(File dir) {
        System.out.format("%,d Files (%.1f MB) extracted in %,d Dirs, "
                + "%,d Files --> next folder [%s] : %.1f Seconds ...%n",
                getExtractFileCount(), getFileSize() / 1048567,
                getDirCount(), getTotalFileCount(), dir.getAbsolutePath(), lapTime);
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

    public int getCompletedTopDir() {
        return completedTopDir;
    }    
}
