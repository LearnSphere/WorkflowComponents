package edu.cmu.pslc.learnsphere.analysis.outputComparator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import edu.cmu.pslc.datashop.extractors.workflows.ComponentOption;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.workflows.InputHeaderOption;

public class OutputComparatorMain extends AbstractComponent {

    public static void main(String[] args) {

        OutputComparatorMain tool = new OutputComparatorMain();
        tool.startComponent(args);
    }

    public OutputComparatorMain() {
        super();
    }

    @Override
    protected void runComponent() {
        Boolean reqsMet = true;
        int nodeIndex = 0;
        String fileType  = this.getOptionAsString("fileType");
        List<File> inputFiles = this.getAttachments(nodeIndex);
        
        if (fileType.equalsIgnoreCase("XML")) {
                if (inputFiles != null) {
                        int cnt = 0;
                        for (File file : inputFiles) {
                                File fileConverted = convertXML(file.getAbsolutePath(), cnt);
                                this.setOption("xmlfile" + cnt, fileConverted.getAbsolutePath());
                                cnt++;
                        }
                } else {
                        String exErr = "No input files uploaded.";
                        addErrorMessage(exErr);
                        reqsMet = false;
                }
                //set compareColumn and matchColumn to ""; sometimes when compareColumn="<model_output>", "<" will cause problem
                this.setOption("compareColumn", "");
        } else if (fileType.equalsIgnoreCase("Properties File")) {
                this.setOption("compareColumn", "");
        } else if (fileType.equalsIgnoreCase("Tabular")) {
                //process match columns
                if (inputFiles != null) {
                        int numColumnsToMatch = this.getOptionAsInteger("numColumnsToMatch");
                        int numOfFiles = inputFiles.size();
                        for (int colOrder = 1; colOrder <= numColumnsToMatch; colOrder++) {
                                String inputHeaderOptionName = "matchColumns" + colOrder;
                                List<InputHeaderOption> matchColumnsList = this.getInputHeaderOption(inputHeaderOptionName, nodeIndex);
                                logger.info(inputHeaderOptionName + ": " + matchColumnsList);
                                if (matchColumnsList == null || matchColumnsList.size() != numOfFiles) {
                                        String matchingColOrder = getMatchingCode(colOrder);
                                        String exErr = "Error found in \"Columns to Match " + matchingColOrder + "\": one and only one column should be selected for matching from each file.";
                                        addErrorMessage(exErr);
                                        reqsMet = false;
                                } else {
                                        Set<Integer> matchColumnsSet = new HashSet<Integer>();
                                        for (InputHeaderOption iho : matchColumnsList) {
                                                matchColumnsSet.add(iho.getFileIndex());
                                        }
                                        List<Integer> matchColumnsIntList = new ArrayList(matchColumnsSet);
                                        Collections.sort(matchColumnsIntList);
                                        if (matchColumnsIntList.get(0) != 0 || matchColumnsIntList.get(matchColumnsIntList.size()-1) != numOfFiles -1 ||
                                                        matchColumnsIntList.size() != numOfFiles) {
                                                String matchingColOrder = getMatchingCode(colOrder);
                                                String exErr = "Error found in \"Columns to Match " + matchingColOrder + "\": one and only one column should be selected for matching from each file.";
                                                addErrorMessage(exErr);
                                                reqsMet = false;
                                        }
                                }
                        }
                        List<InputHeaderOption> compareColumnList = this.getInputHeaderOption("compareColumn", nodeIndex);
                        logger.info("compareColumnList:" + compareColumnList);
                        if (compareColumnList == null || compareColumnList.size() < numOfFiles) {
                                String exErr = "Error found in \"Columns to Compare\": at least one column should be selected for comparison from each file.";
                                addErrorMessage(exErr);
                                reqsMet = false;
                        } else {
                                Set<Integer> compareColumnSet = new HashSet<Integer>();
                                for (InputHeaderOption iho : compareColumnList) {
                                        compareColumnSet.add(iho.getFileIndex());
                                }
                                List<Integer> compareColumnIntList = new ArrayList(compareColumnSet);
                                Collections.sort(compareColumnIntList);
                                if (compareColumnIntList.get(0) != 0 || compareColumnIntList.get(compareColumnIntList.size()-1) != numOfFiles -1 ||
                                                compareColumnIntList.size() != numOfFiles) {
                                        String exErr = "Error found in \"Columns to Compare\": at least one column should be selected for comparison from each file.";
                                        addErrorMessage(exErr);
                                        reqsMet = false;
                                }
                        }
                        
                        
                        
                } else {
                        String exErr = "No input files uploaded.";
                        addErrorMessage(exErr);
                        reqsMet = false;
                }
        }
        
        if (reqsMet) {
                File outputDirectory = this.runExternal();
                if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
                        logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
                        File outputFile = new File(outputDirectory.getAbsolutePath() + "/comparison_result.txt");
                        if (outputFile != null && outputFile.exists()) {
                                Integer nodeIndex0 = 0;
                                Integer fileIndex0 = 0;
                                String label0 = "tab-delimited";
                                this.addOutputFile(outputFile, nodeIndex0, fileIndex0, label0);
                        } else {
                                String exErr = "An error has occurred. No output file is found. A common mistake is input files in wrong format.";
                                addErrorMessage(exErr);
                                logger.info(exErr);
                        }
                }
        }


        
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
        
