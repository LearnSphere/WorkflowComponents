package edu.cmu.pslc.learnsphere.transform.sensordata;

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

public class TransformSensorDataMain extends AbstractComponent {

    
    /** The join type. */
    /*
    public enum JoinType {
        INNER(1) {
            public String toString() {
                return "inner";
            }

        },
        LEFT_OUTER(2) {
            public String toString() {
                return "left_outer";
            }

        },
        RIGHT_OUTER(3) {
            public String toString() {
                return "right_outer";
            }

        };

        private int value;
        private JoinType(int value) {
            this.value = value;
        }

        /** Provides string IDs for backwards compatibility.
         * @return the string ID for backwards compatibility
         */
    /*
        public Integer getId() {
            return value;
        }
    };
    */
     /** The join type. */
    public enum timePeriod {
        ONE(1) {
            public String toString() {
                return "1";
            }

        },
        TWO(2) {
            public String toString() {
                return "2";
            }

        },
        THREE(3) {
            public String toString() {
                return "3";
            }

        };

        private int timePeriodvalue;
        private timePeriod(int value) {
            this.timePeriodvalue = value;
        }

        /** Provides string IDs for backwards compatibility.
         * @return the string ID for backwards compatibility
         */
        public Integer getId() {
            return timePeriodvalue;
        }
    };

         /** The join type. */
    public enum initialTime {
        ZERO(1) {
            public String toString() {
                return "1";
            }

        },
        ONE(2) {
            public String toString() {
                return "1";
            }

        },
        TWO(3) {
            public String toString() {
                return "2";
            }

        },
        THREE(4) {
            public String toString() {
                return "3";
            }

        };

        private int initialTimevalue;
        private initialTime(int value) {
            this.initialTimevalue = value;
        }

        /** Provides string IDs for backwards compatibility.
         * @return the string ID for backwards compatibility
         */
        public Integer getId() {
            return initialTimevalue;
        }
    };


