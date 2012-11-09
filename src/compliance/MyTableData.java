/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compliance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author hohkim
 */
public class MyTableData {
    private String[] header;
    private Object[][] data;
    private int rowCount;
    private int columnCount;
    private int count_type;
    private double totalSize;
    private double size_type;
    private BufferedReader br;
    
    public MyTableData(File file) throws IOException {

        rowCount = 0;
        totalSize = 0;
        columnCount = 5;
        
        // Setting data & count
        try {
            br = new BufferedReader(new FileReader(file));
            while ((br.readLine()) != null) {
                rowCount++;
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            br.close();
        }
        
        data = new Object[rowCount][5];
        String line = "";
        int lineCount = 0;
        try {
            br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {
                String[] temp;
                try {
                    temp = line.split("/");
                    // header initialize
                    if (temp.length == 4) {
                        header = new String[]{"Select", "File Name", "Size(KB)", "Modified", "Hash Code"};
                    } 
                    data[lineCount][0] = true;
                    data[lineCount][1] = temp[0];  // Filename
                    data[lineCount][2] = Integer.parseInt(temp[1])/1024;  // Size in KB    
                    totalSize += Integer.parseInt(temp[1]) / 1024;
                    data[lineCount][3] = temp[2];
                    data[lineCount][4] = temp[3];
                } catch (IndexOutOfBoundsException e) {
                    System.out.print(line + ": " + e.getMessage());
                }
                lineCount++;
            }
        } catch (IOException e) {
            System.out.print(line + ": " + e.getMessage());
        } finally {
            br.close();
        }
    }
      
    public String[] getHeader() {
        return header;
    }
    
    public void setHeader(String[] header) {
        this.header = header;
    }
    
    public Object[][] getData() {
        return data;
    }
    
    public void setData(Object[][] data) {
        this.data = data;
    }
    
    public Object[][] getDataWithType(List<String> exts) {
        Object[][] data1;
        count_type = 0;
        size_type = 0;
        rowCount = data.length;
        // Calculate data count of certain file type
        for (int i=0 ; i < rowCount ; i++) {
            String fileName = (String) data[i][1];
            String ext = fileName.substring(fileName.lastIndexOf(".")+1, fileName.length());
            if (exts.indexOf(ext.toUpperCase()) != -1) {
                count_type++;
                size_type += Double.parseDouble(data[i][2].toString());
            }
        }
        
        data1 = new Object[count_type][columnCount];
        int counter = 0;
        for (int i=0 ; i < rowCount ; i++) {
            String fileName = (String) data[i][1];
            String ext = fileName.substring(fileName.lastIndexOf(".")+1, fileName.length());
            if (exts.indexOf(ext.toUpperCase()) != -1) {
                System.arraycopy(data[i], 0, data1[counter], 0, columnCount);
                counter++;
            }
        }
        return data1;
    }
    
    public Object[][] getDataWithVerify(List<String> exts) {
        Object[][] data1;
        count_type = 0;
        size_type = 0;
        // Calculate data count of certain file type
        for (int i=0 ; i < rowCount ; i++) {
            String fileName = (String) data[i][1];
            String ext = fileName.substring(fileName.lastIndexOf(".")+1, fileName.length());
            if (exts.indexOf(ext.toUpperCase()) != -1) {
                count_type++;
                size_type += Double.parseDouble(data[i][2].toString());
            }
        }
        
        data1 = new Object[count_type][columnCount];
        int counter = 0;
        for (int i=0 ; i < rowCount ; i++) {
            String fileName = (String) data[i][1];
            String ext = fileName.substring(fileName.lastIndexOf(".")+1, fileName.length());
            if (exts.indexOf(ext.toUpperCase()) != -1) {
                System.arraycopy(data[i], 0, data1[counter], 0, columnCount);
                data1[counter][5] = MyUtil.verifyHash(data[i][1].toString(), data[i][4].toString());
                counter++;
            }
        }
        return data1;
    }
    public int getRowCount() {
        return rowCount;
    }
    
    public double getTotalSize() {
        return totalSize;
    }
    
    public void setColumnCount(int count) {
        columnCount = count;
    }
    
    public int getColumnCount() {
        return columnCount;
    }
    
    public int getCountType() {
        return count_type;
    }
    public double getSizeType() {
        return size_type;
    }
    
    public void setHeaderWithVerify (String[] header) {
        this.header = header;
    }
     
    public String[] getHeaderWithVerify() {
        return new String[]{"Select", "File Name", "Size(KB)", "Modified", "Hash Code", "Verify"};
    }
}
