package edu.cmu.pslc.learnsphere.imports.xAPI;

import com.github.opendevl.JFlat;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;        

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ImportXAPImain extends AbstractComponent {

    public static void main(String[] args) {
    	ImportXAPImain tool = new ImportXAPImain();
        tool.startComponent(args);
		}

                //Constructor
		public ImportXAPImain() {
		    super();

		}
          
            @Override
	    protected void runComponent() {
                
                File outputDirectory = this.runExternal();
                
                //Get the option parameters
                String lrsUrl=this.getOptionAsString("url")+"statements/";
                String lrsUsername=this.getOptionAsString("username");
                String lrsPassword=this.getOptionAsString("password");
                String queryMode=null; //v2,aggregate
                
                //Get the use_customfilter infor.
                JSONObject sqlUrlWithFilterNew = new JSONObject();
                Boolean useCustomfilter=this.getOptionAsBoolean("Use_customfilter");
               if(useCustomfilter){
                   String customfilterCode=this.getOptionAsString("customfilter_code");
                   
                    try {
                        sqlUrlWithFilterNew=new JSONObject(customfilterCode);
                        //JSONObject json = JSONObject.fromObject(str);
                    } catch (JSONException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                   
                   //System.out.println(json);
                   //System.out.println(sqlUrlWithFilterNew);
               }else{
                    //If the All_statements=false (by default),it will help us query the specified values in the statements based on the json fields.
                    Boolean All_statements=this.getOptionAsBoolean("All_statements");
                    
                    if(!All_statements){
                        queryMode="usingAggregate";
                        String filterByUntil=this.getOptionAsString("filterByUntil");
                        String filterBySince=this.getOptionAsString("filterBySince");
                        String group="$statement.verb.id";
                        
                        
                    }
                    
                    //get the filters path funcitons
                    List<String> filters = null;
                    if (this.getOptionAsList("filter") != null) {
                            filters = this.getOptionAsList("filter");    
                            logger.info("filters: " + filters);
                    }

                    //get the filter values from option paths
                    List<String> filterValues=new ArrayList<String>();
                    String filterValue01=this.getOptionAsString("filterValue01");
                    if (!filterValue01.equals("null")){
                            filterValues.add(filterValue01);}
                    String filterValue02=this.getOptionAsString("filterValue02");
                    if (!filterValue02.equals("null")){
                            filterValues.add(filterValue02);}
                    String filterValue03=this.getOptionAsString("filterValue03");
                    if (!filterValue03.equals("null")){
                            filterValues.add(filterValue03);}
                    String filterValue04=this.getOptionAsString("filterValue04");
                    if (!filterValue04.equals("null")){
                            filterValues.add(filterValue04);}
                    String filterValue05=this.getOptionAsString("filterValue05");
                    if (!filterValue05.equals("null")){
                            filterValues.add(filterValue05);}
                    String filterValue06=this.getOptionAsString("filterValue06");
                    if (!filterValue06.equals("null")){
                            filterValues.add(filterValue06);}
                    String filterValue07=this.getOptionAsString("filterValue07");
                    if (!filterValue07.equals("null")){
                            filterValues.add(filterValue07);}
                    String filterValue08=this.getOptionAsString("filterValue08");
                    if (!filterValue08.equals("null")){
                            filterValues.add(filterValue08);}
                    String filterValue09=this.getOptionAsString("filterValue09");
                    if (!filterValue09.equals("null")){
                            filterValues.add(filterValue09);}
                    String filterValue10=this.getOptionAsString("filterValue10");
                    if (!filterValue10.equals("null")){
                            filterValues.add(filterValue10);}

                    //Create one hashmap for storing the filterByActor, filterByVerb,filterBySince,filterByUntil,filterByActivity,filterByRegistration,filterByStatementId
                    Map<String,String> filterOptionPathsMap = new HashMap<String,String>();
                    //filterOptionPathsMap.put("filterByActor","statement.actor.mbox");
                    filterOptionPathsMap.put("filterByActor","statement.actor.name");
                    filterOptionPathsMap.put("filterByVerb","statement.verb.id");
                    filterOptionPathsMap.put("filterBySince","statement.timestamp");
                    filterOptionPathsMap.put("filterByUntil","statement.timestamp");
                    filterOptionPathsMap.put("filterByActivity","statement.context.contextActivities.category.definition.extensions.https://app*`*skoonline*`*org/ITSProfile/Extensions/category.SKOType\"");
                    filterOptionPathsMap.put("filterByStatementId","statement.id");
                    
                    filterValuesComb path= new filterValuesComb();
                    
                    try {
                        sqlUrlWithFilterNew = path.sqlUrlWithFilter(filters,filterOptionPathsMap,filterValues);
                    } catch (JSONException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    }
               }//end of using filter options
                    System.out.println(sqlUrlWithFilterNew);
                    JSONArray sqlStatements=new JSONArray();
                    StatementClientVeracity qrlByOptionSts =new StatementClientVeracity(); 

                    try {
                        sqlStatements=qrlByOptionSts.filterByOption(sqlUrlWithFilterNew, lrsUrl, lrsUsername, lrsPassword, queryMode);
                    } catch (JSONException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    //System.out.println(sqlStatements.length());
                    //System.out.println(sqlStatements);
                    
                    File resultFile=null;
                    Json2table queryFile=new Json2table();
                    try {
                        resultFile=queryFile.nestedJson2csv(sqlStatements, outputDirectory);
                    } catch (Exception ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    Integer nodeIndex0 = 0;
                    Integer fileIndex0 = 0;
                    String fileType0 = "tab-delimited";
                    this.addOutputFile(resultFile, nodeIndex0, fileIndex0, fileType0);
                    System.out.println(this.getOutput()); 
	    } 
            
}