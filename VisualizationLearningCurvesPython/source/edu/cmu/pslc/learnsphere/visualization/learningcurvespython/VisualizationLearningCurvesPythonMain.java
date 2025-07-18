package edu.cmu.pslc.learnsphere.visualization.learningcurvespython;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class VisualizationLearningCurvesPythonMain extends AbstractComponent {

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {
    	VisualizationLearningCurvesPythonMain tool = new VisualizationLearningCurvesPythonMain();
        tool.startComponent(args);
    }

   public VisualizationLearningCurvesPythonMain() {

        super();
    }

    @Override
    protected void runComponent() {
    	Boolean reqsMet = true;
        File inputStudentStep = getAttachment(0, 0);
        File inputParameters = getAttachment(1, 0);
        //verify the inputParameters file
        Boolean categorizeLearningCurve = this.getOptionAsBoolean("categorizeLearningCurve");
        String learningCurveMetric = this.getOptionAsString("learningCurveMetric");
        if (categorizeLearningCurve != null && categorizeLearningCurve &&
    			(learningCurveMetric.equals("Error Rate") || learningCurveMetric.equals("Predicted Error Rate"))) {
    		if (inputParameters == null) {
    			reqsMet = false;
            	//send error message
                String err = "VisualizationLearningCurvesPython is aborted because the parameters input file is missing. ";
                addErrorMessage(err);
                logger.info(err);
    		} else {
    			//check the extension is xml of .txt
    			//get inputFile extension
    	        Path path = Paths.get(inputParameters.getAbsolutePath());
    	        String filename = path.getFileName().toString();
    	        String fileExtension = "";
    	        int dotIndex = filename.lastIndexOf('.');
    	        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
    	        	fileExtension = filename.substring(dotIndex + 1);
    	        }
    	        if (!fileExtension.equalsIgnoreCase("xml") && !fileExtension.equalsIgnoreCase("txt")) {
    	        	reqsMet = false;
                	//send error message
                    String err = "VisualizationLearningCurvesPython is aborted because the parameters input file is not XML or TXT. ";
                    addErrorMessage(err);
                    logger.info(err);
    	        }
    		}
    	}
    	if (reqsMet) {
    		//secondary model and primary model can't be the same
    		Boolean viewSecondary = this.getOptionAsBoolean("viewSecondary");
    		String primaryModel = this.getOptionAsString("primaryModel");
    		String secondaryModel = this.getOptionAsString("secondaryModel");
    		if (viewSecondary && primaryModel.equals(secondaryModel)) {
	        	reqsMet = false;
            	//send error message
                String err = "VisualizationLearningCurvesPython is aborted, the primary and secondary models can not be the same. ";
                addErrorMessage(err);
                logger.info(err);
	        }
    	}
    	
    	if (reqsMet) {
        	File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            
	            String newFileName = "all_final.html";
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/" + newFileName);
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, "inline-html");
	                System.out.println(this.getOutput());
	            } else {
	            	reqsMet = false;
	            	String err = "An error has occurred with the VisualizationLearningCurvesPython component: " + newFileName + " can't be found.";
	                addErrorMessage(err);
	                logger.info("VisualizationLearningCurvesPython is aborted: " + err);
	            }
	        }
        }
        
        if (!reqsMet) {
	        for (String err : this.errorMessages) {
	                // These will also be picked up by the workflows platform and relayed to the user.
	                System.err.println(err);
	        }
        }
    
    }

    
}
