package edu.cmu.pslc.learnsphere.analysis.SimModels;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class SimModelsMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        SimModelsMain tool = new SimModelsMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public SimModelsMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.


    }

    @Override
    protected void parseOptions() {


    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();

        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/Students.txt");
            File outputFile1 = new File(outputDirectory.getAbsolutePath() + "/Predict1.txt");
            File outputFile2 = new File(outputDirectory.getAbsolutePath() + "/myplot.png");

            Integer nodeIndex0 = 0;
            Integer fileIndex0 = 0;
            String label0 = "tab-delimited";
            logger.info("Added file: " + outputFile0.getAbsolutePath());
            this.addOutputFile(outputFile0, nodeIndex0, fileIndex0, label0);
            
            Integer nodeIndex1 = 1;
            Integer fileIndex1 = 0;
            String label1 = "tab-delimited";
            logger.info("Added file: " + outputFile1.getAbsolutePath());
            this.addOutputFile(outputFile1, nodeIndex1, fileIndex1, label1);
            
            Integer nodeIndex2 = 2;
            Integer fileIndex2 = 0;
            String label2 = "image";
            logger.info("Added file: " + outputFile2.getAbsolutePath());
            this.addOutputFile(outputFile2, nodeIndex2, fileIndex2, label2);
        }
        System.out.println(this.getOutput());

    }
}
