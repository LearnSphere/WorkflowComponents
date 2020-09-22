package edu.cmu.pslc.learnsphere.imports.xAPI;

import com.github.opendevl.JFlat;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;        

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.ParseException;
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
                //String lrsUrl=this.getOptionAsString("url")+"statements/";
                String lrsUrl=this.getOptionAsString("url");
                String lrsUsername=this.getOptionAsString("username");
                String lrsPassword=this.getOptionAsString("password");
                String queryMode=null; //v2,aggregate
                JSONArray sqlStatements=new JSONArray();
                
                //Get the use_customfilter infor.
                JSONObject sqlUrlWithFilterNew = new JSONObject();
                Boolean useCustomfilter=this.getOptionAsBoolean("use_customfilter");
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
                    String FilterOperator=this.getOptionAsString("FilterOperator");
                    String ProcessOperator=this.getOptionAsString("ProcessOperator");
                    
                    ArrayList filterList = new ArrayList();
                    String Filter01=this.getOptionAsString("Filter01");
                    filterList.add(Filter01);
                    String Filter02=this.getOptionAsString("Filter02");
                    filterList.add(Filter02);
                    String Filter03=this.getOptionAsString("Filter03");
                    filterList.add(Filter03);
                    String Filter04=this.getOptionAsString("Filter04");
                    filterList.add(Filter04);
                    String Filter05=this.getOptionAsString("Filter05");
                    filterList.add(Filter05);
                    String Filter06=this.getOptionAsString("Filter06");
                    filterList.add(Filter06);
                    String Filter07=this.getOptionAsString("Filter07");
                    filterList.add(Filter07);
                    String Filter08=this.getOptionAsString("Filter08");
                    filterList.add(Filter08);
                    String Filter09=this.getOptionAsString("Filter09");
                    filterList.add(Filter09);
                    String Filter10=this.getOptionAsString("Filter10");
                    filterList.add(Filter10);

                    ArrayList processList = new ArrayList();
                    String Process01=this.getOptionAsString("Process01");
                    processList.add(Process01);
                    String Process02=this.getOptionAsString("Process02");
                    processList.add(Process02);
                    String Process03=this.getOptionAsString("Process03");
                    processList.add(Process03);
                    String Process04=this.getOptionAsString("Process04");
                    processList.add(Process04);
                    String Process05=this.getOptionAsString("Process05");
                    processList.add(Process05);
                    String Process06=this.getOptionAsString("Process06");
                    processList.add(Process06);
                    String Process07=this.getOptionAsString("Process07");
                    processList.add(Process07);
                    String Process08=this.getOptionAsString("Process08");
                    processList.add(Process08);
                    String Process09=this.getOptionAsString("Process09");
                    processList.add(Process09);
                    String Process10=this.getOptionAsString("Process10");
                    processList.add(Process10);
                    
                    //store the filter values in the HashMap by key:value
                    HashMap<String,String> filterMap = new HashMap<String,String>();
                    for (int i=0; i<filterList.size(); i++) {
                        if(!filterList.get(i).equals("null")){
                            String[] filterSplit=filterList.get(i).toString().split(":");
                            filterMap.put(filterSplit[0], filterSplit[1]);
                        }
                    }
                    
                    //store the process values in HashMap by key:value
                    HashMap<String,String> processMap = new HashMap<String,String>();
                    for (int i=0; i<processList.size(); i++) {
                        if(!processList.get(i).equals("null")){
                            String[] processSplit=processList.get(i).toString().split(":");
                            processMap.put(processSplit[0], processSplit[1]);
                        }
                    }
                    
                    JSONObject sqlUrlVQL=new JSONObject();
                    try {
                        sqlUrlVQL=new filterValuesCombVQL().sqlUrlWithFilter(FilterOperator, filterMap, ProcessOperator, processMap);
                    } catch (JSONException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    StatementClientVeracity qrlByOptionStsVQL =new StatementClientVeracity();
                    try {
                        sqlStatements=qrlByOptionStsVQL.filterByOptionVQL(sqlUrlVQL, queryMode, lrsUrl, lrsUsername, lrsPassword);
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (JSONException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    }
     
                    System.out.println(sqlStatements.toString());
                    
                    
               }//end of using filter options

                    
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