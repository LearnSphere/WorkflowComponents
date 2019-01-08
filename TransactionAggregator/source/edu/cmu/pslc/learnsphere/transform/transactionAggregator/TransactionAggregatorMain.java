package edu.cmu.pslc.learnsphere.transform.transactionAggregator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class TransactionAggregatorMain extends AbstractComponent {

    public static void main(String[] args) {

            TransactionAggregatorMain tool = new TransactionAggregatorMain();
        tool.startComponent(args);
    }

    public TransactionAggregatorMain() {
        super();
    }

    @Override
    protected void runComponent() {
        //get/set -f option
        File inputFile = getAttachment(0, 0);
        logger.info("TransactionAggregator inputFile: " + inputFile.getAbsolutePath());
        String aggregatedTo = this.getOptionAsString("aggregatedTo");
        List<String> KCModels = this.getMultiOptionAsString("kcModelsToAggregate");
        String outputFileName = "";
        if (aggregatedTo.equals("Student-step rollup"))
                outputFileName = "/studentStepRollup.txt";
        else if (aggregatedTo.equals("Transaction"))
                outputFileName = "/transaction.txt";
        logger.info("TransactionAggregator outputFileName: " + outputFileName);
        
        
        File outputDirectory = this.runExternal();
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
                logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
                File file0 = new File(outputDirectory.getAbsolutePath() + outputFileName);
                if (file0 != null && file0.exists()) {
                        Integer nodeIndex0 = 0;
                        Integer fileIndex0 = 0;
                        String label0 = "tab-delimited";
                        this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);
                } else {
                        addErrorMessage("An unknown error has occurred with the TransactionAggregator component.");
                }
        }
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());

    }
    //in r col name with space should replace space with a "."
    private String removeSpace(String aColName) {
            //return aColName.replaceAll("\\s+",".");
            return aColName.replaceAll("[\\(\\[\\]\\)\\-\\s]",".");
    }

}
