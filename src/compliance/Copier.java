/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compliance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import javax.swing.SwingWorker;
import compliance.AuditCompanion.Stage;

/**
 *
 * @author hohkim
 */
public class Copier extends SwingWorker<Void,Void> {
    private MyTableModel model;
    private AuditCompanion companion;
    private String targetFileDir;

    public Copier (MyTableModel model, AuditCompanion companion) {
        this.model = model;
        this.companion = companion;
        this.targetFileDir = companion.getFileDir();
    }
    
    @Override
    protected Void doInBackground() {
        
        int copy_count = 0;
        int current_count = 0;
        int row_cnt = model.getRowCount();
        
        for (int i= 0; i < row_cnt ; i++) {
            if ((Boolean) model.getValueAt(i, 0)){
                copy_count++;
            }
        }   
        Object[][] data1 = new Object[copy_count][6];       
        FileInputStream fis;
        FileOutputStream fos;
        FileChannel fc_in;
        FileChannel fc_out;
        File file1;
        File file2;
        for (int i = 0; i < row_cnt ; i++) {
            if ((Boolean) model.getValueAt(i, 0)) {
                String src = model.getValueAt(i, 1).toString();               
                try {
                    file1 = new File(src.replace("\\", "\\\\"));
                    fis = new FileInputStream(file1);
                    file2 = new File(targetFileDir.replace("\\", "\\\\"), file1.getName());
                    fos = new FileOutputStream(file2);
                    fc_in  = fis.getChannel();
                    fc_out = fos.getChannel();
                    fc_in.transferTo(0, fc_in.size(), fc_out);
                    fc_out.close();
                    fc_in.close(); 
                    fos.close();
                    fis.close();                        
                    for (int j = 0; j < 5 ; j++) {
                        if (j != 1) {
                            data1[current_count][j] = model.getValueAt(i, j);
                        }
                    }             
                    data1[current_count][1] = file2.getAbsolutePath();
                    data1[current_count][5] = MyUtil.verifyHash(file2.getAbsolutePath(), 
                            model.getValueAt(i, 4).toString());                    
                    current_count++;
                    // System.out.format("Copied (%d of %d) : %s --> %s\n", 
                    //         current_count, copy_count, file1.getAbsolutePath(), file2.getAbsolutePath());
                    companion.setMessage(String.format("Copied (%d of %d): %s --> %s", 
                            current_count, copy_count, file1.getAbsolutePath(), file2.getAbsolutePath()));             

                } catch (NullPointerException ex) {
                    System.out.println("Null Pointer Exception : " + ex.getMessage());
                } catch (SecurityException ex) {
                    System.out.println("Security Exception : " + ex.getMessage());
                } catch (ArrayIndexOutOfBoundsException ex) {
                    System.out.println("Array Index out of bound Exception " + ex.getMessage()); 
                } catch (FileNotFoundException ex) {
                    System.out.println("File not found Exception " + ex.getMessage());
                } catch (IOException ex) {
                    System.out.println("Io Exception : " + ex.getMessage());
                } 
            }
        }
        companion.setMessage(String.format("Copy completed : total %d files.", current_count));

        // InfoTable Update
        String[] columnNames = {"Select", "File Name", "Size(KB)", "Modified", "Hash Code", "Verify"};        
        model = new MyTableModel(data1, columnNames);
        companion.setInfoTable(model);
        companion.setInfoDataAndHeader(data1, columnNames);
        return null;
    }
    /**
     *
     */
    @Override
    public void done() {
        companion.setCursor(null);
        companion.setStage(Stage.COPY_COMPLETED);
    }
}
