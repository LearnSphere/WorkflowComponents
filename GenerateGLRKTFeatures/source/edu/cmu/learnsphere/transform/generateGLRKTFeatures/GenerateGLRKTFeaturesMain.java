package edu.cmu.learnsphere.transform.generateGLRKTFeatures;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class GenerateGLRKTFeaturesMain extends AbstractComponent {

	/** Component option (model). */

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        GenerateGLRKTFeaturesMain tool = new GenerateGLRKTFeaturesMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public GenerateGLRKTFeaturesMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.
	// Add input meta-data (headers) to output file.
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
	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/GLRKT.txt");
	this.addOutputFile(outputFile0, 0, 0, "transaction");
        System.out.println(this.getOutput());
    }
}
