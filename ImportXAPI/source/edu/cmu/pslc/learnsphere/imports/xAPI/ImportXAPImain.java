package edu.cmu.pslc.learnsphere.imports.xAPI;

import static com.google.common.io.Files.map;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.learnsphere.imports.xAPI.JsonFlattener;
import edu.cmu.pslc.learnsphere.imports.xAPI.TabTextWriter;

import com.google.gson.Gson;
import com.google.gson.*;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import gov.adlnet.xapi.client.StatementClient;
import gov.adlnet.xapi.model.Activity;
import gov.adlnet.xapi.model.ActivityDefinition;
import gov.adlnet.xapi.model.Actor;
import gov.adlnet.xapi.model.Agent;
import gov.adlnet.xapi.model.InteractionComponent;
import gov.adlnet.xapi.model.Statement;
import gov.adlnet.xapi.model.StatementResult;
import gov.adlnet.xapi.model.Verb;
import gov.adlnet.xapi.model.Verbs;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.xml.transform.Result;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

public class ImportXAPImain extends AbstractComponent {

    public static void main(String[] args) {
    	ImportXAPImain tool = new ImportXAPImain();
        tool.startComponent(args);
		}

		public ImportXAPImain() {
		    super();

		}
          
            @Override
	    protected void runComponent() {
	        // Parse arguments
	        //File inputFile = null;
	        String username = null;
	        String password = null;
	        String url = null;
	        String filter = null;
	        String customfilter = null;
	        String filterValue = null;
               
	        username = this.getOptionAsString("username");
	        password = this.getOptionAsString("password");
	        url = this.getOptionAsString("url");
	        filter = this.getOptionAsString("filter");
	        customfilter = this.getOptionAsString("customFilter");
	        filterValue = this.getOptionAsString("filterValue");

	        //Generating required out
	        try {
	        	getXAPIdata(url, username, password, filter, customfilter, filterValue);

	        } catch (Exception e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }

	        System.out.println(this.getOutput());

	        for (String err : this.errorMessages) {
	            // These will also be picked up by the workflows platform and relayed to the user.
	            System.err.println(err);
	        }

	        Integer nodeIndex = 0;
                Integer fileIndex = 0;
                String fileType = "text";
	    }
 
