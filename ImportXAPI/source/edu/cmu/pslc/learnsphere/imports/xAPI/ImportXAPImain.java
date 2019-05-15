package edu.cmu.pslc.learnsphere.imports.xAPI;

import com.google.gson.Gson;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;        

import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.ReadContext;

import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;

import gov.adlnet.xapi.client.StatementClient;
import gov.adlnet.xapi.model.Actor;
import gov.adlnet.xapi.model.Agent;
import gov.adlnet.xapi.model.StatementResult;
        
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
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
	        //customfilter = this.getOptionAsString("customFilter");
	        filterValue = this.getOptionAsString("filterValue");
                
	        //Generating required out
	        try {
	        	getXAPIdata(url, username, password, filter, customfilter, filterValue);

	        } catch (Exception e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }

	        for (String err : this.errorMessages) {
	            // These will also be picked up by the workflows platform and relayed to the user.
	            System.err.println(err);
	        }
	    }
            
	    public void getXAPIdata(String url,String username,String password,String filter,String customfilter,String filterValue) throws Exception {
                
                //Get the values for query options
                String queryPath01 = null;
                String headers01 = null;
                queryPath01 = this.getOptionAsString("queryPath01");
                headers01 = this.getOptionAsString("headers01");
                
	    	StatementClient client = new StatementClient(url, username, password);
                client = getStatementClientWithFilter(filter,filterValue, client,customfilter);
                StatementResult results = client.getStatements();
                
                String jsonTxt = null;
                String jsonTxtSpr;
                
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
                        sb.append(gson.toJson(obj));
                     }
                     jsonTxtSpr= sb.toString();
                     jsonTxt = jsonTxtSpr.replace("][",","); //case two stages of statements
	        } catch (Exception e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
                
                JSONArray jsonArray = new JSONArray(jsonTxt); 
                int size = jsonArray.length();
                
                //Split the comma-separaeted queryPath01
                List<String> queryPath01List = Arrays.asList(queryPath01.split(",")); 
                int queryPath01Leth = queryPath01List.size();
                List<String> mainValueList = new ArrayList<String>(); 
                String queryStringValue = null;
         
        //Add tag:headers
        List<String> headersList = new ArrayList<String>();
        headersList.add("Row");
        
        if(headers01.equals("null")){
            headersList.add(queryPath01);
        }else{
            headersList.add(headers01);
        }
        //Replace some header by specific header's name
        
        int cSize=headersList.size();        
        
        //Query the specifc value follow the jsonpath.
        if (queryPath01Leth > 1) {
                for (int i=0; i<size;i++){
                    Object qvalue;
                    JSONObject sts= jsonArray.getJSONObject(i);
                        String queryStringValue0=queryPath01List.get(0);
                        if(sts.has(queryStringValue0)){
                            JSONObject node=sts.getJSONObject(queryStringValue0);
                            if(queryPath01Leth > 2){
                                for (int j=1;j<queryPath01Leth-1;j++){ 
                                    queryStringValue=queryPath01List.get(j);
                                    if(!node.has(queryStringValue)){
                                        node=null;
                                        break;
                                    }else{
                                        node = node.getJSONObject(queryStringValue);
                                    }
                                }
                                if(node==null){
                                    qvalue="null";
                                }else{
                                    queryStringValue=queryPath01List.get(queryPath01Leth-1);
                                    //If qvalue !exists, make qvalue as NA
                                    if(node.has(queryStringValue)){
                                        qvalue = node.get(queryStringValue);
                                    }else{
                                        qvalue="null";
                                    }
                                } 
                            }else{
                                queryStringValue=queryPath01List.get(1);
                                //If qvalue !exists, make qvalue as NA
                                if(node.has(queryStringValue)){
                                    qvalue= node.get(queryStringValue);
                                }else{
                                    qvalue="null";
                                }
                            }
                            mainValueList.add(qvalue.toString());
                            //System.out.println(qvalue);
                        //System.out.println(qvalue);
                        }else{
                            qvalue="null";
                            logger.info("Incorrect Path String from the first node");
                            mainValueList.add(qvalue.toString());
                            //System.out.println(qvalue);
                        }
                }
        } else {
            queryStringValue=queryPath01List.get(0);
            for (int i=0; i<size;i++){
                JSONObject sts= jsonArray.getJSONObject(i);
                if (sts.has(queryStringValue)){
                    Object qvalue= sts.get(queryStringValue);
                    mainValueList.add(qvalue.toString());
                }else{
                Object qvalue=null;
                mainValueList.add("null");
                logger.info("Incorrect Path String");
                }
            }
        }
        
        //Add row count        
        ArrayList<String> row=new ArrayList<String>();
        int count=0;
        for (int c=0;c<size;c++){
            count++;
            row.add(String.valueOf(count));
        }
        
        String [][] queryContent=new String[size][cSize];
        
        //List add list
        //List<List<String>> vecvecRes = new ArrayList<List<String>>();
        //vecvecRes.add(row);
        //vecvecRes.add(mainValueList);
        //for (List<String> subList:vecvecRes){
        //    System.out.println(subList);
        //}
        
        Map<Integer , List<String>> map = new HashMap<Integer , List<String>>();
        map.put(0,row);
        for (int cr=1;cr<cSize;cr++){
            map.put(cr,mainValueList);
        }
        
        for (int crf=0;crf<cSize;crf++){
            for(int crr=0;crr<size;crr++){
                queryContent[crr][crf]=map.get(crf).get(crr);
            }
        }
        
        System.out.println(queryContent[28643][1]);
        
        File jsonFile = this.createFile("xAPI_Query", ".txt");
        FileWriter fw = new FileWriter(jsonFile.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        for(String h:headersList){
            bw.write(h+"\t");
        }
        bw.write("\n");
        for(int crw=0;crw<size;crw++){
            for(String qc:queryContent[crw]){
                bw.write(qc+"\t");
            }
        bw.write("\n");
        }
        bw.close();

        Integer nodeIndex0 = 0;
        Integer fileIndex0 = 0;
        String fileType0 = "tab-delimited";
        this.addOutputFile(jsonFile, nodeIndex0, fileIndex0, fileType0);
        System.out.println(this.getOutput()); 
          
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
            
            private Set<String> collectHeaders(List<Map<String, String>> flatJson) {
                Set<String> headers = new TreeSet<String>();
                for (Map<String, String> map : flatJson) {
                     headers.addAll(map.keySet());
                }
            return headers;
            }
            
            
}