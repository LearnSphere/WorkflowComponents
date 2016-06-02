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
        this.addMetaDataFromInput("student-step", 0, 0, ".*");
        this.addMetaData("student-step", 0, META_DATA_LABEL, "label0", 0, "Predicted Error Rate (" + modelName + ")");
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
        File outputDirectory = this.runExternalMultipleFileOuput();

        Integer nodeIndex = 0;
        Integer fileIndex = 0;
        String fileLabel = "student-step";

        File file0 = new File(outputDirectory.getAbsolutePath() + "/output.txt");

        this.addOutputFile(file0, nodeIndex, fileIndex, fileLabel);
        // Send the component output bakc to the workflow.
        System.out.println(this.getOutput());
    }

}
