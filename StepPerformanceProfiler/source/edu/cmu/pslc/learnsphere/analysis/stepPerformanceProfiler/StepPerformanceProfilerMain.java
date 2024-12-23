package edu.cmu.pslc.learnsphere.analysis.stepPerformanceProfiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class StepPerformanceProfilerMain extends AbstractComponent {

    public static void main(String[] args) {

    	StepPerformanceProfilerMain tool = new StepPerformanceProfilerMain();
        tool.startComponent(args);
    }

    public StepPerformanceProfilerMain() {
        super();
    }
    
    /**
     * make the output file headers available to the downstream component
     */
    @Override
    protected void processOptions() {
        
    }

    @Override
    protected void runComponent() {
        File inputFile = getAttachment(0, 0);
        logger.info("StepPerformanceProfilerMain inputFile: " + inputFile.getAbsolutePath());
        Boolean reqsMet = true;
        String[] requiredCols = {"Row", "Anon Student Id", "Problem Name", "Step Name", "Problem View", "Outcome", "Input", "Selection", "Action"};
        String[] headers = null;
        //make sure the input file has requeired columns
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            // Read the first line of the file
            String headerLine = br.readLine();
            if (headerLine != null) {
                headers = headerLine.split("\t");
            }
        } catch (IOException e) {
        	addErrorMessage("Error reading the input file: " + inputFile.getAbsolutePath() + e.getMessage());
        	reqsMet = false;
        }

        //make sure all required column are present
        Set<String> headersSet = new HashSet<>(Arrays.asList(headers));
        // Check if all elements of the subset are in the superset
        for (String col : requiredCols) {
            if (!headersSet.contains(col)) {
            	addErrorMessage("Missing required column: " + col);
            	reqsMet = false;
            }
        }
        
        if (reqsMet) {
        	File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            
	            String newFileName = "step_performance_profiler.txt";
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/" + newFileName);
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, "txt");
	            } else {
	                addErrorMessage("An error has occurred: " + newFileName + " can't be found.");
	            }
	        }
        }
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
        
        for (String err : this.errorMessages) {
                // These will also be picked up by the workflows platform and relayed to the user.
                System.err.println(err);
        }

    }
    
    
}
