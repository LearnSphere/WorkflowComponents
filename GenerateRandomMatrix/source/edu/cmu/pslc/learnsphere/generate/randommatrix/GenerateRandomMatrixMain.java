package edu.cmu.pslc.learnsphere.generate.randommatrix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

import static edu.cmu.pslc.datashop.util.FileUtils.truncateFile;

public class GenerateRandomMatrixMain extends AbstractComponent {

	private static final String NEW_LINE_CHAR = "\r\n";
    public static void main(String[] args) {

        GenerateRandomMatrixMain tool = new GenerateRandomMatrixMain();
        tool.startComponent(args);
    }

    public GenerateRandomMatrixMain() {
        super();
    }


    /**
     * The test() method is used to test the known inputs prior to running.
     * @return true if passing, false otherwise
     */
    @Override
    protected Boolean test() {
        Boolean passing = true;

        return passing;
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // Add the column headers from our input file to this component's output metadata,
        // plus one extra column for Predicted Error Rate, if it doesn't already exist.

        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        Integer rows = this.getOptionAsInteger("rows");
        Integer columns = this.getOptionAsInteger("columns");
        // There is only one output node so the output node index is 0
        Integer outNodeIndex = 0;
        // There is only one output file created for the node (not a list of files) so the file index is 0.
        Integer fileIndex = 0;
        this.addMetaData("tab-delimited", outNodeIndex, META_DATA_HEADER, "rowheader", fileIndex, "Row Name");
        for (Integer i = 0; i < columns; i++) {
        	this.addMetaData("tab-delimited", outNodeIndex, META_DATA_HEADER, "header" + i, fileIndex, "Column[" + i + "]");
        }
    }

    /**
     * Processes the student-step file and verify the first N lines.
     */
    @Override
    protected void runComponent() {

        File theFile = this.createFile("myMatrix.txt");

        if (this.getOptionAsString("rows") != null && this.getOptionAsString("columns") != null
        		&& this.getOptionAsString("randomSeed") != null) {
        	Integer rows = this.getOptionAsInteger("rows");
        	Integer columns = this.getOptionAsInteger("columns");
        	Integer randomSeed = this.getOptionAsInteger("randomSeed");
        	Random generator = new Random(randomSeed);
        	BufferedWriter bw = null;
        	DecimalFormat decimalFormat = new DecimalFormat("#,###.###");
        	try {
	        	FileWriter fw = new FileWriter(theFile);
	        	bw = new BufferedWriter(fw);

	    		if (bw != null) {
	    			bw.write("Row Name\t");
	    			for (int i = 0; i < columns; i++) {
	    				bw.write("Column[" + i + "]");
	    				if (i < columns - 1) {
	    					bw.write("\t");
	    				}
	    			}
	    			bw.write(NEW_LINE_CHAR);

		    		for (int i = 0; i < columns; i++) {

		    			bw.write("Row [" + i + "]\t");

		    			for (int j = 0; j < rows; j++) {
		    				bw.write(decimalFormat.format(generator.nextDouble()));
		    				if (j < rows - 1) {
		    					bw.write("\t");
		    				}
		        		}
		    			bw.write(NEW_LINE_CHAR);
		        	}
        			bw.close();
        		}
        	} catch (IOException e) {

        	} finally {

        	}
        }

        try {
        	Integer nodeIndex = 0;
            Integer fileIndex = 0;
            String fileType = "tab-delimited";
            this.addOutputFile(theFile, nodeIndex, fileIndex, fileType);

        } catch (Exception e) {

        }

        System.out.println(this.getOutput());
    }

    /**
     * Parse the options list.
     */
    @Override protected void parseOptions() {

    }



}
