/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.cmu.pslc.learnsphere.imports.xAPI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMappingException;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder;
import com.github.opendevl.JFlat;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.json.JSONArray;

/**
 *
 * @author Liang Zhang
 */
public class Json2table {
    private JSONArray sqlStatements;
    private File outputDirectory;
    
    public File nestedJson2csv(JSONArray sqlStatements,File outputDirectory) throws IOException, CsvMappingException, Exception{
        File tempFile= new File(outputDirectory.getAbsoluteFile() + "/xAPI_Query.txt");
        JFlat flatMe = new JFlat(sqlStatements.toString());
        List<Object[]> json2csv = flatMe.json2Sheet().getJsonAsSheet();
        //flatMe.json2Sheet().headerSeparator("_").write2csv(outputDirectory.getAbsoluteFile() + "/xAPI_Query.txt");
        flatMe.headerSeparator("_").write2csv(outputDirectory.getAbsoluteFile() + "/xAPI_Query.txt", '\t');
        return tempFile;
    }
    
    public File Json2csv(JSONArray sqlStatements,File outputDirectory) throws IOException, CsvMappingException{
        JsonNode jsonTree = new ObjectMapper().readTree(sqlStatements.toString());
        
        Builder csvSchemaBuilder = CsvSchema.builder();
        JsonNode firstObject = jsonTree.elements().next();
        
        firstObject.fieldNames().forEachRemaining(fieldName -> {csvSchemaBuilder.addColumn(fieldName);});
        
        CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
        
        File tempFile= new File(outputDirectory.getAbsoluteFile() + "/xAPI_Query.txt");
        CsvMapper csvMapper = new CsvMapper();
        csvMapper.writerFor(JsonNode.class)
          .with(csvSchema)
          .writeValue(tempFile, jsonTree);
        
        return tempFile; 
    }
    
}
