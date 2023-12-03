package edu.cmu.pslc.learnsphere.analysis.analysisPfaStepBased;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class AnalysisPfaStepBased extends AbstractComponent {

    /** Component option (model). */
    String modelName = null;

    public static void main(String[] args) {

    	AnalysisPfaStepBased tool = new AnalysisPfaStepBased();
        tool.startComponent(args);
    }

    public AnalysisPfaStepBased() {
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
    	File inputFile = getAttachment(0, 0);
        logger.info("PFA inputFile: " + inputFile.getAbsolutePath());
        String paramOutputFileFormat = this.getOptionAsString("paramOutputFormat");
        // Run the program...
        File outputDirectory = this.runExternal();
        Integer fileIndex = 0;
        Integer nodeIndex = 0;
        String fileLabel = "student-step";
        File studentStepFile = new File(outputDirectory.getAbsolutePath() + "/student-step.txt");
        this.addOutputFile(studentStepFile, nodeIndex, fileIndex, fileLabel);
        
        nodeIndex = 1;
        fileLabel = "analysis-summary";
        File summaryFile = new File(outputDirectory.getAbsolutePath() + "/R-summary.txt");
        this.addOutputFile(summaryFile, nodeIndex, fileIndex, fileLabel);


    	nodeIndex = 2;
    	fileLabel = "model-values";
        File valuesFile = new File(outputDirectory.getAbsolutePath() + "/model-values.xml");
        this.addOutputFile(valuesFile, nodeIndex, fileIndex, fileLabel);

    	nodeIndex = 3;
    	fileLabel = "parameters";
    	File paramsFile = new File(outputDirectory.getAbsolutePath() + "/parameters.xml");
    	File paramsFile2 = new File(outputDirectory.getAbsolutePath() + "/parameters_tab_delim.txt");
    	if (paramOutputFileFormat.equalsIgnoreCase("xml"))
    		this.addOutputFile(paramsFile, nodeIndex, fileIndex, fileLabel);
    	else
    		this.addOutputFile(paramsFile2, nodeIndex, fileIndex, fileLabel);           
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
        
    }

}
