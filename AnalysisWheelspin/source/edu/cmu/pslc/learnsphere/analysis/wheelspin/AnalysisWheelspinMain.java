package edu.cmu.pslc.learnsphere.analysis.wheelspin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;

import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import edu.cmu.pslc.afm.dataObject.AFMDataObject;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.dataStructure.TrainingResult;

// The PenalizedAFMTransferModel applies the AFM to the student-step data.
import edu.cmu.pslc.afm.transferModel.AFMTransferModel;
import edu.cmu.pslc.afm.transferModel.PenalizedAFMTransferModel;
import edu.cmu.pslc.datashop.servlet.workflows.WorkflowImportHelper;
// The AbstractComponent class is required by each component.
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: Analysis wheelspin main class.
 */
public class AnalysisWheelspinMain extends AbstractComponent {
	/** Default value for 'model' in schema. */
    private static final String DEFAULT_MODEL = "\\s*KC\\s*\\((.*)\\)\\s*";
    /** Component option (model). */
    String modelName = null;

    /** XML doc transformer. */
    Transformer transformer = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

    	AnalysisWheelspinMain tool = new AnalysisWheelspinMain();
        tool.startComponent(args);
    }

    /**
     * This class runs AFM one or more times depending on the number of input elements.
     */
    public AnalysisWheelspinMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // Add the column headers from our input file to this component's output metadata,
        // plus one extra column for Predicted Error Rate, if it doesn't already exist.

        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        Integer outNodeIndex0 = 0;
        //this.addMetaDataFromInput("student-step", 0, outNodeIndex0, ".*");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header0", 0, "Anon Student Id");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header1", 1, "KC (" + modelName + ")");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header2", 2, "Count of Cases");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header3", 3, "Count of Correct First Attempts");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header4", 4, "Count of Incorrect First Attempts");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header5", 5, "Count of Maximum Consecutive Correct First Attempts");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header6", 6, "Assistment Score");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header7", 7, "iAFM Overall Slope");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header8", 8, "iAFM Overall Slope Std Error");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header9", 9, "iAFM KC Slope");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header10", 10, "iAFM KC Slope Std Error");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header11", 11, "iAFM Student Slope");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header12", 12, "iAFM Student Slope Std Error");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header13", 13, "iAFM Overall Intercept");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header14", 14, "iAFM KC Intercept");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header15", 15, "iAFM Student Intercept");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header16", 16, "Predictive Slope");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header17", 17, "Predictive Slope Std Error");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header18", 18, "Predictive Intercept");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header19", 19, "Predictive Slope CI Upper Bound");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header20", 20, "Predictive Slope CI Lower Bound");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header21", 21, "Predictive Model Prediction");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header22", 22, "Predictive Model Progress Probability");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header23", 23, "Beck Model Prediction");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header24", 24, "Local Measurement Slope");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header25", 25, "Local Measurement Slope Std Error");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header26", 26, "Local Measurement Intercept");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header27", 27, "Local Measurement Slope CI upper Bound");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header28", 28, "Local Measurement Slope CI lower Bound");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header29", 29, "Local Measurement Prediction");
        this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header30", 30, "Local Measurement Progress Probability");
        
    }

    @Override
    protected void parseOptions() {
        if (this.getOptionAsString("model") != null) {
            modelName = this.getOptionAsString("model").replaceAll("(?i)\\s*KC\\s*\\((.*)\\)\\s*", "$1");
            if (modelName.equals(DEFAULT_MODEL)) {
                // This will happen when component has no input or we've failed to parse input headers.
                logger.info("modelName not specified: " + DEFAULT_MODEL);
                modelName = null;
            }
        }
        logger.info("Parse Options. modelName: " + modelName);
    }
    

    /**
     * Processes the student-step file and associated model name to generate
     * the inputs to the next component.
     */
    @Override
    protected void runComponent() {
    	//process files
        File dataFile = this.getAttachment(0, 0);
        logger.info("dataFile: " + dataFile.getAbsolutePath());
        
        //process files, make sure delimiter is \t or ,
        char delim = '\t';
        LinkedHashMap<String, Integer> columnHeaders = WorkflowImportHelper.getColumnHeaders(dataFile, delim);
        if (columnHeaders.size() <= 1) {
        	//try ,
        	delim = ',';
            columnHeaders = WorkflowImportHelper.getColumnHeaders(dataFile, delim);
            if (columnHeaders.size() <= 1) {
                //throw error if not CSV, send error message
                String errMsgForUI = "Input file should be in tab-delimited or CSV format.";
                String errMsgForLog = errMsgForUI + " File: " + dataFile.getAbsolutePath();
                handleAbortingError (errMsgForUI, errMsgForLog);
                return;
            }
        }
        //make sure required columns exist
        if (!columnHeaders.containsKey("Anon Student Id") || !columnHeaders.containsKey("Problem Name") ||
        		!columnHeaders.containsKey("Step Name") || !columnHeaders.containsKey("First Attempt") ||
        		!columnHeaders.containsKey("KC (" + modelName + ")") || !columnHeaders.containsKey("Opportunity (" + modelName + ")") ) {
        	String errMsgForUI = "Input file is missing one of more required columns(case sensitive): " +
        							"Anon Student Id, Problem Name, Step Name, First Attempt, KC (model_name), Opportunity (model_name)";
            String errMsgForLog = errMsgForUI + " File: " + dataFile.getAbsolutePath();
            handleAbortingError (errMsgForUI, errMsgForLog);
        }
        
        
        File outputDirectory = this.runExternal();
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
                logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
                File outputFileResult = new File(outputDirectory.getAbsolutePath() + "/wheelspin_result.txt");
                File outputFileParameters = new File(outputDirectory.getAbsolutePath() + "/wheelspin_parameters.txt");
                
                if (outputFileResult != null && outputFileResult.exists() &&
                		outputFileParameters != null && outputFileParameters.exists()) {
                        Integer nodeIndex = 0;
                        Integer fileIndex = 0;
                        String label = "tab-delimited";
                        this.addOutputFile(outputFileResult, nodeIndex, fileIndex, label);
                        
                        nodeIndex = 1;
                        label = "parameters";
                        this.addOutputFile(outputFileParameters, nodeIndex, fileIndex, label);

                            
                } else {
                        String exErr = "An error has occurred. No output file is found.";
                        addErrorMessage(exErr);
                        logger.info(exErr);
                }
        }
        
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());

    }
    
    private void handleAbortingError (String errMsgForUI, String errMsgForLog) {
        addErrorMessage(errMsgForUI);
        logger.info("Wheelspin Analysis aborted: " + errMsgForLog );
        System.out.println(getOutput());
        return; 
}

    
}