        /*Causing error messages displayed twice
        for (String err : this.errorMessages) {
                // These will also be picked up by the workflows platform and relayed to the user.
                System.err.println(err);
        } */       
        
    }

    /**
     * Method to convert input XML file to tab-delimited text file.
     * @param inputFilePathName path to the input file
     * @param index the index for the input (0-3)
     * @return the generated text file
     */
    private File convertXML(String inputFilePathName, Integer index) {
            logger.info("Converting xml file: " + inputFilePathName);
            File generatedFile = null;
            String generatedFileName = "file" + index + "_converted.txt";
            SAXBuilder builder = new SAXBuilder();
            // Setting reuse parser to false is a workaround
            // for a JDK 1.7u45 bug described in
            // https://community.oracle.com/thread/2594170
            builder.setReuseParser(false);
            try {
                    String xmlStr = FileUtils.readFileToString(new File(inputFilePathName), null);
                    StringReader reader = new StringReader(xmlStr.replaceAll("[\r\n]+", ""));
                    Document doc = builder.build(reader);
                    logger.info("Input XML file is well-formed.");
                    List<Element> cList = doc.getRootElement().getChildren();
                    logger.info("Found root: " + doc.getRootElement().getName() + " with " + cList.size() + " children.");
                    Iterator<Element> iter = cList.iterator();
                    //HashMap<String, HashMap<String, String>> data = new HashMap<String, HashMap<String, String>>();
                    List<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
                    List<String> colNames = new ArrayList<String>();
                    List<String> rowNames = new ArrayList<String>();
                    while (iter.hasNext()) {
                            Element e = (Element) iter.next();
                            String rowName = e.getName();
                            /*HashMap<String, String> dataRow = new HashMap<String, String>();
                            data.put(rowName, dataRow);
                            if (!rowNames.contains(rowName)) {
                                    rowNames.add(rowName);
                                    List<Element> e_cList = e.getChildren();
                                    Iterator<Element> e_iter = e_cList.iterator();
                                    while (e_iter.hasNext()) {
                                            Element sub_e = (Element) e_iter.next();
                                            String entryKey = sub_e.getName();
                                            String entryVal = sub_e.getValue();
                                            dataRow.put(entryKey, entryVal);
                                            if (!colNames.contains(entryKey))
                                                    colNames.add(entryKey);
                                    }
                            }*/
                            rowNames.add(rowName);
                            HashMap<String, String> dataRow = new HashMap<String, String>();
                            data.add(dataRow);
                            List<Element> e_cList = e.getChildren();
                            Iterator<Element> e_iter = e_cList.iterator();
                            while (e_iter.hasNext()) {
                                    Element sub_e = (Element) e_iter.next();
                                    String entryKey = sub_e.getName();
                                    String entryVal = sub_e.getValue();
                                    if (!dataRow.containsKey(entryKey)) {
                                            dataRow.put(entryKey, entryVal);
                                    }
                                    if (!colNames.contains(entryKey)) {
                                            colNames.add(entryKey);
                                    }
                            }
                    }
                    //output
                    generatedFile = this.createFile(generatedFileName);
                    BufferedWriter bw = null;
                    try {
                            FileWriter fstream = new FileWriter(generatedFile);
                            bw = new BufferedWriter(fstream);
                            generatedFile.createNewFile();
                            //write out header
                            String headers = "tag";
                            for (String colName : colNames) {
                                headers += "\t" + colName;
                            }
                            bw.append(headers + "\n");
                            //write out content
                            /*for (String row : rowNames) {
                                    HashMap<String, String> rowData = data.get(row);
                                    for (String colName : colNames) {
                                            if (rowData.containsKey(colName)) {
                                                    row += "\t" + rowData.get(colName);
                                            } else {
                                                    row += "\t";
                                            }
                                    }
                                    bw.append(row + "\n");
                            }*/
                            for (int i = 0; i < rowNames.size(); i++) {
                                    String row = rowNames.get(i);
                                    HashMap<String, String> rowData = data.get(i);
                                    for (String colName : colNames) {
                                            if (rowData.containsKey(colName)) {
                                                    row += "\t" + rowData.get(colName);
                                            } else {
                                                    row += "\t";
                                            }
                                    }
                                    bw.append(row + "\n");
                            }

                    } catch (Exception e) {
                            String exErr = "Found error while writing output file: " + e.getMessage();
                            addErrorMessage(exErr);
                            logger.info(exErr);
                            return null;
                    } finally {
                            try {
                                if (bw != null) {
                                        bw.flush();
                                        bw.close();
                                }
                            } catch (IOException e) {
                                    String exErr = "Error found while closing output file: " + e.getMessage();
                                    addErrorMessage(exErr);
                                    logger.info(exErr);
                                    return null;
                            }
                    }
            } catch (IOException ioe) {
                    String exErr = "XML file not found. Error: " + ioe.getMessage();
                    addErrorMessage(exErr);
                    logger.info(exErr);
                    return null;
            } catch (JDOMException je) {
                    String exErr = "XML file in wrong format. Error: " + je.getMessage();
                    addErrorMessage(exErr);
                    logger.info(exErr);
                    return null;
            }
            return generatedFile;
    }
    
    private String getMatchingCode(int matchingNumber) {
            switch (matchingNumber) {
                    case 1:
                            return "First";
                    case 2:
                            return "Second";
                    case 3:
                            return "Third";
                    case 4:
                            return "Fourth";
                    case 5:
                            return "Fifth";
                    case 6:
                            return "Sixth";
                    case 7:
                            return "Seventh";
                    case 8:
                            return "Eighth";
                    case 9:
                            return "Ninth";
                    case 10:
                            return "Tenth";
            }
            return null;
        }

}
