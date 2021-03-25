package edu.cmu.pslc.learnsphere.transform.join;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class JoinMain extends AbstractComponent {

    public static void main(String[] args) {

    	JoinMain tool = new JoinMain();
        tool.startComponent(args);
    }

    public JoinMain() {
        super();
    }
    
    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        this.addMetaDataFromInput("text", 0, 0, ".*");
        this.addMetaDataFromInput("text", 1, 0, ".*");

    }

    @Override
    protected void runComponent() {
        File file1 = this.getAttachment(0, 0);
        File file2 = this.getAttachment(1, 0);
        logger.info("JoinMain inputFile1: " + file1.getAbsolutePath());
        logger.info("JoinMain inputFile2: " + file2.getAbsolutePath());
        this.setOption("file_1", file1.getAbsolutePath());
        this.setOption("file_2", file2.getAbsolutePath());
        String outputFileName = "/joinedResult.txt";
        logger.info("JoinMain outputFileName: " + outputFileName);
        
        //file delimiter
        String file1Delimiter = this.getOptionAsString("file1Delimiter");
        String file2Delimiter = this.getOptionAsString("file2Delimiter");
        //get file headers
        String[] file1Headers = getHeaderFromFile(file1, file1Delimiter);
        String[] file2Headers = getHeaderFromFile(file2, file2Delimiter);
        
        String howToJoin  = this.getOptionAsString("howToJoin");
        if (howToJoin.equalsIgnoreCase("merge")) {
        	String numColumnsToMerge_str  = this.getOptionAsString("numColumnsToMerge");
        	int numColumnsToMerge = 0;
        	try {
        		numColumnsToMerge = Integer.parseInt(numColumnsToMerge_str);
        	} catch (NumberFormatException e) {
        		addErrorMessage("Number of merging conditions should be an integer.");
        	}
        	if (numColumnsToMerge < 0 || numColumnsToMerge > 5) {
        		addErrorMessage("Number of merging conditions should be an integer between 1 and 5.");
        		
        	}
        	//process each file1MatchColumnsN and file2MatchColumnsN
        	String file1MatchColumnsVals = "";
    		String file2MatchColumnsVals = "";
        	for (int i = 1; i <= numColumnsToMerge; i++ ) {
        		String file1MatchColumnsVarName = "file1MatchColumns" + i;
        		String file2MatchColumnsVarName = "file2MatchColumns" + i;
        		String file1MatchColumnsVarVal = this.getOptionAsString(file1MatchColumnsVarName);
        		String file2MatchColumnsVarVal = this.getOptionAsString(file2MatchColumnsVarName);
        		//check the headers of each files. don't allow columns with same headers
        		if (!isUniqueHeaderFromFile(file1Headers, file1MatchColumnsVarVal)) {
        			addErrorMessage("Match column in file 1 is not unique");
        		}
        		if (!isUniqueHeaderFromFile(file2Headers, file2MatchColumnsVarVal)) {
        			addErrorMessage("Match column in file 2 is not unique");
        		}
        		file1MatchColumnsVals += file1MatchColumnsVarVal;
        		if (i < numColumnsToMerge)
        			file1MatchColumnsVals += ",";
        		file2MatchColumnsVals += file2MatchColumnsVarVal;
        		if (i < numColumnsToMerge)
        			file2MatchColumnsVals += ",";
        	}
        	//delete the last character if it is ","
        	file1MatchColumnsVals = file1MatchColumnsVals.trim();
        	file2MatchColumnsVals = file2MatchColumnsVals.trim();
        	if (file1MatchColumnsVals.substring(file1MatchColumnsVals.length() - 1).equals(","))
        		file1MatchColumnsVals = StringUtils.substring(file1MatchColumnsVals, 0, file1MatchColumnsVals.length() - 1);
        	if (file2MatchColumnsVals.substring(file2MatchColumnsVals.length() - 1).equals(","))
        		file2MatchColumnsVals = StringUtils.substring(file2MatchColumnsVals, 0, file2MatchColumnsVals.length() - 1);
        		
        	this.setOption("file_1_match_columns", file1MatchColumnsVals);
            this.setOption("file_2_match_columns", file2MatchColumnsVals);
        }
        File outputDirectory = this.runExternal();
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
                logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
                File file0 = new File(outputDirectory.getAbsolutePath() + outputFileName);
                if (file0 != null && file0.exists()) {
                        Integer nodeIndex0 = 0;
                        Integer fileIndex0 = 0;
                        String label0 = "tab-delimited";
                        this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);
                } else {
                        addErrorMessage("An unknown error has occurred with the Join component.");
                }
        }
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());

    }
    
    /**
     * Given a File, read and return the first line.
     * @param file the File to read
     * @param delimiter file delimiter
     * @return String[] first line of the file which is hopefully the column headings
     */
    public String[] getHeaderFromFile(File file, String delimiter) {
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
    
    /**
     * Check if a column name is unique in headers
     * @param headers 
     * @param columnNameToCheck column name to check
     * @return boolean if columnNameToCheck is unique
     */
    private boolean isUniqueHeaderFromFile(String[] headers, String columnNameToCheck) {
        //loop through headers
    	logger.info("columnNameToCheck:" + columnNameToCheck);
        boolean found = false;
        for (String header : headers) {
        	logger.info("header:" + header);
        	if (header.equals(columnNameToCheck)) {
        		if (found)
        			return false;
        		else
        			found = true;
        	}
        }
        return true;
    }
    
}
