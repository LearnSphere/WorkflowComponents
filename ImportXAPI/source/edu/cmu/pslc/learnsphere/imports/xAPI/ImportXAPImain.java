package edu.cmu.pslc.learnsphere.imports.xAPI;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.learnsphere.imports.xAPI.JsonFlattener;
import edu.cmu.pslc.learnsphere.imports.xAPI.TabTextWriter;

import com.google.gson.Gson;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
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
                StatementResult results =null;
                Statement statement = new Statement();
                
	        switch (filter) {
	        case "Null":
	        	break;
	        case "filterByVerb":
	        	results=client.filterByVerb(filterValue).getStatements();
	        	break;
	        case "filterByActor":
	        	Actor actor = new Agent();
	        	actor.setName(filterValue);
	        	results=client.filterByActor(actor).getStatements();
	        case "filterByActivity":
	        	results=client.filterByActivity(filterValue).getStatements();
	        	break;
	        case "filterByRegistration":
	        	results=client.filterByRegistration(filterValue).getStatements();
	        	break;
	        case "filterBySince":
	        	results=client.filterBySince(filterValue).getStatements();
	        	break;
	        case "filterByUntil":
	        	results=client.filterByUntil(filterValue).getStatements();
	        	break;
	        case "Custom":
	        	client.addFilter(customfilter,filterValue);
	        	break;
	        default:
	            this.addErrorMessage("Invalid filter type");
	        }
           
	    	// Retrieving xAPI statements
	        try {
//		    	StatementResult results = client.getStatements();
				//StatementResult nextPage = client.getStatements(results.getMore());  
				Object object= results.getStatements();
				Gson gson = new Gson();
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
               
               int rows=array.length;
               int columns=array[0].length;
               List<String> items = new ArrayList<String>();
               
               Set<String> headers = collectHeaders(flatJson);
               List mainKeys= new ArrayList();
               int count=0; 
               for (String index : headers){
                    count++;
                    mainKeys.add(index);
                }
               
               for (int row=0;row<rows;row++){
                   for (int column=0;column<columns;column++){
                       if(mainKeys.contains(array[row][column])){
                           mainKeys.remove(array[row][column]);
                       }
                   }
               }
               
                 File generatedFile_0 = this.createFile("xAPI-JsonFlattener-file", ".txt");
                 FileWriter oStream_0 = new FileWriter(generatedFile_0);
	         BufferedWriter sw_0 = new BufferedWriter(oStream_0);
	         sw_0.write(writer.writeAsTxt(flatJson));
                 
               int length=mainKeys.size();
               for (int k=0;k<mainKeys.size();k++){
                   for (Map<String, String> map: flatJson){
                       if (map.containsKey(mainKeys.get(k))){
                            map.remove(mainKeys.get(k));
                            for (int row=0;row<rows;row++){
                                String key=array[row][0];
                                String value=map.get(array[row][1]);
                                map.put(key, value);
                            }
                        }
                    }
               }
               
               
               for (int r=0;r<rows;r++){
                   for (Map<String, String> map: flatJson){
                       map.remove(array[r][1]);
                   }  
               }
               

	        File generatedFile = this.createFile("xAPI-TabDelimited-file", ".txt");
	        FileWriter oStream = new FileWriter(generatedFile);
	        BufferedWriter sw = new BufferedWriter(oStream);
	        sw.write(writer.writeAsTxt(flatJson));

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

}
