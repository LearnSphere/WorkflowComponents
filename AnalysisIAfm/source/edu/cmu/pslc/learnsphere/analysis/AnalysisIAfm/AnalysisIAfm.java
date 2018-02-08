package edu.cmu.pslc.learnsphere.analysis.AnalysisIAfm;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

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
        // Run the program...
        File outputDirectory = this.runExternal();

        Integer fileIndex = 0;
        Integer nodeIndex = 0;
        String fileLabel = "student-step";
        File studentStepFile = new File(outputDirectory.getAbsolutePath() + "/student-step.txt");
        this.addOutputFile(studentStepFile, nodeIndex, fileIndex, fileLabel);

	nodeIndex = 1;
        fileLabel = "model-values";
        File valuesFile = new File(outputDirectory.getAbsolutePath() + "/model-values.txt");
        this.addOutputFile(valuesFile, nodeIndex, fileIndex, fileLabel);

	nodeIndex = 2;
	fileLabel = "parameters";
        File paramsFile = new File(outputDirectory.getAbsolutePath() + "/parameters.txt");
        this.addOutputFile(paramsFile, nodeIndex, fileIndex, fileLabel);

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }
}