	    public void getXAPIdata(String url,String username,String password,String filter,String customfilter,String filterValue) throws Exception {
                
	    	StatementClient client = new StatementClient(url, username, password);
	    	String jsonTxt =null;
                StatementResult results = null;
                client = getStatementClientWithFilter(filter,filterValue, client,customfilter);
                results = client.getStatements();
                
	    	// Retrieving xAPI statements
	        try {
                     StringBuilder sb = new StringBuilder();
                     Object object= results.getStatements();
                     Gson gson = new Gson();
	             sb.append(gson.toJson(object));
                     while(results.hasMore()){
                        String moreString = results.getMore();
                        moreString = moreString.replace("/data/xAPI", "");
                        results = client.getStatements(moreString);
                        Object obj = results.getStatements();
                        sb.append(gson.toJson(object));
                     }  
			
                     jsonTxt= gson.toJson(object);
	        } catch (Exception e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
                
                //import Configuration File
                File theFile = this.getAttachment(0, 0);
                
                //Read Configuration File as list
                List<String> list = new ArrayList<String>();
                    try {
                       if (theFile.isFile() && theFile.exists()) {
                         InputStreamReader read = new InputStreamReader(new FileInputStream(theFile));
                         BufferedReader bufferedReader = new BufferedReader(read);
                         String lineTxt = null;
                         
                         while ((lineTxt = bufferedReader.readLine()) != null) {
                             if (!lineTxt.startsWith("#"))
                                 list.add(lineTxt); 
                         }
                         read.close();
                       } else{
                       System.out.println("Configuration file missing");
                       }
                    }catch(IOException e){
                        System.out.println("Error Happened");
                    } 

                    //Read Configuration File (list to array)
                    String array[][] = new String[list.size()][];
                    for (int i=0;i<list.size();i++){
                        array[i]=new String[2];
                        String linetxt=list.get(i);
                        String[] myArray=linetxt.split("=");
                        System.arraycopy(myArray, 0, array[i], 0, myArray.length);
                    }
                
                //writer.writeAsTxt(flatJson, "sample.txt");
	    	JsonFlattener parser = new JsonFlattener();
                TabTextWriter writer = new TabTextWriter();
	        List<Map<String, String>> flatJson = parser.parseJson(jsonTxt);
               
                File generatedFile_0 = this.createFile("xAPI-JsonFlattener-file", ".txt");
                FileWriter oStream_0 = new FileWriter(generatedFile_0);
	        BufferedWriter sw_0 = new BufferedWriter(oStream_0);
	        sw_0.write(writer.writeAsTxt(flatJson));
                
                
               int rows=array.length;
               int columns=array[0].length;
               List<String> items = new ArrayList<String>();
               
               //Write headers as list
               Set<String> headers = collectHeaders(flatJson);
               List<String> mainKeys= new ArrayList<String>();
               int count=0; 
               for (String index : headers){
                    count++;
                    mainKeys.add(index);
                }
                       
               //Headers: List to array
               String[] tabNames=new String[mainKeys.size()];
               for (int i=0; i<mainKeys.size();i++){
                   tabNames[i]= mainKeys.get(i);
               }
               
               //Values filled into array matrix 
               List<String> mainValueList = new ArrayList<String>();
               String [][] mainContent=new String[tabNames.length][];
               for (int k=0;k<tabNames.length;k++){
                    mainValueList.clear();
                    for (Map<String, String> map: flatJson){
                       String mainValue=map.get(tabNames[k]);
                       if(mainValue == null){
                           mainValue = "null";
                       }
                       mainValueList.add(mainValue); 
                    } 
                    
                    Object[] mainValueArr= mainValueList.toArray();
                    
                    String mainValueArrStr[]=new String[mainValueArr.length];
                    System.arraycopy(mainValueArr, 0, mainValueArrStr,0, mainValueArr.length);
                    mainContent[k]=mainValueArrStr;
               }
                
               //remove the "-" symbol of id
               for(int k=0;k<tabNames.length;k++){
                   if(tabNames[k].equals("id")){
                       for(int rs=0;rs<mainContent[k].length;rs++){
                           mainContent[k][rs]=mainContent[k][rs].replace("-","");
                       }
                   }
               }
              
               //Transfer Time Format
               for(int k=0;k<tabNames.length;k++){
                   if(tabNames[k].equals("stored")){
                       for(int rs=0;rs<mainContent[k].length;rs++){
                           SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                           Date date = dt.parse(mainContent[k][rs]);
                           SimpleDateFormat dt1 = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                           mainContent[k][rs]=dt1.format(date);
                       }
                   }
               }               
                            
               //Create array matrix about selected columns
                String [][] selectContent=new String[array.length][];
                for(int sc=0;sc<array.length;sc++){
                    for (int k=0;k<tabNames.length;k++){
                        if(array[sc][1].equals(tabNames[k])){
                            selectContent[sc]=mainContent[k];    
                        }
                    }
                }
                            
               	File generatedFile = this.createFile("xAPI-TabDelimited-file", ".txt");
                    FileWriter fw = new FileWriter(generatedFile.getAbsoluteFile());
                    try (BufferedWriter bw = new BufferedWriter(fw)) {
//                    bw.write(key, 0, key.length());
                        for (String[] array1 : array) {
                            bw.write(array1[0]+"\t");
                        }
                        
                        for (int col=0;col<selectContent.length;col++){
                             for(int rs=0;rs<selectContent[col].length;rs++){
                             bw.newLine();
                             for (String[] array2:selectContent){
                             bw.write(array2[rs]+"\t");
                            }
                            }                          
                        }
                    }
                
	    Integer nodeIndex = 0;
            Integer fileIndex = 0;
            String fileType = "text";
            this.addOutputFile(generatedFile, nodeIndex, fileIndex, fileType);
	    }
            
            private Set<String> collectHeaders(List<Map<String, String>> flatJson) {
                Set<String> headers = new TreeSet<String>();
                for (Map<String, String> map : flatJson) {
                     headers.addAll(map.keySet());
                }
            return headers;
            }

            private StatementClient getStatementClientWithFilter(String filter,String filterValue, StatementClient client,String customfilter){
                StatementClient outputClient = null;
                try{
                    switch (filter) {
                        case "Null":
                                break;
                        case "filterByVerb":
                                outputClient=client.filterByVerb(filterValue);
                                break;
                        case "filterByActor":
                                Actor actor = new Agent(null,filterValue);
                                outputClient=client.filterByActor(actor);
                                break;
                        case "filterByActivity":
                                outputClient=client.filterByActivity(filterValue);
                                break;
                        case "filterByRegistration":
                                outputClient=client.filterByRegistration(filterValue);
                                break;
                        case "filterBySince":
                                outputClient=client.filterBySince(filterValue);
                                break;
                        case "filterByUntil":
                                outputClient=client.filterByUntil(filterValue);
                                break;
                        case "Custom":
                                outputClient = client.addFilter(customfilter,filterValue);
                                break;
                        default:
                            this.addErrorMessage("Invalid filter type");
                    }
                    return outputClient;
                }catch(Exception e){
                    logger.fatal(e.toString());
                    return null;
                }
            }            
            
}
