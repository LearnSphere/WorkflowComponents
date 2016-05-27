package edu.cmu.pslc.learnsphere.generate.pfafeatures;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class PFAMain extends AbstractComponent {


    /** Component option (model). */
    String modelName = null;

    public static void main(String[] args) {

        PFAMain tool = new PFAMain();
        tool.startComponent(args);
    }

    public PFAMain() {
        super();
    }


    @Override
    protected void parseOptions() {
        String modelOption = this.getOptionAsString("model");
        logger.debug("Model option: " + modelOption);

        if (modelOption != null && modelOption.matches(".* - .*")) {

            String[] split = modelOption.split(" - ");
            modelName = split[split.length - 1].replaceAll("\\s*KC\\s*\\((.*)\\)\\s*", "$1");

            logger.debug("Model name: " + modelName);

        }

    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // Add the column headers from our input file to this component's output metadata,
        // plus one extra column for Predicted Error Rate, if it doesn't already exist.

        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        this.addMetaDataFromInput("transaction", 0, 0, ".*");
        this.addMetaData("transaction", 0, META_DATA_LABEL, "label0", 0, "KC (" + modelName + ")");
    }

    @Override
    protected void runComponent() {
        // Run the program and add the files it generates to the component output.
        File outputDirectory = this.runExternalMultipleFileOuput();
        // Attach the output files to the component output: file_type = "analysis-summary", label = ""
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File outputFile = new File(outputDirectory.getAbsoluteFile() + "/output-features.txt");
            Integer nodeIndex0 = 0;
            Integer fileIndex0 = 0;
            String label0 = "transaction";
            this.addOutputFile(outputFile, nodeIndex0, fileIndex0, label0);
        }

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }
}
