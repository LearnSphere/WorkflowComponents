package edu.cmu.pslc.learnsphere.analysis.pfa;

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

        if (this.getOptionAsString("model") != null) {
            modelName = this.getOptionAsString("model").replaceAll("(?i)\\s*KC\\s*\\((.*)\\)\\s*", "$1");
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
        File outputDirectory = this.runExternal();
        // Attach the output files to the component output with addOutputFile(..>)
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File file0 = new File(outputDirectory.getAbsolutePath() + "/transaction_file_output.txt");
            File file1 = new File(outputDirectory.getAbsolutePath() + "/R_output_model_summary.txt");
            File file2 = new File(outputDirectory.getAbsolutePath() + "/random_effect_parameters.txt");
            File file3 = new File(outputDirectory.getAbsolutePath() + "/model_result_values.xml");

            if (file0 != null && file0.exists() && file1 != null && file1.exists()) {

                Integer nodeIndex0 = 0;
                Integer fileIndex0 = 0;
                String label0 = "transaction";
                this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);

                Integer nodeIndex1 = 1;
                Integer fileIndex1 = 0;
                String label1 = "text";
                this.addOutputFile(file1, nodeIndex1, fileIndex1, label1);

                Integer nodeIndex2 = 2;
                Integer fileIndex2 = 0;
                String label2 = "tab-delimited";
                this.addOutputFile(file2, nodeIndex2, fileIndex2, label2);

                Integer nodeIndex3 = 3;
                Integer fileIndex3 = 0;
                String label3 = "text";
                this.addOutputFile(file3, nodeIndex3, fileIndex3, label3);

            } else {
                this.addErrorMessage("An unknown error has occurred with the PFA component.");
            }

        }

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }

}
