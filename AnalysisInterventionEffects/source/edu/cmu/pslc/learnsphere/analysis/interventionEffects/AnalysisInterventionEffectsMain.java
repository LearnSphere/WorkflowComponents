package edu.cmu.pslc.learnsphere.analysis.interventionEffects;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class AnalysisInterventionEffectsMain extends AbstractComponent {

    public static void main(String[] args) {

        AnalysisInterventionEffectsMain tool = new AnalysisInterventionEffectsMain();
        tool.startComponent(args);
    }

    public AnalysisInterventionEffectsMain() {
        super();
    }

    @Override
    protected void runComponent() {
        //get/set -f option
    	boolean reqsMet = true;
        File inputFile = getAttachment(0, 0);
        logger.info("AnalysisInterventionEffects inputFile: " + inputFile.getAbsolutePath());
        //check hadInterventionColumn column, has to be 0 or 1
        String hadInterventionColumn = this.getOptionAsString("hadInterventionColumn");
        if (!isColumnZeroOrOne(inputFile.getAbsolutePath(), hadInterventionColumn)) {
			//send error message
            String err = "Had Intervention Column has to be 0 or 1.";
            addErrorMessage(err);
            logger.info("AnalysisInterventionEffects is aborted: " + err);
            reqsMet = false;
		}
        //check hadInterventionColumn column, has to be 0 or 1
        String postInterventionColumn = this.getOptionAsString("postInterventionColumn");
        if (!isColumnZeroOrOne(inputFile.getAbsolutePath(), postInterventionColumn)) {
			//send error message
            String err = "Pre Post Intervention Column has to be 0 or 1.";
            addErrorMessage(err);
            logger.info("AnalysisInterventionEffects is aborted: " + err);
            reqsMet = false;
		}
        
        //measurementColumnN is numeric
        String numberOfMeasurements = this.getOptionAsString("numberOfMeasurements");
        int numberOfMeasurements_int = -1;
        if (isNumeric(numberOfMeasurements)) {
        	try {
        		numberOfMeasurements_int = Integer.parseInt(numberOfMeasurements);
        		if (numberOfMeasurements_int < 0 || numberOfMeasurements_int > 5) {
        			//send error message
                    String err = "Number of Measurements can't be negative number or greater than 5.";
                    addErrorMessage(err);
                    logger.info("AnalysisInterventionEffects is aborted: " + err);
                    reqsMet = false;
        		}
            } catch (NumberFormatException e) {
            	//send error message
                String err = "Number of Measurements has to be an integer";
                addErrorMessage(err);
                logger.info("AnalysisInterventionEffects is aborted: " + err);
                reqsMet = false;
            }
        	if (reqsMet) {
        		for (int i = 1; i <= numberOfMeasurements_int; i++) {
        			String measurementColName = "measurementColumn" + i;
        			String measurementCol = this.getOptionAsString(measurementColName);
        			if (!isColumnNumeric(inputFile.getAbsolutePath(), measurementCol)) {
        				//send error message
        	            String err = "Measurement " + i + " Column has non-numeric value.";
        	            addErrorMessage(err);
        	            logger.info("AnalysisInterventionEffects is aborted: " + err);
        	            reqsMet = false;
        			}
        		}
        	}
        }
        
        if (reqsMet) {
	        File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            String label = "analysis-summary";
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/analysis_result.txt");
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the AnalysisInterventionEffects component: analysis_result.txt can't be found.");
	            }
	            nodeIndex = 1;
	            label = "pdf";
	            File file1 = new File(outputDirectory.getAbsolutePath() + "/analysis_result_plots.pdf");
	            if (file1 != null && file1.exists()) {
	                this.addOutputFile(file1, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the AnalysisInterventionEffects component: analysis_result_plots.pdf can't be found.");
	            }
	            
	        }
        }
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
        
    }
    
    
    private boolean isColumnZeroOrOne(String filePath, String colName) {
    	String[][] allCells = IOUtil.read2DStringArray(filePath, "\t");
        String[] headers = allCells[0];
        if (headers.length < 2) {
        	allCells = IOUtil.read2DStringArray(filePath, ",");
            headers = allCells[0];
        }
        int cumsumColInd = -1;
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            header = header.trim();
            //also delete the quotes if there is 
            header = header.replaceAll("^\"|\"$", "");
            if (header.equals(colName)) {
            	cumsumColInd = i;
            }
        }
        
        if (cumsumColInd == -1)
        	return false;
        for (int i = 1; i < allCells.length; i++) {
        	if (!isZeroOrOne(allCells[i][cumsumColInd]))
        		return false;
        }
        return true;
    }
    
    private boolean isColumnNumeric(String filePath, String colName) {
    	String[][] allCells = IOUtil.read2DStringArray(filePath, "\t");
        String[] headers = allCells[0];
        if (headers.length < 2) {
        	allCells = IOUtil.read2DStringArray(filePath, ",");
            headers = allCells[0];
        }
        int cumsumColInd = -1;
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            header = header.trim();
            //also delete the quotes if there is 
            header = header.replaceAll("^\"|\"$", "");
            if (header.equals(colName)) {
            	cumsumColInd = i;
            }
        }
        
        if (cumsumColInd == -1)
        	return false;
        for (int i = 1; i < allCells.length; i++) {
        	if (!isNumeric(allCells[i][cumsumColInd]))
        		return false;
        }
        return true;
    }
    
    private boolean isNumeric(String val) {
    	boolean valIsNumber = false;
    	try {
        	Double.parseDouble(val);
        	valIsNumber = true;
        } catch (NumberFormatException e) {
        } finally {
        	return valIsNumber;
        }
    }
    
    private boolean isZeroOrOne(String val) {
    	boolean valZeroOrOne = false;
    	if (val.trim().equals("0") || val.trim().equals("1"))
    		valZeroOrOne = true;
        return valZeroOrOne;
    }
}
