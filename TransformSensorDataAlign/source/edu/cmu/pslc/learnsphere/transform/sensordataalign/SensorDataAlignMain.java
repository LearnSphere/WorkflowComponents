package edu.cmu.pslc.learnsphere.transform.join;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


import org.apache.commons.lang.StringUtils;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SensorDataAlignMain extends AbstractComponent {

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        SensorDataAlignMain tool = new SensorDataAlignMain();
        tool.startComponent(args);

    }

    /**
     * This class runs Join on two files.
     */
    public SensorDataAlignMain() {
        super();


    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        this.addMetaDataFromInput("tab-delimited", 0, 0, ".*");
        this.addMetaDataFromInput("tab-delimited", 1, 0, ".*");

    }

    @Override
    public Boolean test() {
        Boolean passing = true;
        // The first index is the input node index of this component.
        // The second index is the file index for that node.
        return passing;
    }

    /**
     * Joins the two files and adds the resulting file to the component output.
     */
    List<Integer> listTime1=new ArrayList<>();
    List<Integer> listSensorData1=new ArrayList<>();
    List<Integer> listTime2=new ArrayList<>();
    List<Integer> listSensorData2=new ArrayList<>();
    
    @Override
    protected void runComponent() {

           // Input files
        // The first index is the input node index of this component.
        // The second index is the file index for that node.

        File file1 = this.getAttachment(0, 0);
        File file2 = this.getAttachment(1, 0);


        if (file1 == null || file2 == null) {
            System.err.println("The Transform -> Join component requires two input files.");
            return;
        }

        // Output file
        File generatedFile = this.createFile("Join", ".txt");
        String delimiterpattern = this.getOptionAsString("delimiterPattern");
        String strInitialtime1=this.getOptionAsString("initialtime1");
        String strInitialtime2=this.getOptionAsString("initialtime2");
        
        String strReSamplingscale1=this.getOptionAsString("resamplingscale1");
        String strReSamplingscale2=this.getOptionAsString("resamplingscale2");
        
   // String delimiterpattern ="\t";
    Double reSamplingscale1=Double.parseDouble(strReSamplingscale1);
    Double resamplingscale2=Double.parseDouble(strReSamplingscale2);
   /* 
     String timeperiodString=this.getOptionAsString("timeperiod");
     Integer timeperiodInteger=Integer.valueOf(timeperiodString);
     int intTimePeriod=timeperiodInteger;
    */ 
//...... call  output file ...... //
       generatedFile=this.getCombinedOutput(file1, file2, reSamplingscale1, resamplingscale2,delimiterpattern, generatedFile);
        Integer nodeIndex = 0;
        Integer fileIndex = 0;
        
        String fileLabel = "tab-delimited";  /// what should be ???

        this.addOutputFile(generatedFile, nodeIndex, fileIndex, fileLabel);

        System.out.println(this.getOutput());
    }

      
