package edu.cmu.pslc.learnsphere.transform.DataShopTxnRollup;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class DataShopTxnRollup extends AbstractComponent {

    public static void main(String[] args) {

        DataShopTxnRollup tool = new DataShopTxnRollup();
        tool.startComponent(args);
    }

    public DataShopTxnRollup() {
        super();
    }

    @Override
    protected void parseOptions() {
	logger.info("Parsing options");
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
    }

    @Override
    protected void runComponent() {
        // Run the program...
        File outputDirectory = this.runExternal();

        Integer fileIndex = 0;
        Integer nodeIndex = 0;
        String fileLabel = "csv";
        File outputFile = new File(outputDirectory.getAbsolutePath() + "/txn_rollup.csv");
        this.addOutputFile(outputFile, nodeIndex, fileIndex, fileLabel);

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }
}