    /** Debug logging. */
//    public static final int FILE_MERGING_INNER_JOIN = 1,
//                            FILE_MERGING_LEFT_OUTER_JOIN = 2,
//                            FILE_MERGING_RIGHT_OUTER_JOIN = 3,
//                            FILE_MERGING_FULL_OUTER_JOIN = 4;
//    
//    private static final String DEFAULT_DELIMITER = "\t";
//    private static final String OUTPUT_DELIMITER = "\t";
//    
    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        TransformSensorDataMain tool = new TransformSensorDataMain();
        tool.startComponent(args);

    }

    /**
     * This class runs Join on two files.
     */
    public TransformSensorDataMain() {
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
        File generatedFile = this.createFile("Join", ".csv");
        
        
    String delimiterpattern =",";
    Double reSamplingscale1=2.00;
    Double resamplingscale2=2.00;
    
     String timeperiodString=this.getOptionAsString("timeperiod");
     Integer timeperiodInteger=Integer.valueOf(timeperiodString);
     int intTimePeriod=timeperiodInteger;
     
//...... call  output file ...... //
       generatedFile=this.getCombinedOutput(file1, file2, reSamplingscale1, resamplingscale2,delimiterpattern, generatedFile);
        Integer nodeIndex = 0;
        Integer fileIndex = 0;
        
        String fileLabel = "tab-delimited";  /// what should be ???

        this.addOutputFile(generatedFile, nodeIndex, fileIndex, fileLabel);

        System.out.println(this.getOutput());

        // Options
       // String joinTypeString = this.getOptionAsString("join");
      /*
       String initialTimeString= this.getOptionAsString("initialtime");
       String timeperiodString=this.getOptionAsString("timeperiod");
       
       Integer initialTimeInteger=Integer.valueOf(initialTimeString);
       Integer timeperiodInteger=Integer.valueOf(timeperiodString);

       int intTimeIni=initialTimeInteger;
       int intTimePeriod=timeperiodInteger;
       
        String file1ColumnName = this.getOptionAsString("file1ColumnName");

        String file2ColumnName = this.getOptionAsString("file2ColumnName");

        String caseSensitiveString = this.getOptionAsString("caseSensitive");
        String delimiterString = this.getOptionAsString("delimiterPattern");

        Boolean caseSensitive = caseSensitiveString.equalsIgnoreCase("true")? true : false;
        */
     
          
// Call inner join method
//            generatedFile = this.innerJoin(file1, file2, delimiterString, true,
//                "File1-", "File2-", file1ColumnName, file2ColumnName,
//                    generatedFile, caseSensitive);


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
            Logger.getLogger(TransformSensorDataMain.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(TransformSensorDataMain.class.getName()).log(Level.SEVERE, null, ex);
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
     

int resultedDataSize;
if(newlistSensorData1.size()>newlistSensorData2.size())
    resultedDataSize=newlistSensorData1.size();
else
    resultedDataSize=newlistSensorData2.size();

       //.................Output file .............//
       //System.out.println("output");
         BufferedWriter bw = null;           

         FileWriter fstream=null;
        try {
            fstream = new FileWriter(outputFile);       
            bw = new BufferedWriter(fstream);
            outputFile.createNewFile();
            
            List<String> newRow = new ArrayList<String>();
        for(int j=0;j<resultedDataSize-1;j++)
        { 
         bw.append(newlistSensorData1.get(j).toString());
         bw.append("\t");
         bw.append(newlistSensorData2.get(j).toString());
         bw.append("\n");
 
        }

         bw.flush();
        
        bw.close();
        
        } catch (IOException ex) {
           Logger.getLogger(TransformSensorDataMain.class.getName()).log(Level.SEVERE, null, ex);
       } finally {
           try {
               fstream.close();
           } catch (IOException ex) {
               Logger.getLogger(TransformSensorDataMain.class.getName()).log(Level.SEVERE, null, ex);
           }
       }
       
        
return outputFile;

}
    
    
   

    /**
     * Combine the headers from two files. Headers are in String[] format.
     * Left headers are prefixed with the specified string except the first column
     * Right headers are prefixed with the specified string except its first column will be eliminated
     * @param String leftFileHeaderPrefix prefix for the new headers that are from the left file
     * @param String[] leftFileHeaders headers from leftFile
     * @param String rightFileHeaderPrefix prefix for the new headers that are from the right file
     * @param String[] rightFileHeaders headers from right file */
    /*
    private String[] combineHeaders(String leftFileHeaderPrefix, String[] leftFileHeaders,
                String rightFileHeaderPrefix, String[] rightFileHeaders,
                Integer leftColumnIndex, Integer rightColumnIndex) {
            List<String> leftFileHeaders_l = new LinkedList<String>(Arrays.asList(leftFileHeaders));
            List<String> rightFileHeaders_l = new LinkedList<String>(Arrays.asList(rightFileHeaders));
            for (int i = 0; i < leftFileHeaders_l.size(); i++) {
                    if (leftFileHeaderPrefix != null)
                            leftFileHeaders_l.set(i, leftFileHeaderPrefix + leftFileHeaders_l.get(i));
            }
            //remove the first column for it's the matching column
            leftFileHeaders_l.remove(leftColumnIndex);
            for (int i = 0; i < rightFileHeaders_l.size(); i++) {
                    if (rightFileHeaderPrefix != null)
                            rightFileHeaders_l.set(i, rightFileHeaderPrefix + rightFileHeaders_l.get(i));
            }
            rightFileHeaders_l.remove(rightColumnIndex);
            List<String> combined = new LinkedList<String>();
            //leave the first column blank for the matched column
            combined.add("");
            combined.addAll(leftFileHeaders_l);
            combined.addAll(rightFileHeaders_l);
            return combined.toArray(new String[0]);
    }
*/
    /**
     * Make a list of string with empty string as elements
     * @param elementCnt how many elements to be made
     * @return List of empty string
     * */
    /*
    private List<String> makeListWithEmptyStrings (int elementCnt) {
            List<String> list = new LinkedList<String>();
            for (int i = 0; i < elementCnt; i++)
                    list.add("");
            return list;
    }
    */


    /**
     * Given a File, read and return the first line.
     * @param file the File to read
     * @param field delimiter
     * @return String[] first line of the file which is hopefully the column headings
     * @throws ResourceUseException could occur while opening the file
     */
    /*
    public String[] getHeaderFromFile(File file, String delimiter)
                    throws ResourceUseException {
        String[] headers = null;
        try {
                Scanner sc = new Scanner(file);
                headers = sc.nextLine().split(delimiter, -1);
                if (headers.length == 1 && headers[0].length() == 1)
                        headers = new String[0];
                sc.close();
        } catch (FileNotFoundException fEx) {
                this.addErrorMessage(fEx.getMessage());
        }
        return headers;
    }
*/

    /**
     * Turn File into Map structure. The column index is the key. The rest are put in a LinkedList.
     * @param File file whose content to be extracted
     * @param String delimiter field delimiter
     * @param boolean skipEmptyLine skip empty line or not
     * @param boolean includeFirstRow whether to include first row. When first row is header, use false
     * @param colIndex the column index to join on
     * @return Map<String, List<String>> first column becomes the key, turn the rest columns into List<String>
     * @throws ResourceUseException */
    /*
    public Map<String, List<String>> turnFileContentToMap(File file, String delimiter,
            boolean skipEmptyLine, boolean includeFirstRow, int colIndex)
                                                                    throws ResourceUseException {

            Map<String, List<String>> dataInMap = new HashMap<String, List<String>>();
            List<String[]> data = getListOfStringArrayFromFile(file, delimiter, skipEmptyLine);
            boolean firstRow = true;
            for (String[] row : data) {
                    if (!includeFirstRow && firstRow) {
                            firstRow = false;
                            continue;
                    }
                    String key = row[colIndex];
                    List<String> rowList = new LinkedList<String>(Arrays.asList(row));
                    rowList.remove(colIndex);
                    dataInMap.put(key, rowList);
                    //System.warn.println(key +" " + Arrays.asList(rowList).toString());
                    firstRow = false;
            }
            return dataInMap;
    }

*/
    /**
     * Given a File, read and return list of string array. Skip empty line.
     * @param file the File to read
     * @param field delimiter
     * @param boolean skipEmptyLine skip empty line or not
     * @return List<String[]> the content in List of string array
     * @throws ResourceUseException could occur while opening the file
     */
    /*
    public List<String[]> getListOfStringArrayFromFile(File file, String delimiter, boolean skipEmptyLine)
                                            throws ResourceUseException {
            List<String[]> output = new ArrayList<String[]>();
            try {
                    Scanner sc = new Scanner(file);
                    while (sc.hasNextLine()) {
                            String[] cols = sc.nextLine().split(delimiter, -1);
                            if (skipEmptyLine) {
                                    if (cols.length == 0 || (cols.length == 1 && cols[0].length() == 1))
                                            continue;
                            }
                            output.add(cols);
                    }
                    sc.close();
            } catch (FileNotFoundException fEx) {
                    throw ResourceUseException.fileNotFoundException(file);
            }
            return output;
    }
    
    */
    /*
      public Void getListOfIntegerArrayFromFile(File file1, String delimiter1, boolean skipEmptyLine1,File file2,String delimiter2, boolean skipEmptyLine2)
                                            throws ResourceUseException {
                  
            Scanner sc1;
        try {
            sc1 = new Scanner(file1);
             while(sc1.hasNextLine())
                 { 
                     String[] cols = sc1.nextLine().split(delimiter1, -1);
                            if (skipEmptyLine1) {
                                    if (cols.length == 0 || (cols.length == 1 && cols[0].length() == 1))
                                            continue;
                            }
                            
                     Integer ii = Integer.valueOf(cols[0]);
                     listSensorData1.add(ii);
                     Integer ii2 = Integer.valueOf(cols[1]);
                     listTime1.add(ii2);
                     
                    // output.add(sc1.nextInt());
                 }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TransformSensorDataMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        
            Scanner sc2;
        try {
            sc2 = new Scanner(file2);
             while(sc2.hasNextLine())
                 { 
                     String[] cols = sc2.nextLine().split(delimiter2, -1);
                            if (skipEmptyLine2) {
                                    if (cols.length == 0 || (cols.length == 1 && cols[0].length() == 1))
                                            continue;
                            }
                            
                     Integer ii21 = Integer.valueOf(cols[0]);
                     listSensorData2.add(ii21);
                     Integer ii22 = Integer.valueOf(cols[1]);
                     listTime2.add(ii22);
                     
                    // output.add(sc1.nextInt());
                 }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TransformSensorDataMain.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
            return null;
    }
*/
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
