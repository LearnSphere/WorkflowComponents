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
    protected void processOptions() {
        logger.info("Processing Options");
        // The addMetaData* methods make the meta data available to downstream components.
    }

    @Override
    protected void parseOptions() {
        logger.info("The value of the url option is " + this.getOptionAsString("url"));
        logger.info("The value of the username option is " + this.getOptionAsString("username"));        
    }

    @Override
    protected void runComponent() {
        this.addComponentProgressMessage("Querying data based on xAPI now");
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
                   
               }else{
                    String FilterOperator=this.getOptionAsString("FilterOperator");
                    String ProcessOperator=this.getOptionAsString("ProcessOperator");
                    
                    ArrayList filterList = new ArrayList();
                    ArrayList filterValuesList = new ArrayList();
                    String Filter01=this.getOptionAsString("Filter01");
                    filterList.add(Filter01);
                    String FilterValue01=this.getOptionAsString("FilterValue01");
                    filterValuesList.add(FilterValue01);
                    String Filter02=this.getOptionAsString("Filter02");
                    filterList.add(Filter02);
                    String FilterValue02=this.getOptionAsString("FilterValue02");
                    filterValuesList.add(FilterValue02);
                    String Filter03=this.getOptionAsString("Filter03");
                    filterList.add(Filter03);
                    String FilterValue03=this.getOptionAsString("FilterValue03");
                    filterValuesList.add(FilterValue03);
                    String Filter04=this.getOptionAsString("Filter04");
                    filterList.add(Filter04);
                    String FilterValue04=this.getOptionAsString("FilterValue04");
                    filterValuesList.add(FilterValue04);
                    String Filter05=this.getOptionAsString("Filter05");
                    filterList.add(Filter05);
                    String FilterValue05=this.getOptionAsString("FilterValue05");
                    filterValuesList.add(FilterValue05);
                    String Filter06=this.getOptionAsString("Filter06");
                    filterList.add(Filter06);
                    String FilterValue06=this.getOptionAsString("FilterValue06");
                    filterValuesList.add(FilterValue06);
                    String Filter07=this.getOptionAsString("Filter07");
                    filterList.add(Filter07);
                    String FilterValue07=this.getOptionAsString("FilterValue07");
                    filterValuesList.add(FilterValue07);
                    String Filter08=this.getOptionAsString("Filter08");
                    filterList.add(Filter08);
                    String FilterValue08=this.getOptionAsString("FilterValue08");
                    filterValuesList.add(FilterValue08);
                    String Filter09=this.getOptionAsString("Filter09");
                    filterList.add(Filter09);
                    String FilterValue09=this.getOptionAsString("FilterValue09");
                    filterValuesList.add(FilterValue09);
                    String Filter10=this.getOptionAsString("Filter10");
                    filterList.add(Filter10);
                    String FilterValue10=this.getOptionAsString("FilterValue10");
                    filterValuesList.add(FilterValue10);

                    ArrayList processList = new ArrayList();
                    ArrayList processValueList = new ArrayList();
                    String Process01=this.getOptionAsString("Process01");
                    processList.add(Process01);
                    String ProcessValue01=this.getOptionAsString("ProcessValue01");
                    processValueList.add(ProcessValue01);
                    String Process02=this.getOptionAsString("Process02");
                    processList.add(Process02);
                    String ProcessValue02=this.getOptionAsString("ProcessValue02");
                    processValueList.add(ProcessValue02);
                    String Process03=this.getOptionAsString("Process03");
                    processList.add(Process03);
                    String ProcessValue03=this.getOptionAsString("ProcessValue03");
                    processValueList.add(ProcessValue03);
                    String Process04=this.getOptionAsString("Process04");
                    processList.add(Process04);
                    String ProcessValue04=this.getOptionAsString("ProcessValue04");
                    processValueList.add(ProcessValue04);
                    String Process05=this.getOptionAsString("Process05");
                    processList.add(Process05);
                    String ProcessValue05=this.getOptionAsString("ProcessValue05");
                    processValueList.add(ProcessValue05);
                    String Process06=this.getOptionAsString("Process06");
                    processList.add(Process06);
                    String ProcessValue06=this.getOptionAsString("ProcessValue06");
                    processValueList.add(ProcessValue06);
                    String Process07=this.getOptionAsString("Process07");
                    processList.add(Process07);
                    String ProcessValue07=this.getOptionAsString("ProcessValue07");
                    processValueList.add(ProcessValue07);
                    String Process08=this.getOptionAsString("Process08");
                    processList.add(Process08);
                    String ProcessValue08=this.getOptionAsString("ProcessValue08");
                    processValueList.add(ProcessValue08);
                    String Process09=this.getOptionAsString("Process09");
                    processList.add(Process09);
                    String ProcessValue09=this.getOptionAsString("ProcessValue09");
                    processValueList.add(ProcessValue09);
                    String Process10=this.getOptionAsString("Process10");
                    processList.add(Process10);
                    String ProcessValue10=this.getOptionAsString("ProcessValue10");
                    processValueList.add(ProcessValue10);
                    
                    //store the filter values in the HashMap by key:value
                    HashMap<String,String> filterMap = new HashMap<String,String>();
                    for (int i=0; i<filterList.size(); i++) {
                        if(!filterList.get(i).equals("null")){
                            filterMap.put(filterList.get(i).toString(),filterValuesList.get(i).toString());
                        }
                    }
                    
                    //store the process values in HashMap by key:value
                    HashMap<String,String> processMap = new HashMap<String,String>();
                    for (int i=0; i<processList.size(); i++) {
                        if(!processList.get(i).equals("null")){
                            String[] processSplit=processList.get(i).toString().split(":");
                            processMap.put(processList.get(i).toString(), processValueList.get(i).toString());
                        }
                    }
                    
                    JSONObject sqlUrlVQL=new JSONObject();
                    
                    try {
                        sqlUrlVQL=new filterValuesCombVQL().sqlUrlVQL(FilterOperator, filterMap, ProcessOperator, processMap);
                        String filterValue="";
                        String processValue="";
                    } catch (JSONException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    StatementClientVeracity qrlByOptionStsVQL =new StatementClientVeracity();
                    StatementClientVeracity qrlByOptionStsVQL1 =new StatementClientVeracity();
                    try {
                        sqlStatements=qrlByOptionStsVQL.filterByOptionVQL(sqlUrlVQL, queryMode, lrsUrl, lrsUsername, lrsPassword);
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (JSONException ex) {
                        Logger.getLogger(ImportXAPImain.class.getName()).log(Level.SEVERE, null, ex);
                    }
     
                   //System.out.println(sqlStatements.toString());
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