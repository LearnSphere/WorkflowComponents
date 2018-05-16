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

import gov.adlnet.xapi.client.StatementClient;
import gov.adlnet.xapi.model.Actor;
import gov.adlnet.xapi.model.Agent;
import gov.adlnet.xapi.model.StatementResult;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

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

//	        System.out.println(this.getOutput());
//
//	        for (String err : this.errorMessages) {
//	            // These will also be picked up by the workflows platform and relayed to the user.
//	            System.err.println(err);
//	        }
	    }
 
	    public void getXAPIdata(String url,String username,String password,String filter,String customfilter,String filterValue) throws Exception {
                
	    	StatementClient client = new StatementClient(url, username, password);
	    	String jsonTxt = null;
                String jsonTxtSpr;
                StatementResult results;
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
                        sb.append(gson.toJson(obj));
                     }
                     jsonTxtSpr= sb.toString();
                     jsonTxt = jsonTxtSpr.replace("][",","); //case two stages of statements
	        } catch (Exception e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
                
                       //import Configuration File
                
                File theFile = this.getAttachment(0, 0);
                
                Integer nodeIndex0 = 0;
                Integer fileIndex0 = 0;
                String fileType = "text";
                this.addOutputFile(theFile, nodeIndex0, fileIndex0, fileType);
                logger.info(this.getOutput());
                
                //Read Configuration File as list
                List<String> list = new ArrayList<String>();
                    try {
                       if (theFile.isFile() && theFile.exists()) {
                         InputStreamReader read = new InputStreamReader(new FileInputStream(theFile));
                         BufferedReader bufferedReader = new BufferedReader(read);
                         String lineTxt = null;
                         
                         while ((lineTxt = bufferedReader.readLine()) != null) {
                             if(lineTxt.length()>0){
                                 if (!lineTxt.startsWith("#"))
                                 list.add(lineTxt);
                             }
                         }
                         read.close();
                       } else{
//                       System.out.println("Configuration file missing");
                       logger.info("Configuration file missing");
                       
                       }
                    }catch(IOException e){
//                        System.out.println("Error Happened");
                        logger.info("Error Happened in inputting Configuration File");
                    } 
                    
                    //Read Configuration File (list to array)
                    String arrayConf[][] = new String[list.size()][];
                    String[] myArray = null;
                    for (int i=0;i<list.size();i++){
                        arrayConf[i]=new String[2];
                            String linetxt=list.get(i);
                            myArray=linetxt.split("=");
                            System.arraycopy(myArray, 0, arrayConf[i], 0, myArray.length);
                    }                    
                    
                    String array[][]=new String[list.size()-3][];
                    for (int ar=0;ar<list.size()-3;ar++){
                        array[ar]=arrayConf[ar];
                    }
                    
                    String arrayAnal[][]=new String[3][];
                    arrayAnal[0]=arrayConf[list.size()-3];
                    arrayAnal[1]=arrayConf[list.size()-2];
                    arrayAnal[2]=arrayConf[list.size()-1];
                                                                              
                //writer.writeAsTxt(flatJson, "sample.txt");
	    	JsonFlattener parser = new JsonFlattener();
                TabTextWriter writer = new TabTextWriter();
	        List<Map<String, String>> flatJson = parser.parseJson(jsonTxt);
                 
//                File generatedFile_0 = this.createFile("xAPI-JsonFlattener-file", ".txt");
//                FileWriter oStream_0 = new FileWriter(generatedFile_0);
//	        BufferedWriter sw_0 = new BufferedWriter(oStream_0);
//	        sw_0.write(writer.writeAsTxt(flatJson));
 
               int rows=array.length;
               int columns=array[0].length;
               List<String> items = new ArrayList<>();
               
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
                     
                    File generatedFile_0 = this.createFile("xAPI-JsonFlattener-file", ".txt");
                    FileWriter fw_0 = new FileWriter(generatedFile_0.getAbsoluteFile());
                    try (BufferedWriter bw_0 = new BufferedWriter(fw_0)) {
                        for (String names:tabNames){
                            bw_0.write(names+"\t");
                        }                     
                        
                        for(int line=0;line<mainContent[0].length;line++){
                            bw_0.newLine();
                                for (int col=0;col<mainContent.length;col++) {
                                    bw_0.write(mainContent[col][line].replace("\n","") + "\t");
                                }
                        }
                    }
            Integer nodeIndex1 = 1;
            Integer fileIndex1 = 0;
            String fileType1 = "tab-delimited";
            this.addOutputFile(generatedFile_0, nodeIndex1, fileIndex1, fileType1);
//            System.out.println(this.getOutput());
            logger.info(this.getOutput());
                     
               //remove the "-" symbol of id
               for(int k=0;k<tabNames.length;k++){
                   if(tabNames[k].equals("id")){
                       for(int rs=0;rs<mainContent[k].length;rs++){
                           mainContent[k][rs]=mainContent[k][rs].replace("-","");
                       }
                   }
               }
               
               for (int k=0;k<tabNames.length;k++){
                   if(tabNames[k].equals("object id")){
                        for(int rs=0;rs<mainContent[k].length;rs++){
                            mainContent[k][rs]=mainContent[k][rs].replace("-","");
                        }
                   }
               }

               for (int k=0;k<tabNames.length;k++){
                   if(tabNames[k].equals("actor mbox")){
                        for(int rs=0;rs<mainContent[k].length;rs++){
                            mainContent[k][rs]=mainContent[k][rs].replace("mailto:","");
                        }
                   }
               }               
               
               //Transfer Time Format
//               for(int k=0;k<tabNames.length;k++){
//                   if(tabNames[k].equals("stored")){
//                       for(int rs=0;rs<mainContent[k].length;rs++){
//                           SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//                           Date date = dt.parse(mainContent[k][rs]);
//                           SimpleDateFormat dt1 = new SimpleDateFormat("yyyy/MM/dd HH:mm");
//                           mainContent[k][rs]=dt1.format(date);
//                       }
//                   }
//               }               
                                        
               //Create array matrix about selected columns
                String [][] selectContent=new String[array.length][];
                for(int sc=0;sc<array.length;sc++){
                    for (int k=0;k<tabNames.length;k++){
                        if(array[sc][1].equals(tabNames[k])){
                            selectContent[sc]=mainContent[k];    
                        }
                    }
                }
               
                
               String sizeFilter = null;
               String sizeFilter1 = null;
               String sizeFilter2 = null;
               List filterList = new ArrayList();
               String [][] selectContentFil=new String[array.length][];
               if(  !"null".equals(arrayAnal[1][1])){
                   sizeFilter=arrayAnal[2][1]; 
                        sizeFilter1 = sizeFilter.replace("<"," "); 
                        sizeFilter2 = sizeFilter1.replace(">"," ");
                        List<String> sizeFilterList = new ArrayList<>(Arrays.asList(sizeFilter2.split(",")));
                        double minValue=Double.parseDouble(sizeFilterList.get(0));               
                        double maxValue=Double.parseDouble(sizeFilterList.get(1));
                        
                        for(int sc=0;sc<array.length;sc++){
                            if(array[sc][0].contains(arrayAnal[1][1])){
                                for(int rs=0;rs<selectContent[0].length;rs++){
                                    if(Double.parseDouble(selectContent[sc][rs])>minValue&Double.parseDouble(selectContent[sc][rs])<maxValue){
                                        filterList.add(rs);
                                    }
                                }   
                            }
                        }

                        List<String> filterValueList = new ArrayList<String>();
                        for (int sc=0;sc<array.length;sc++){
                            filterValueList.clear();
                            for(int nf=0;nf<filterList.size();nf++){
                                filterValueList.add(selectContent[sc][(Integer) filterList.get(nf)]);  
                            }
                            Object[] filterValueArr= filterValueList.toArray();
                            String filterValueArrStr[]=new String[filterValueArr.length];
                            System.arraycopy(filterValueArr, 0, filterValueArrStr,0, filterValueArr.length);
                            selectContentFil[sc]=filterValueArrStr;
                        }
                   selectContent=selectContentFil;
               }else{
                   selectContent=selectContent;
               }
               
               int ns;
               if(  !"null".equals(arrayAnal[0][1])){
                   ns=Integer.parseInt(arrayAnal[0][1]);  //numStatements
                   if(Integer.parseInt(arrayAnal[0][1])>selectContent[0].length){
                       ns=selectContent[0].length;
                   }
               }
               
               else{
                   ns=selectContent[0].length; //numStatements
               }

               File generatedFile = this.createFile("xAPI-TabDelimited-file", ".txt");
                    FileWriter fw = new FileWriter(generatedFile.getAbsoluteFile());
                    try (BufferedWriter bw = new BufferedWriter(fw)) {
//                    bw.write(key, 0, key.length());
                        for (String[] array1 : array) {
                            bw.write(array1[0]+"\t");
                        }
                        
                        for(int rs=0;rs<ns;rs++){
                            bw.newLine();
                            for (String[] array2:selectContent){
                                bw.write(array2[rs]+"\t");
                            }                          
                        }
                    }
                
	    Integer nodeIndex2 = 2;
            Integer fileIndex2 = 0;
            String fileType2 = "tab-delimited";
            this.addOutputFile(generatedFile, nodeIndex2, fileIndex2, fileType2);
            
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