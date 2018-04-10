package edu.cmu.pslc.learnsphere.analysis.emd;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class EMDMain extends AbstractComponent {

    public static void main(String[] args) {

        EMDMain tool = new EMDMain();
        tool.startComponent(args);
    }

    public EMDMain() {
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
 
    }


    @Override
    protected void runComponent() {
        // Run the program and add the files it generates to the component output.
        File outputDirectory = this.runExternalMultipleFileOuput();
        // Attach the output files to the component output with addOutputFile(..>)
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
             System.out.println("Start");
            File file0 = new File(outputDirectory.getAbsolutePath() + "/myplot.jpeg");
 
System.out.println(outputDirectory.getAbsolutePath());
            if (file0 != null && file0.exists() ) {

                Integer nodeIndex0 = 0;
                Integer fileIndex0 = 0;
                String label0 = "jpeg";
                this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);

              

            } else {
                this.addErrorMessage("An unknown error has occurred with the EMD component.");
            }

        }

        // Send the component output back to the workflow.
        System.out.println("END");
        System.out.println(this.getOutput());
    }

}
