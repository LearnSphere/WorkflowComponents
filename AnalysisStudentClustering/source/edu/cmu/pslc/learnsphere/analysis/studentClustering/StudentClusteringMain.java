package edu.cmu.pslc.learnsphere.analysis.studentClustering;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class StudentClusteringMain extends AbstractComponent {

    public static void main(String[] args) {

        StudentClusteringMain tool = new StudentClusteringMain();
        tool.startComponent(args);
    }

    public StudentClusteringMain() {
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
        File outputDirectory = this.runExternal();
        // Attach the output files to the component output with addOutputFile(..>)
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
           
            File file0 = new File(outputDirectory.getAbsolutePath() + "/myplot.png");
            File file1 = new File(outputDirectory.getAbsolutePath() + "/R_output_model_summary.txt");
           

            if (file0 != null && file0.exists() && file1 != null && file1.exists() ) {

                Integer nodeIndex0 = 0;
                Integer fileIndex0 = 0;
                String label0 = "image";
                Integer nodeIndex1 = 1;
                Integer fileIndex1 = 0;
                String label1 = "text";
                this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);
                this.addOutputFile(file1, nodeIndex1, fileIndex1, label1);

            } else {
                 this.addErrorMessage("Files missing.");
            }

        }

        System.out.println(this.getOutput());
    }

}
