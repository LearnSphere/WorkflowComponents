package edu.cmu.pslc.learnsphere.analysis.AnalysisIAfm;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class AnalysisIAfm extends AbstractComponent {

    /** Component option (model). */
    String modelName = null;

    public static void main(String[] args) {

        AnalysisIAfm tool = new AnalysisIAfm();
        tool.startComponent(args);
    }

    public AnalysisIAfm() {
        super();
    }

    @Override
    protected void parseOptions() {
	logger.info("Parsing options");

        if (this.getOptionAsString("model") != null) {
            modelName = this.getOptionAsString("model").replaceAll("(?i)\\s*KC\\s*\\((.*)\\)\\s*", "$1");
            logger.debug("modelName = " + modelName);
        }
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        this.addMetaDataFromInput("student-step", 0, 0, ".*");
        this.addMetaData("student-step", 0, META_DATA_LABEL, "label0", 0, "KC (" + modelName + ")");
    }

    @Override
    protected void runComponent() {
    	//make sure opportunity is numeric otherwise the component will run a long time
    	//this is to prevent multiskill model to run
    	boolean reqsMet = true;
    	File inputFile = getAttachment(0, 0);
        logger.info("iAFM inputFile: " + inputFile.getAbsolutePath());
        String opportunity = this.getOptionAsString("opportunity");
        if (!isColumnNumeric(inputFile.getAbsolutePath(), opportunity, true)) {
			String err = "Opportunity Column has non-numeric value.";
            addErrorMessage(err);
            logger.info("iAFM component is aborted: " + err);
            reqsMet = false;
		}
        
        if (reqsMet) {
        	// Run the program...
            File outputDirectory = this.runExternal();

            Integer fileIndex = 0;
            Integer nodeIndex = 0;
            String fileLabel = "student-step";
            File studentStepFile = new File(outputDirectory.getAbsolutePath() + "/student-step.txt");
            this.addOutputFile(studentStepFile, nodeIndex, fileIndex, fileLabel);

    	nodeIndex = 1;
            fileLabel = "model-values";
            File valuesFile = new File(outputDirectory.getAbsolutePath() + "/model-values.xml");
            this.addOutputFile(valuesFile, nodeIndex, fileIndex, fileLabel);

    	nodeIndex = 2;
    	fileLabel = "parameters";
            File paramsFile = new File(outputDirectory.getAbsolutePath() + "/parameters.xml");
            this.addOutputFile(paramsFile, nodeIndex, fileIndex, fileLabel);

        }               
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
        
    }
    
    private boolean isColumnNumeric(String filePath, String colName, boolean excludeSpecialChar) {
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
        	String cellVal = allCells[i][cumsumColInd];
        	if (excludeSpecialChar) {
        		if (cellVal.trim().equals("") || cellVal.trim().equals(".") ||
        				cellVal.trim().equalsIgnoreCase("none") ||
        				cellVal.trim().equalsIgnoreCase("null") ||
        				cellVal.trim().equalsIgnoreCase("na"))
        			continue;
        	}
        	if (!isNumeric(cellVal.trim()))
        		return false;
        }
        return true;
    }
    
    //in r col name with space should replace space with a "."
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
}
