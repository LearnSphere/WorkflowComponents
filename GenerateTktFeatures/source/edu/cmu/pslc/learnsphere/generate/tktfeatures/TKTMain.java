package edu.cmu.pslc.learnsphere.generate.tktfeatures;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class TKTMain extends AbstractComponent {

    /** Component option (model). */
    String modelName = null;
 String modelsubName = null;
    public static void main(String[] args) {

        TKTMain tool = new TKTMain();
        tool.startComponent(args);
    }

    public TKTMain() {
        super();
    }


    @Override
    protected void parseOptions() {

        if (this.getOptionAsString("model") != null) {
            modelName = this.getOptionAsString("model").replaceAll("(?i)\\s*KC\\s*\\((.*)\\)\\s*", "$1");
        }
          if (this.getOptionAsString("modelsub") != null) {
            modelsubName = this.getOptionAsString("modelsub").replaceAll("(?i)\\s*KC\\s*\\((.*)\\)\\s*", "$1");
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
        this.addMetaData("transaction", 0, META_DATA_LABEL, "label1", 0,"KC (" + modelsubName + ")");

    }

    @Override
    protected void runComponent() {
        // Run the program and add the files it generates to the component output.
        File outputDirectory = this.runExternalMultipleFileOuput();
        // Attach the output files to the component output: file_type = "analysis-summary", label = ""
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File outputFile = new File(outputDirectory.getAbsoluteFile() + "/transaction_file_with_added_features.txt");
            Integer nodeIndex0 = 0;
            Integer fileIndex0 = 0;
            String label0 = "transaction";
            logger.info("Added file: " + outputFile.getAbsolutePath());
            this.addOutputFile(outputFile, nodeIndex0, fileIndex0, label0);
        }

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }
}
