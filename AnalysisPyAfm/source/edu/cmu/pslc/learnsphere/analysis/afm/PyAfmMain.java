package edu.cmu.pslc.learnsphere.analysis.afm;

import java.io.File;
import java.text.DecimalFormat;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class PyAfmMain extends AbstractComponent {

    /** Component option (model). */
    String modelName = null;

    public static void main(String[] args) {

        PyAfmMain tool = new PyAfmMain();
        tool.startComponent(args);
    }

    public PyAfmMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // Add the column headers from our input file to this component's output metadata,
        // plus one extra column for Predicted Error Rate, if it doesn't already exist.

        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        String fileType = "student-step";
        Integer inputNodeIndex = 0;
        Integer outputNodeIndex = 0;
        Integer fileIndex = 0;

        this.addMetaDataFromInput(fileType, inputNodeIndex, outputNodeIndex, ".*");
        this.addMetaData(fileType, outputNodeIndex, META_DATA_LABEL, "label0", fileIndex, "Predicted Error Rate (" + modelName + ")");
    }

    @Override
    protected void parseOptions() {

        if (this.getOptionAsString("kc_model") != null) {
            modelName = this.getOptionAsString("kc_model").replaceAll("(?i)\\s*KC\\s*\\((.*)\\)\\s*", "$1");
        }
    }


    @Override
    protected void runComponent() {
        // Run the program and return its stdout to a file.
        File outputDirectory = this.runExternal();
        File stuStepFile = new File(outputDirectory.getAbsolutePath() + "/output.txt");
        File moelValuesFile = new File(outputDirectory.getAbsolutePath() + "/model_values.xml");
        File parametersFile = new File(outputDirectory.getAbsolutePath() + "/Parameter-estimate-values.xml");

        if (stuStepFile != null && stuStepFile.exists() && moelValuesFile != null && moelValuesFile.exists()
                       && parametersFile != null && parametersFile.exists()) {
                Integer nodeIndex = 0;
                Integer fileIndex = 0;
                String fileLabel = "student-step";
                this.addOutputFile(stuStepFile, nodeIndex, fileIndex, fileLabel);
                
                nodeIndex = 1;
                fileIndex = 0;
                fileLabel = "model-values";
                this.addOutputFile(moelValuesFile, nodeIndex, fileIndex, fileLabel);
                
                nodeIndex = 2;
                fileIndex = 0;
                fileLabel = "parameters";
                this.addOutputFile(parametersFile, nodeIndex, fileIndex, fileLabel);
                
        } else {
            this.addErrorMessage("An unknown error has occurred with the Python AFM component.");
        }

        /*nodeIndex = 1;
        fileIndex = 0;
        fileLabel = "tab-delimited";

        File fitStatsFile = new File(outputDirectory.getAbsolutePath() + "/output.txt");

        this.addOutputFile(fitStatsFile, nodeIndex, fileIndex, fileLabel);*/

        // Send the component output bakc to the workflow.
        System.out.println(this.getOutput());
    }

}
