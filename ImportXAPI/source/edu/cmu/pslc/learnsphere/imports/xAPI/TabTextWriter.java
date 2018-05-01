package edu.cmu.pslc.learnsphere.imports.xAPI;

import java.io.BufferedReader;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;


public class TabTextWriter {
    public String writeAsTxt(List<Map<String, String>> flatJson) throws FileNotFoundException {
        Set<String> headers = collectHeaders(flatJson);
        
        String array1 = StringUtils.join(headers.toArray(), "\t") + "\n";
        String output= replaceHeaders(array1);
        for (Map<String, String> map : flatJson) {
            output = output + getCommaSeperatedRow(headers, map) + "\n";
        }
        //writeToFile(output, fileName);
        return output;
    }
    
    private void writeToFile(String output, String fileName) throws FileNotFoundException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(output);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(writer);
        }
    }

    private void close(BufferedWriter writer) {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getCommaSeperatedRow(Set<String> headers, Map<String, String> map) {
        List<String> items = new ArrayList<String>();
        for (String header : headers) {
            String value = "";
            if (map.get(header) != null){
            	value= map.get(header).replace("\t", "").replace("\n", "");
            }
            items.add(value);
        }
        return StringUtils.join(items.toArray(), "\t");
    }

    private Set<String> collectHeaders(List<Map<String, String>> flatJson) {
        Set<String> headers = new TreeSet<String>();
        for (Map<String, String> map : flatJson) {
            headers.addAll(map.keySet());
        }
        return headers;
    }
        private String replaceHeaders(String output){
        output=output.replaceAll("-"," ");
        return output;
    }
       
}
