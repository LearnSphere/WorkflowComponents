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
            File outputFile = new File(outputDirectory.getAbsolutePath() + "/tab-delimited_file with covariate.txt");

            Integer nodeIndex0 = 0;
            Integer fileIndex0 = 0;
            String label0 = "tab-delimited";
            logger.info("Added file: " + outputFile.getAbsolutePath());
            this.addOutputFile(outputFile, nodeIndex0, fileIndex0, label0);
        }
        System.out.println(this.getOutput());

    }
}