File getCombinedOutput(File file1,File file2,Double sampleScale1,Double sampleScale2,String delimiterpattern,File outputfile)
{
  
Double samplingScale1=sampleScale1;
Double samplingScale2=sampleScale2;
File outputFile = outputfile;
       
        
        String delimiter1=delimiterpattern;
        String delimiter2=delimiterpattern;
        boolean skipEmptyLine1=true;
        boolean skipEmptyLine2=true;
      
         List<Integer> listTime1=new ArrayList<>();
         List<Double> listSensorData1=new ArrayList<>();
         List<Integer> listTime2=new ArrayList<>();
         List<Double> listSensorData2=new ArrayList<>();
    
         List<Double> newlistSensorData1=new ArrayList<>();
         List<Double> newlistSensorData2=new ArrayList<>();
        //// get data/////////////////////
        Scanner sc1;
        int count1=0;
        try {
            sc1 = new Scanner(file1);
             while(sc1.hasNextLine())
                 { 
                     String[] cols = sc1.nextLine().split(delimiter1, -1);
                            if (skipEmptyLine1) {
                                    if (cols.length == 0 || (cols.length == 1 && cols[0].length() == 1))
                                            continue;
                            }
                                                    
                           if(count1!=0)
                           { String tt =  cols[0];
                             Double ii = Double.parseDouble(tt); 
                             listSensorData1.add(ii);
                            // System.out.println("ii= "+ii);
                             Integer ii2 = Integer.valueOf(cols[1]);
                             listTime1.add(ii2);
                           }
                          count1++;
                    
                     
                    // output.add(sc1.nextInt());
                 }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SensorDataAlignMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        
            Scanner sc2;
            int count2=0;
        try {
            sc2 = new Scanner(file2);
             while(sc2.hasNextLine())
                 { 
                     String[] cols = sc2.nextLine().split(delimiter2, -1);
                            if (skipEmptyLine2) {
                                    if (cols.length == 0 || (cols.length == 1 && cols[0].length() == 1))
                                            continue;
                            }
                     if(count2!=0)
                     {
                     Double ii21 = Double.parseDouble(cols[0]);
                     listSensorData2.add(ii21);
                     Integer ii22 = Integer.valueOf(cols[1]);
                     listTime2.add(ii22);  
                     }
                     count2++;
                 }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SensorDataAlignMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //////// end get data from file////////
        
        //.................... modified data with given sampling scale.................................//
      int sizefile1Data=listSensorData1.size();
          
      if(samplingScale1>=1)
      {
      int k=0;
      for(k=0;k<sizefile1Data-1;k++)
      {
          newlistSensorData1.add(listSensorData1.get(k));
          
                  Double diff=listSensorData1.get(k+1)-listSensorData1.get(k);
                  Double diffAdd=diff/samplingScale1;
                  for(int j=1;j<samplingScale1;j++)
                  {
                    //Double newData=newlistSensorData1.get(newlistSensorData1.size()-1)+diffAdd;
                      Double newData=listSensorData1.get(k)+diffAdd*j;
                    newlistSensorData1.add(newData);
                   }
                
      }   
        newlistSensorData1.add(listSensorData1.get(k));
        
      }
     else if(samplingScale1<1)
       {
    
         }
     
     ///// for the modified data for 2nd file to resampling with given samplingScale ......//
     
     int sizefile1Data2=listSensorData2.size();
          
      if(samplingScale2>=1)
      {
      int k=0;
      for(k=0;k<sizefile1Data2-1;k++)
      {
          newlistSensorData2.add(listSensorData2.get(k));
          
                  Double diff=listSensorData2.get(k+1)-listSensorData2.get(k);
                  Double diffAdd=diff/samplingScale2;
                  for(int j=1;j<samplingScale2;j++)
                  {
                    //Double newData=newlistSensorData1.get(newlistSensorData1.size()-1)+diffAdd;
                      Double newData=listSensorData2.get(k)+diffAdd*j;
                    newlistSensorData2.add(newData);
                   }
                
      }   
        newlistSensorData2.add(listSensorData2.get(k));
        
      }
     else if(samplingScale2<1)
       {
     
         }
     

int resultedBigDataSize,resultedSmallDataSize;
boolean firstDataSetIsBig=true;
if(newlistSensorData1.size()>newlistSensorData2.size())
    {resultedBigDataSize=newlistSensorData1.size();
     resultedSmallDataSize=newlistSensorData2.size();
     firstDataSetIsBig=true;
    }
else
    {resultedBigDataSize=newlistSensorData2.size();
     resultedSmallDataSize= newlistSensorData1.size();
     firstDataSetIsBig=false;
    }

       //.................Output file .............//
       //System.out.println("output");
         BufferedWriter bw = null;           

         FileWriter fstream=null;
        try {
            fstream = new FileWriter(outputFile);       
            bw = new BufferedWriter(fstream);
            outputFile.createNewFile();
            
            List<String> newRow = new ArrayList<String>();
        for(int j=0;j<resultedSmallDataSize-1;j++)
        { 
         bw.append(newlistSensorData1.get(j).toString());
         bw.append("\t");
         bw.append(newlistSensorData2.get(j).toString());
         bw.append("\n");
 
        }

        if(firstDataSetIsBig)
        {
          for(int j=resultedSmallDataSize;j<resultedBigDataSize-1;j++)
            { 
             bw.append(newlistSensorData1.get(j).toString());
             bw.append("\t");
             bw.append("");
             bw.append("\n");
 
            }
         }
        else
        {
          for(int j=resultedSmallDataSize;j<resultedBigDataSize-1;j++)
            { 
             bw.append("");
             bw.append("\t");
             bw.append(newlistSensorData2.get(j).toString());
             bw.append("\n");
 
            } 
        }
            
         bw.flush();
        
        bw.close();
        
        } catch (IOException ex) {
           Logger.getLogger(SensorDataAlignMain.class.getName()).log(Level.SEVERE, null, ex);
       } finally {
           try {
               fstream.close();
           } catch (IOException ex) {
               Logger.getLogger(SensorDataAlignMain.class.getName()).log(Level.SEVERE, null, ex);
           }
       }
       
        
return outputFile;

}
    
    
    
    



    private static class ResourceUseException extends Exception {

        public static ResourceUseException fileNotFoundException(File file) {
            // TODO Auto-generated method stub
            return null;
        }

        public static ResourceUseException wrongFileFormatException(File leftFile) {
            // TODO Auto-generated method stub
            return null;
        }

    }

}
