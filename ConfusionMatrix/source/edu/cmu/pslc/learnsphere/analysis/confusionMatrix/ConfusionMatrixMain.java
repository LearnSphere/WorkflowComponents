package edu.cmu.pslc.learnsphere.analysis.confusionMatrix;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class ConfusionMatrixMain extends AbstractComponent {

    public static void main(String[] args) {

    	ConfusionMatrixMain tool = new ConfusionMatrixMain();
        tool.startComponent(args);
    }

    public ConfusionMatrixMain() {
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
        File inputFile1 = getAttachment(0, 0);
        logger.info("TutorTranscriptEvaluation inputFile transcript: " + inputFile1.getAbsolutePath());
        File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            String newFileName = "confusion_matrix.html";
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/" + newFileName);
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, "inline-html");
	            } else {
	                addErrorMessage("An error has occurred with the ConfusionMatrix component: " + newFileName + " can't be found.");
	            }
	            nodeIndex = 1;
	            newFileName = "confusion_matrix.csv";
	            File file1 = new File(outputDirectory.getAbsolutePath() + "/" + newFileName);
	            if (file1 != null && file1.exists()) {
	                this.addOutputFile(file1, nodeIndex, fileIndex, "csv");
	            } else {
	                addErrorMessage("An error has occurred with the ConfusionMatrix component: " + newFileName + " can't be found.");
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
