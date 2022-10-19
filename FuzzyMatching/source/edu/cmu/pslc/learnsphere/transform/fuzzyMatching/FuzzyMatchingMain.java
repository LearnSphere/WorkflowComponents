package edu.cmu.pslc.learnsphere.transform.fuzzyMatching;

import java.io.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;

import org.codehaus.plexus.util.FileUtils;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.workflows.InputHeaderOption;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FuzzyMatchingMain extends AbstractComponent {

    public static void main(String[] args) {

            FuzzyMatchingMain tool = new FuzzyMatchingMain();
        tool.startComponent(args);
    }

    public FuzzyMatchingMain() {
        super();
    }

    //make output file's headers available for the downstream components
    protected void processOptions() {
	}


    @Override
    protected void runComponent() {
    	Boolean reqsMet = true;
    	String matchingMode = this.getOptionAsString("matching_mode");
    	int threshold = this.getOptionAsInteger("threshold");
    	if (threshold > 95 || threshold < 40) {
    		reqsMet = false;
        	//send error message
            String err = "The threshold should be between 40 and 95";
            addErrorMessage(err);
            logger.info("FuzzyMatching is aborted: " + err);
    	}
    	String stringToMatch = this.getOptionAsString("string_to_match");
    	if (matchingMode.equalsIgnoreCase("file to string") && 
    			(stringToMatch == null ||stringToMatch.trim().equals("")) ) {
    		reqsMet = false;
        	//send error message
            String err = "String to Match can't be empty.";
            addErrorMessage(err);
            logger.info("FuzzyMatching is aborted: " + err);
    	}
    	//the first input file is required
    	File inputFile0 = getAttachment(0, 0); 
    	if (inputFile0 == null) {
    		reqsMet = false;
        	//send error message
            String err = "The first input file is required.";
            addErrorMessage(err);
            logger.info("FuzzyMatching is aborted: " + err);
    	}
    	//if file to file, second input file is required
    	File inputFile1 = null;
    	if (matchingMode.equalsIgnoreCase("file to file")) {
    		inputFile1 = getAttachment(1, 0);
    		logger.info("inputFile1: " + inputFile1);
	    	if (inputFile1 == null) {
	    		reqsMet = false;
	        	//send error message
	            String err = "A second input file is required for File to File matching.";
	            addErrorMessage(err);
	            logger.info("FuzzyMatching is aborted: " + err);
	    	}
	    	int numCol = this.getOptionAsInteger("num_column_to_match");
	    	if (numCol > 4) {
	        	reqsMet = false;
	        	//send error message
	            String err = "The Fuzzy Matching only match up to 4 columns.";
	            addErrorMessage(err);
	            logger.info("FuzzyMatching is aborted: " + err);
	            reqsMet = false;
	        }
    	}
    	if (reqsMet) {
    		if (matchingMode.equalsIgnoreCase("file to file")) {
	    		String file1Columns = null;
		    	String file2Columns = null;
		    	int numCol = this.getOptionAsInteger("num_column_to_match");
		    	for (int i = 1; i <= numCol; i++) {
			    	String file1Name = "file1_column" + i + "_to_match";
			    	String file2Name = "file2_column" + i + "_to_match";
			    	List<InputHeaderOption> file1MatchColumnList =  this.getInputHeaderOption(file1Name, 0);
			    	List<InputHeaderOption> file2MatchColumnList =  this.getInputHeaderOption(file2Name, 1);
		    		String file1SelectedCol = file1MatchColumnList.get(0).getOptionValue();
		    		String file2SelectedCol = file2MatchColumnList.get(0).getOptionValue();
			    	if (file1Columns == null) {
			    		file1Columns = file1SelectedCol;
			    	} else {
			    		file1Columns += "_" + file1SelectedCol;
			    	}
			    	if (file2Columns == null) {
			    		file2Columns = file2SelectedCol;
			    	} else {
			    		file2Columns += "_" + file2SelectedCol;
			    	}
		    	}
		    	this.setOption("file1_columns", file1Columns);
		    	this.setOption("file2_columns", file2Columns);
    		}
	        File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            String label = "tab-delimited";
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/fuzzy_matching_result.txt");
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with Fuzzy Matching component: fuzzy_matching_result.txt can't be found.");
	            }
	        }
    	}

        // Send the component output back to the workflow.
      System.out.println(this.getOutput());
    }

}
