package edu.cmu.pslc.learnsphere.analysis.fastAfmAndCv;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class FastAfmAndCvMain extends AbstractComponent {

    public static void main(String[] args) {

    	FastAfmAndCvMain tool = new FastAfmAndCvMain();
        tool.startComponent(args);
    }

    public FastAfmAndCvMain() {
        super();
    }
    
    /**
     * make the output file headers available to the downstream component
     */
    @Override
    protected void processOptions() {
        logger.debug("processing options");
        Integer outNodeIndex0 = 0;

        List<String> selectedCols = this.getMultiOptionAsString("columns");
        List<String> allColumns = new ArrayList<String>();

        //get all column labels
        List<Element> inputElements = this.inputXml.get(0);
        if (inputElements == null) {
            this.addErrorMessage("Python AFM Requires an input Student-Step file.");
            return;
        }
        for (Element inputElement : inputElements) {
            if (inputElement.getChild("files") != null && inputElement.getChild("files").getChildren() != null) {
                for (Element filesChild : (List<Element>) inputElement.getChild("files").getChildren()) {
                    if (filesChild.getChild("metadata") != null) {
                        Element inMetaElement = filesChild.getChild("metadata");
                        if (inMetaElement != null && !inMetaElement.getChildren().isEmpty()) {
                            for (Element child : (List<Element>) inMetaElement.getChildren()) {
                                if (child.getChild("name") != null
                                        && child.getChild("index") != null
                                        && child.getChild("id") != null) {
                                    String colLabel = child.getChildTextTrim("name");

                                    allColumns.add(colLabel);
                                }
                            }
                        }
                        break; // we only get metadata from one of the objects for now.. more code required to handle them separately
                    }
                }
            }
        }
        //add meta data for columns that will be in the output
        int c = 0;
        for (String col : allColumns) {
        	this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header" + c, c, col);
            c++;
        }
    }

    @Override
    protected void runComponent() {
        File inputFile = getAttachment(0, 0);
        logger.info("FastAfmAndCvMain inputFile: " + inputFile.getAbsolutePath());
        //check cv fold can't be over 10
        Boolean reqsMet = true;
        int cvFold = this.getOptionAsInteger("numFold");
        if (cvFold > 10) {
        	reqsMet = false;
        	//send error message
            String err = "The CV fold can't be over 10.";
            addErrorMessage(err);
            logger.info("FastAFMandCV is aborted: " + err);
            reqsMet = false;
        }
        if (reqsMet) {
	        File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            String label = "analysis-summary";
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/analysis_summary.txt");
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the FastAfmAndCvMain component: analysis_summary.txt can't be found.");
	            }
	            nodeIndex = 1;
	            label = "student-step";
	            File file1 = new File(outputDirectory.getAbsolutePath() + "/student_step_with_prediction.txt");
	            if (file1 != null && file1.exists()) {
	                this.addOutputFile(file1, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the FastAfmAndCvMain component: student_step_with_prediction.txt can't be found.");
	            }
	            nodeIndex = 2;
	            label = "model-values";
	            File file2 = new File(outputDirectory.getAbsolutePath() + "/model_values.xml");
	            if (file2 != null && file2.exists()) {
	                this.addOutputFile(file2, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the FastAfmAndCvMain component: model_values.xml can't be found.");
	            }
	            nodeIndex = 3;
	            label = "parameters";
	            File file3 = new File(outputDirectory.getAbsolutePath() + "/parameters.xml");
	            if (file3 != null && file3.exists()) {
	                this.addOutputFile(file3, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the FastAfmAndCvMain component: parameters.xml can't be found.");
	            }
	        }
        }
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());

    }
    
}
