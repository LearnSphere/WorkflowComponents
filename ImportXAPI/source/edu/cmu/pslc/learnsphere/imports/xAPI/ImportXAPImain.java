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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

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

	        for (String err : this.errorMessages) {
	            // These will also be picked up by the workflows platform and relayed to the user.
	            System.err.println(err);
	        }
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
                File confiFile = this.getAttachment(0, 0);
                
                File configFile = this.createFile("Config", ".txt");
                try{
                // read file content from file
                StringBuffer sb= new StringBuffer("");
                FileReader reader = new FileReader(confiFile);
                BufferedReader br = new BufferedReader(reader);
                String str = null;
            
                while((str = br.readLine()) != null) {
                  sb.append(str+"\r\n");
                }
                br.close();
                reader.close();
                
                // write string to file
                FileWriter cf = new FileWriter(configFile.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(cf);
                bw.write(sb.toString()+"\t");
                bw.close();
                cf.close();
                }   
                catch(FileNotFoundException e) {
                  e.printStackTrace();
            }
            catch(IOException e) {
                  e.printStackTrace();
            }
                
                Integer nodeIndex0 = 0;
                Integer fileIndex0 = 0;
                String fileType0 = "text";
                this.addOutputFile(configFile, nodeIndex0, fileIndex0, fileType0);
        
                //Read Configuration File as list
                List<String> list = new ArrayList<String>();
                    try {
                       if (confiFile.isFile() && confiFile.exists()) {
                         InputStreamReader read = new InputStreamReader(new FileInputStream(confiFile));
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
                    
                    //Create the array for replacing taps
                    String array[][]=new String[list.size()-13][];
                    for (int ar=0;ar<list.size()-13;ar++){
                        array[ar]=arrayConf[ar];
                    }
                    
                    //Create the array for analytics
                    String arrayAnal[][]=new String[13][];
                    for (int aa=0;aa<13;aa++){
                        arrayAnal[aa]=arrayConf[list.size()-13+aa];                     
                    }

                //writer.writeAsTxt(flatJson, "sample.txt");
	    	JsonFlattener parser = new JsonFlattener();
                TabTextWriter writer = new TabTextWriter();
	        List<Map<String, String>> flatJson = parser.parseJson(jsonTxt);
                  
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
                    
                    File jsonFlatFile = this.createFile("xAPI_JsonFlattener_file_HeadNames", ".txt");
                    FileWriter fw_0 = new FileWriter(jsonFlatFile.getAbsoluteFile());
                    try (BufferedWriter bw_0 = new BufferedWriter(fw_0)) {
                        for (String names:tabNames){
                            bw_0.write(names+"\t");
                        }                     
                        
//                        for(int line=0;line<mainContent[0].length;line++){
//                            bw_0.newLine();
//                                for (int col=0;col<mainContent.length;col++) {
//                                    bw_0.write(mainContent[col][line].replace("\n","") + "\t");
//                                }
//                        }
                    }
                      
            Integer nodeIndex1 = 1;
            Integer fileIndex1 = 0;
            String fileType1 = "tab-delimited";
            this.addOutputFile(jsonFlatFile, nodeIndex1, fileIndex1, fileType1);            
                                
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

               //transfer "actor mabox" into hashcode (add the prefix "Stu_" and make same length by append "#")
               ArrayList<Integer> strInt = new ArrayList<Integer>();
               int str;
               for (int k=0;k<tabNames.length;k++){
                   if(tabNames[k].equals("actor mbox")){
                        for(int rs=0;rs<mainContent[k].length;rs++){
//                            mainContent[k][rs]=mainContent[k][rs].replace("mailto:","");
                            str=Math.abs(mainContent[k][rs].hashCode());
                            strInt.add(String.valueOf(str).length());
                        }
                        Integer max=Collections.max(strInt);
                        for (int rs=0;rs<mainContent[k].length;rs++){
                            if (strInt.get(rs)<max){
                                for(int a=0;a<max-strInt.get(rs);a++){
                                    mainContent[k][rs]="Stu_"+String.valueOf(Math.abs(mainContent[k][rs].hashCode()))+"#";
                                }
                            }else{
                                mainContent[k][rs]="Stu_"+String.valueOf(Math.abs(mainContent[k][rs].hashCode()));
                            }
                        }
                   }
               }               
                                            
               //Transfer Time Format
               for(int kt=0;kt<tabNames.length;kt++){
                   if(tabNames[kt].equals("stored")){
                       for(int rs=0;rs<mainContent[kt].length;rs++){
                           SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                           Date date = dt.parse(mainContent[kt][rs]);
                           SimpleDateFormat dt1 = new SimpleDateFormat("MM/dd/yyyy HH:mm");
                           mainContent[kt][rs]=dt1.format(date);
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
               }else{
                   ns=selectContent[0].length; //numStatements
               }
               //Add new tag: Row
               ArrayList<String> row=new ArrayList<String>();
               ArrayList<String> addTags=new ArrayList<String>();
               String [][] selectContent1=new String[array.length+1][];
               String [][] arrayAdd=new String[array.length+1][];
               if (!"null".equals(arrayAnal[3][1])){
                   int count1=0;
                        for (String selectContent2 : selectContent[0]) {
                            count1++;
                            row.add(String.valueOf(count1));
                        }
                        Object[] rowArray=row.toArray();
                        String rowAdd1[]=new String[rowArray.length];
                        System.arraycopy(rowArray, 0, rowAdd1,0,rowArray.length);
                        selectContent1[0]=rowAdd1;
                        for (int sc1=1;sc1<array.length+1;sc1++){
                            selectContent1[sc1]=selectContent[sc1-1];
                        }
                        selectContent=selectContent1;
                        
                        addTags.add(arrayAnal[3][1]); 
                        Object[] rowAdd=addTags.toArray();
                        String rowAdd2[]=new String[rowAdd.length];
                        System.arraycopy(rowAdd, 0, rowAdd2, 0, rowAdd.length);
                        arrayAdd[0]=rowAdd2;
                        for(int sc2=1;sc2<array.length+1;sc2++){
                            arrayAdd[sc2]=array[sc2-1];
                        }
                        array=arrayAdd;
               }else{
                   selectContent=selectContent;
               }

               //Add new tag: "Outcome"
               ArrayList<String> column=new ArrayList<String>();
               ArrayList<String> columnTags=new ArrayList<String>();
               String [][] selectContent2=new String[array.length+1][];
               String [][] arrayAdd2=new String[array.length+1][];

               if(!"null".equals(arrayAnal[4][1])){
                   for(int sc=0;sc<array.length;sc++){
                       if (array[sc][0].contains(arrayAnal[5][1])){
                           double threshold=Double.parseDouble(arrayAnal[6][1]);
                           for(int rs=0;rs<selectContent[0].length;rs++){
                               if(Double.parseDouble(selectContent[sc][rs])>=threshold){
                                   column.add("CORRECT");
                               }else{
                                   column.add("INCORRECT");   
                               }
                           }
                            
                           Object[] columnArray=column.toArray();
                           String columnAdd1[]=new String[columnArray.length];
                           System.arraycopy(columnArray, 0, columnAdd1,0,columnArray.length);
                           selectContent2[array.length]=columnAdd1;
                           for (int sc1=0;sc1<array.length;sc1++){
                               selectContent2[sc1]=selectContent[sc1];
                           }
                           selectContent=selectContent2;
                           
                           columnTags.add(arrayAnal[4][1]);
                           Object[] columnTagsArray=columnTags.toArray();
                           String columnTags1[]=new String[columnTagsArray.length];
                           System.arraycopy(columnTagsArray, 0, columnTags1, 0, columnTagsArray.length);
                           
                           for (int columnNum=0;columnNum<array.length;columnNum++){
                               arrayAdd2[columnNum]=array[columnNum];
                           }
                           arrayAdd2[array.length]=columnTags1;
                           array=arrayAdd2;

                       }
                   }
               }
               
               //Add new tag with two combined parts: "Problem Hierarchy"
               ArrayList<String> column3=new ArrayList<String>();
               ArrayList<String> tagComb_1=new ArrayList<String>();
               ArrayList<String> tagComb_2=new ArrayList<String>();
               ArrayList<String> columnTags3=new ArrayList<String>();
               String [][] selectContent3=new String[array.length+1][];
               String [][] arrayAdd3=new String[array.length+1][];
               
               if(!"null".equals(arrayAnal[7][1])){
                   for(int kt=0;kt<tabNames.length;kt++){
                           if(tabNames[kt].equals(arrayAnal[8][1])){
                               for (int rsa=0;rsa<mainContent[kt].length;rsa++){
                                   tagComb_1.add(mainContent[kt][rsa].toString());
                                }
                            }
                    }
                   
                   for(int kt=0;kt<tabNames.length;kt++){
                           if(tabNames[kt].equals(arrayAnal[9][1])){
                               for (int rsa=0;rsa<mainContent[kt].length;rsa++){
                                   tagComb_2.add(mainContent[kt][rsa].toString());
                                }
                            }
                    }
                   
                   for(int rs=0;rs<selectContent[0].length;rs++){
                       column3.add(tagComb_1.get(rs).toString()+", "+tagComb_2.get(rs).toString());
                   }
                   
                    Object[] columnArray=column3.toArray();
                    String columnAdd1[]=new String[columnArray.length];
                    System.arraycopy(columnArray, 0, columnAdd1,0,columnArray.length);
                    selectContent3[array.length]=columnAdd1;
                    for (int sc1=0;sc1<array.length;sc1++){
                        selectContent3[sc1]=selectContent[sc1];
                    }
                    selectContent=selectContent3;
                   
                   columnTags3.add(arrayAnal[7][1]);
                   Object[] columnTagsArray=columnTags3.toArray();
                   String columnTags31[]=new String[columnTagsArray.length];
                   System.arraycopy(columnTagsArray, 0, columnTags31, 0, columnTagsArray.length);
                   
                   for (int columnNum=0;columnNum<array.length;columnNum++){
                       arrayAdd3[columnNum]=array[columnNum];
                   }
                   arrayAdd3[array.length]=columnTags31;
                   array=arrayAdd3;
                    
               }
               
               //Add new tags:"Opportunity (Default)"
               ArrayList<String> column4=new ArrayList<String>();
               ArrayList<String> tagBase_1=new ArrayList<String>();
               ArrayList<String> tagBase_2=new ArrayList<String>();
               ArrayList<String> columnTags4=new ArrayList<String>();
               String [][] selectContent4=new String[array.length+1][];
               String [][] arrayAdd4=new String[array.length+1][];               
               
               if(!"null".equals(arrayAnal[10][1])){
                   for(int kt=0;kt<tabNames.length;kt++){
                           if(tabNames[kt].equals(arrayAnal[11][1])){
                               for (int rsa=0;rsa<mainContent[kt].length;rsa++){
                                   tagBase_1.add(mainContent[kt][rsa].toString());
                                }
                            }
                    }

                   for(int kt=0;kt<tabNames.length;kt++){
                           if(tabNames[kt].equals(arrayAnal[12][1])){
                               for (int rsa=0;rsa<mainContent[kt].length;rsa++){
                                   tagBase_2.add(mainContent[kt][rsa].toString());
                                }
                            }
                    }                   
                   
                   for(int mc=0;mc<mainContent[0].length;mc++){
                       int count4=1;
                       for (int mcb=0;mcb<mc;mcb++){
                           if(tagBase_1.get(mc).equals(tagBase_1.get(mcb))){
                               if(tagBase_2.get(mc).equals(tagBase_2.get(mcb))){
                                    count4++;
                               }
                           }else{
                               count4=count4;
                           } 
                       }
                       column4.add(String.valueOf(count4));
                   }
                
                   Object[] rowArray=column4.toArray();
                   String rowAdd1[]=new String[rowArray.length];
                   System.arraycopy(rowArray, 0, rowAdd1,0,rowArray.length);
                   for (int sc1=0;sc1<array.length;sc1++){
                        selectContent4[sc1]=selectContent[sc1];
                   }
                   selectContent4[array.length]=rowAdd1;
                   selectContent=selectContent4;                  

                   columnTags4.add(arrayAnal[10][1]);
                   Object[] columnTagsArray=columnTags4.toArray();
                   String columnTags41[]=new String[columnTagsArray.length];
                   System.arraycopy(columnTagsArray, 0, columnTags41, 0, columnTagsArray.length);
                   
                   for (int columnNum=0;columnNum<array.length;columnNum++){
                       arrayAdd4[columnNum]=array[columnNum];
                   }
                   arrayAdd4[array.length]=columnTags41;
                   array=arrayAdd4;
               }

               //Transfer Duration Format
               for(int sc=0;sc<array.length;sc++){
                   if (array[sc][0].contains("Duration (sec)")){
                       for (int rs=0;rs<mainContent[sc].length;rs++){
                           double duration=Double.parseDouble(selectContent[sc][rs])/1000;
                           BigDecimal b =new BigDecimal(duration);
                           double f1 = b.setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
                           selectContent[sc][rs]=Double.toString(f1);
                       }
                   }   
               }
             
          File tabDeliFile = this.createFile("xAPI_TabDelimited_file", ".txt");
                    FileWriter fw = new FileWriter(tabDeliFile.getAbsoluteFile());
                    try (BufferedWriter bw = new BufferedWriter(fw)) {
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
            this.addOutputFile(tabDeliFile, nodeIndex2, fileIndex2, fileType2);
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