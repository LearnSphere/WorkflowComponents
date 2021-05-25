package edu.cmu.pslc.learnsphere.analysis.fastAfmAndCv;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
	            Integer nodeIndex0 = 0;
	            Integer fileIndex = 0;
	            String label = "analysis-summary";
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/analysis_summary.txt");
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex0, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the FastAfmAndCvMain component: analysis_summary.txt can't be found.");
	            }
	            fileIndex = 1;
	            label = "student-step";
	            File file1 = new File(outputDirectory.getAbsolutePath() + "/student_step_with_prediction.txt");
	            if (file1 != null && file1.exists()) {
	                this.addOutputFile(file1, nodeIndex0, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the FastAfmAndCvMain component: student_step_with_prediction.txt can't be found.");
	            }
	            fileIndex = 2;
	            label = "model-values";
	            File file2 = new File(outputDirectory.getAbsolutePath() + "/model_values.xml");
	            if (file2 != null && file2.exists()) {
	                this.addOutputFile(file2, nodeIndex0, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the FastAfmAndCvMain component: model_values.xml can't be found.");
	            }
	            fileIndex = 3;
	            label = "parameters";
	            File file3 = new File(outputDirectory.getAbsolutePath() + "/parameters.xml");
	            if (file3 != null && file3.exists()) {
	                this.addOutputFile(file3, nodeIndex0, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the FastAfmAndCvMain component: parameters.xml can't be found.");
	            }
	        }
        }
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());

    }
    
}
