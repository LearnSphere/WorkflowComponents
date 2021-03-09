package edu.cmu.pslc.learnsphere.analysis;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class GenerateLKTFeaturesMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        GenerateLKTFeaturesMain tool = new GenerateLKTFeaturesMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public GenerateLKTFeaturesMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.
        this.addMetaDataFromInput("transaction", 0, 0, ".*");
	// Add additional meta-data for each output file.
	this.addMetaData("file", 0, META_DATA_LABEL, "label0", 0, null);


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
	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/LKT.txt");
	this.addOutputFile(outputFile0, 0, 0, "transaction");
        System.out.println(this.getOutput());

    }
}
