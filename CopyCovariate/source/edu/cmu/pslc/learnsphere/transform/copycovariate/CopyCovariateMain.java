package edu.cmu.pslc.learnsphere.transform.copycovariate;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class CopyCovariateMain extends AbstractComponent {


    public static void main(String[] args) {

        CopyCovariateMain tool = new CopyCovariateMain();
        tool.startComponent(args);
    }

    public CopyCovariateMain() {
        super();
    }


    @Override
    protected void parseOptions() {      

    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // Add the column headers from our input file to this component's output metadata,
        // plus one extra column for Predicted Error Rate, if it doesn't already exist.

        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        this.addMetaDataFromInput("transaction", 0, 0, ".*");
        //this.addMetaData("transaction", 0, META_DATA_LABEL, "label0", 0, "KC (" + modelName + ")");
        this.addMetaDataFromInput("tab-delimited", 1, 0, ".*");
    }

    @Override
    protected void runComponent() {
        // Run the program and add the files it generates to the component output.
        File outputDirectory = this.runExternalMultipleFileOuput();
        // Attach the output files to the component output: file_type = "analysis-summary", label = ""
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File outputFile = new File(outputDirectory.getAbsoluteFile() + "/transaction file with added features.txt");
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
