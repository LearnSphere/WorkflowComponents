package edu.cmu.pslc.learnsphere.analysis.AnalysisDash;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class AnalysisDash extends AbstractComponent {

    /** Component option (model). */
    String modelName = null;

    public static void main(String[] args) {

        AnalysisDash tool = new AnalysisDash();
        tool.startComponent(args);
    }

    public AnalysisDash() {
        super();
    }

   
    @Override
    protected void parseOptions() {
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        this.addMetaDataFromInput("student-step", 0, 0, ".*");
        this.addMetaData("student-step", 0, META_DATA_LABEL, "label0", 0, "KC");
    }
    
    
    @Override
    protected void runComponent() {
        // Run the program...
        File outputDirectory = this.runExternalMultipleFileOuput();

        Integer fileIndex = 0;
        Integer nodeIndex = 0;
        String fileLabel = "student-step";
        File studentStepFile = new File(outputDirectory.getAbsolutePath() + "/student-step.txt");
        this.addOutputFile(studentStepFile, nodeIndex, fileIndex, fileLabel);

	nodeIndex = 1;
        fileLabel = "model-values";
        File valuesFile = new File(outputDirectory.getAbsolutePath() + "/model-values.txt");
        this.addOutputFile(valuesFile, nodeIndex, fileIndex, fileLabel);

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }
}
