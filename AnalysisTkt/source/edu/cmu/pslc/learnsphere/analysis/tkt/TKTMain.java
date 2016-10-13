package edu.cmu.pslc.learnsphere.analysis.tkt;

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
        // Attach the output files to the component output with addOutputFile(..>)
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File file0 = new File(outputDirectory.getAbsolutePath() + "/tkt-model.txt");
            File file1 = new File(outputDirectory.getAbsolutePath() + "/AUC.png");
            File file2 = new File(outputDirectory.getAbsolutePath() + "/TKT-log.txt");

            if (file0 != null && file0.exists() && file1 != null && file1.exists()) {

                Integer nodeIndex0 = 0;
                Integer fileIndex0 = 0;
                String label0 = "transaction";
                this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);

                Integer nodeIndex1 = 1;
                Integer fileIndex1 = 0;
                String label1 = "image";
                this.addOutputFile(file1, nodeIndex1, fileIndex1, label1);

                Integer nodeIndex2 = 2;
                Integer fileIndex2 = 0;
                String label2 = "text";
                this.addOutputFile(file2, nodeIndex2, fileIndex2, label2);

            } else {
                this.addErrorMessage("An unknown error has occurred with the TKT component.");
            }

        }

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }

}