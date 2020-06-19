package edu.cmu.pslc.learnsphere.imports.xAPI;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;        
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
                
                //Get the option parameters
                String lrsUrl=this.getOptionAsString("url")+"statements/";
                String lrsUsername=this.getOptionAsString("username");
                String lrsPassword=this.getOptionAsString("password");
                
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
                    //get the filters funcitons
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
                    filterOptionPathsMap.put("filterByActor","actor.mbox");
                    filterOptionPathsMap.put("filterByVerb","verb.id");
                    filterOptionPathsMap.put("filterBySince","timestamp");
                    filterOptionPathsMap.put("filterByUntil","timestamp");
                    filterOptionPathsMap.put("filterByActivity","context.contextActivities.category.definition.extensions.https://app*`*skoonline*`*org/ITSProfile/Extensions/category.SKOType\"");
                    filterOptionPathsMap.put("filterByStatementId","id");

                    filterValuesComb path= new filterValuesComb();
                    
                    try {
                        sqlUrlWithFilterNew = path.sqlUrlWithFilter(filters,filterOptionPathsMap,filterValues);
                    } catch (JSONException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    }
               }//end of using filter options
                    //System.out.println(sqlUrlWithFilterNew);
                    JSONArray sqlStatements=new JSONArray();
                    StatementClientVeracity qrlByOptionSts =new StatementClientVeracity();

                    try {
                        sqlStatements=qrlByOptionSts.filterByOption(sqlUrlWithFilterNew, lrsUrl, lrsUsername, lrsPassword);
                    } catch (JSONException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    //System.out.println(sqlStatements.length());
                    //System.out.println(sqlStatements);
               
	    }
      
            
    //private getStatementClientWithFilter(String filter, String filterValue, String customfilter){
    //    String outputClient = null;
        
    //    return outputClient;
    //}        
            
}