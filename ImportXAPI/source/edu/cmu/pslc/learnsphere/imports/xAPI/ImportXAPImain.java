package edu.cmu.pslc.learnsphere.imports.xAPI;

import com.google.gson.Gson;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;        

import java.util.List;
import java.util.Map;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import gov.adlnet.xapi.client.StatementClient;
import gov.adlnet.xapi.model.Actor;
import gov.adlnet.xapi.model.Agent;
import gov.adlnet.xapi.model.StatementResult;
import gov.adlnet.xapi.model.*;
        
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
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
                
                String url = null;
	        String username = null;
	        String password = null;
	        
	        String filter = null;
	        String filterValue = null;
                String customfilter = null;
               
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
                String queryPath02 = null;
                String headers02 = null;
                String queryPath03 = null;
                String headers03 = null;
                String queryPath04 = null;
                String headers04 = null;
                String queryPath05 = null;
                String headers05 = null;
                String queryPath06 = null;
                String headers06 = null;
                String queryPath07 = null;
                String headers07 = null;
                String queryPath08 = null;
                String headers08 = null;                
                String queryPath09 = null;
                String headers09 = null;
                String queryPath10 = null;
                String headers10 = null;
                String queryPath11 = null;
                String headers11 = null;
                String queryPath12 = null;
                String headers12 = null;
                String queryPath13 = null;
                String headers13 = null;
                String queryPath14 = null;
                String headers14 = null;
                String queryPath15 = null;
                String headers15 = null;
                String queryPath16 = null;
                String headers16 = null;                
                String queryPath17 = null;
                String headers17 = null;
                String queryPath18 = null;
                String headers18 = null;
                String queryPath19 = null;
                String headers19 = null;
                String queryPath20 = null;
                String headers20 = null;
                String queryPath21 = null;
                String headers21 = null;
                String queryPath22 = null;
                String headers22 = null;
                String queryPath23 = null;
                String headers23 = null;
                String queryPath24 = null;
                String headers24 = null;                
                String queryPath25 = null;
                String headers25 = null;
                String queryPath26 = null;
                String headers26 = null;
                String queryPath27 = null;
                String headers27 = null;
                String queryPath28 = null;
                String headers28 = null;
                String queryPath29 = null;
                String headers29 = null;
                String queryPath30 = null;
                String headers30 = null;                
                
                queryPath01 = this.getOptionAsString("queryPath01");
                    headers01 = this.getOptionAsString("headers01");
                queryPath02 = this.getOptionAsString("queryPath02");
                    headers02 = this.getOptionAsString("headers02");                
                queryPath03 = this.getOptionAsString("queryPath03");
                    headers03 = this.getOptionAsString("headers03");                
                queryPath04 = this.getOptionAsString("queryPath04");
                    headers04 = this.getOptionAsString("headers04");                
                queryPath05 = this.getOptionAsString("queryPath05");
                    headers05 = this.getOptionAsString("headers05");
                queryPath06 = this.getOptionAsString("queryPath06");
                    headers06 = this.getOptionAsString("headers06");
                queryPath07 = this.getOptionAsString("queryPath07");
                    headers07 = this.getOptionAsString("headers07");
                queryPath08 = this.getOptionAsString("queryPath08");
                    headers08 = this.getOptionAsString("headers08");
                queryPath09 = this.getOptionAsString("queryPath09");
                    headers09 = this.getOptionAsString("headers09");
                queryPath10 = this.getOptionAsString("queryPath10");
                    headers10 = this.getOptionAsString("headers10");                
                queryPath11 = this.getOptionAsString("queryPath11");
                    headers11 = this.getOptionAsString("headers11");                
                queryPath12 = this.getOptionAsString("queryPath12");
                    headers12 = this.getOptionAsString("headers12");                
                queryPath13 = this.getOptionAsString("queryPath13");
                    headers13 = this.getOptionAsString("headers13");
                queryPath14 = this.getOptionAsString("queryPath14");
                    headers14 = this.getOptionAsString("headers14");
                queryPath15 = this.getOptionAsString("queryPath15");
                    headers15 = this.getOptionAsString("headers15");
                queryPath16 = this.getOptionAsString("queryPath16");
                    headers16 = this.getOptionAsString("headers16");
                queryPath17 = this.getOptionAsString("queryPath17");
                    headers17 = this.getOptionAsString("headers17");
                queryPath18 = this.getOptionAsString("queryPath18");
                    headers18 = this.getOptionAsString("headers18");
                queryPath19 = this.getOptionAsString("queryPath19");
                    headers19 = this.getOptionAsString("headers19");                
                queryPath20 = this.getOptionAsString("queryPath20");
                    headers20 = this.getOptionAsString("headers20");                
                queryPath21 = this.getOptionAsString("queryPath21");
                    headers21 = this.getOptionAsString("headers21");                
                queryPath22 = this.getOptionAsString("queryPath22");
                    headers22 = this.getOptionAsString("headers22");
                queryPath23 = this.getOptionAsString("queryPath23");
                    headers23 = this.getOptionAsString("headers23");
                queryPath24 = this.getOptionAsString("queryPath24");
                    headers24 = this.getOptionAsString("headers24");
                queryPath25 = this.getOptionAsString("queryPath25");
                    headers25 = this.getOptionAsString("headers25");               
                queryPath26 = this.getOptionAsString("queryPath26");
                    headers26 = this.getOptionAsString("headers26");
                queryPath27 = this.getOptionAsString("queryPath27");
                    headers27 = this.getOptionAsString("headers27");
                queryPath28 = this.getOptionAsString("queryPath28");
                    headers28 = this.getOptionAsString("headers28");
                queryPath29 = this.getOptionAsString("queryPath29");
                    headers29 = this.getOptionAsString("headers29");
                queryPath30 = this.getOptionAsString("queryPath30");
                    headers30 = this.getOptionAsString("headers30");  
                
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
        
                //Collect all the queryPaths and headers
                List<String> allPathsList=new ArrayList<String>();
                List<String> allHeadersList=new ArrayList<String>();        

                allPathsList.add(queryPath01);
                allPathsList.add(queryPath02);
                allPathsList.add(queryPath03);
                allPathsList.add(queryPath04);
                allPathsList.add(queryPath05);
                allPathsList.add(queryPath06);
                allPathsList.add(queryPath07);
                allPathsList.add(queryPath08);
                allPathsList.add(queryPath09);
                allPathsList.add(queryPath10); 
                allPathsList.add(queryPath11);
                allPathsList.add(queryPath12);
                allPathsList.add(queryPath13);
                allPathsList.add(queryPath14);
                allPathsList.add(queryPath15);
                allPathsList.add(queryPath16);
                allPathsList.add(queryPath17);
                allPathsList.add(queryPath18);
                allPathsList.add(queryPath19);
                allPathsList.add(queryPath20);         
                allPathsList.add(queryPath21); 
                allPathsList.add(queryPath22);
                allPathsList.add(queryPath23);
                allPathsList.add(queryPath24);
                allPathsList.add(queryPath25);
                allPathsList.add(queryPath26);
                allPathsList.add(queryPath27);
                allPathsList.add(queryPath28);
                allPathsList.add(queryPath29);
                allPathsList.add(queryPath30);          

                allHeadersList.add(headers01);
                allHeadersList.add(headers02);
                allHeadersList.add(headers03);
                allHeadersList.add(headers04);
                allHeadersList.add(headers05);
                allHeadersList.add(headers06);
                allHeadersList.add(headers07);
                allHeadersList.add(headers08);        
                allHeadersList.add(headers09);
                allHeadersList.add(headers10);
                allHeadersList.add(headers11);
                allHeadersList.add(headers12);
                allHeadersList.add(headers13);
                allHeadersList.add(headers14);
                allHeadersList.add(headers15);
                allHeadersList.add(headers16);
                allHeadersList.add(headers17);
                allHeadersList.add(headers18);
                allHeadersList.add(headers19);
                allHeadersList.add(headers20);        
                allHeadersList.add(headers21);
                allHeadersList.add(headers22);
                allHeadersList.add(headers23);
                allHeadersList.add(headers24); 
                allHeadersList.add(headers25);
                allHeadersList.add(headers26);        
                allHeadersList.add(headers27);
                allHeadersList.add(headers28);
                allHeadersList.add(headers29);
                allHeadersList.add(headers30);               

                //Add row count        
                ArrayList<String> row=new ArrayList<String>();
                int count=0;
                for (int c=0;c<size;c++){
                    count++;
                    row.add(String.valueOf(count));
                }

                //Add tag:headers
                //Add count row
                List<String> headersList = new ArrayList<String>();
                List<String> selectPathsList = new ArrayList<String>();
                headersList.add("Row");

                //Replace some header by specific header's name
                for(int n=0;n<allHeadersList.size();n++){
                    String queryPath00=allPathsList.get(n);
                    String headers00=allHeadersList.get(n);
                    if(!queryPath00.equals("null")){
                        selectPathsList.add(queryPath00);
                        if(!headers00.equals("null")){
                            headersList.add(headers00);
                        }else{
                            headersList.add(queryPath00);
                        }
                    }
                }

                //Add query contents
                Map<Integer , List<String>> map = new HashMap<Integer , List<String>>();
                map.put(0,row);

                for(int j=0;j<selectPathsList.size();j++){  
                    //Split the comma-separaeted queryPath

                    List<String> queryPath00List = Arrays.asList(selectPathsList.get(j).split(",")); 
                    int queryPath00Leth = queryPath00List.size();
                    List<String> mainValueList = new ArrayList<String>();
                    String queryStringValue = null;
                    Object qvalue=null;

                    //Query the specifc value follow the jsonpath.
                    if (queryPath00Leth > 1) {
                            for (int i=0; i<size;i++){
                                JSONObject sts= jsonArray.getJSONObject(i);
                                    String queryStringValue0=queryPath00List.get(0);
                                    //System.out.println(sts.has(queryStringValue0));
                                    if(sts.has(queryStringValue0)){
                                        JSONObject node=sts.getJSONObject(queryStringValue0);
                                        if(queryPath00Leth > 2){
                                            for (int m=1;m<queryPath00Leth-1;m++){ 
                                                queryStringValue=queryPath00List.get(m);
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
                                                queryStringValue=queryPath00List.get(queryPath00Leth-1);
                                                //If qvalue !exists, make qvalue as NA
                                                if(node.has(queryStringValue)){
                                                    qvalue = node.get(queryStringValue);
                                                }else{
                                                    qvalue="null";
                                                }
                                            } 
                                        }else{
                                            queryStringValue=queryPath00List.get(1);
                                            //If qvalue !exists, make qvalue as NA
                                            if(node.has(queryStringValue)){
                                                qvalue= node.get(queryStringValue);
                                                //System.out.println(qvalue);
                                            }else{
                                                qvalue="null";
                                            }
                                        }
                                        //System.out.println(qvalue);
                                        //mainValueList.add(qvalue.toString());
                                    }else{
                                        qvalue="null";
                                        logger.info("Incorrect Path String from the first node");
                                        //mainValueList.add(qvalue.toString());
                                        //System.out.println(qvalue);
                                    }
                                mainValueList.add(qvalue.toString());
                            }
                    } else {
                        queryStringValue=queryPath00List.get(0);
                        for (int i=0; i<size;i++){
                            JSONObject sts= jsonArray.getJSONObject(i);
                            if (sts.has(queryStringValue)){
                                qvalue= sts.get(queryStringValue);
                                mainValueList.add(qvalue.toString());
                            }else{
                                qvalue=null;
                            mainValueList.add("null");
                            logger.info("Incorrect Path String");
                            }
                        }
                    }
                    //System.out.println(mainValueList);
                    //System.out.println(mainValueList);
                    map.put(j+1,mainValueList);      
                }

                int cSize=selectPathsList.size();
                //System.out.println(cSize);
                //System.out.println(selectPathsList.size());
                String [][] queryContent=new String[size][cSize+1];
                //Create matrix for query content
                for (int crf=0;crf<cSize+1;crf++){
                    for(int crr=0;crr<size;crr++){
                        queryContent[crr][crf]=map.get(crf).get(crr);
                    }
                }

                //Print the query results
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